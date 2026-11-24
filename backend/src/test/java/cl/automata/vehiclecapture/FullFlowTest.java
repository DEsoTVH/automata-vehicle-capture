package cl.automata.vehiclecapture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class FullFlowTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) throws IOException {
        r.add("spring.datasource.url", () -> "jdbc:h2:mem:it;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        r.add("app.storage.path", () -> {
            try {
                return Files.createTempDirectory("avc-it").toString();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });
        r.add("app.ocr.dummy-latency-ms", () -> "0");
    }

    @Test
    void captureProcessCorrectValidateExport() throws Exception {
        byte[] photo = randomBytes(1);

        // 1. Captura
        String id = upload(photo, 201);

        // 2. Procesamiento asÃ­ncrono: nunca queda validado solo
        JsonNode detail = awaitProcessed(id);
        assertThat(detail.get("status").asText()).isIn("NEEDS_REVIEW", "PENDING_VALIDATION");
        assertThat(detail.get("editable").asBoolean()).isTrue();

        // 3. La foto original se puede recuperar tal cual
        byte[] stored = mvc.perform(get("/api/vehicles/{id}/image", id)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(stored).isEqualTo(photo);

        // 4. CorrecciÃ³n manual: completa lo obligatorio que el OCR no pudo leer
        Map<String, String> data = new LinkedHashMap<>();
        detail.get("data").fields().forEachRemaining(e -> data.put(e.getKey(), e.getValue().asText()));
        data.putIfAbsent("vin", "1HGBH41JXMN109186");
        data.putIfAbsent("brand", "Marca X");
        data.putIfAbsent("model", "Modelo Y");
        data.putIfAbsent("year", "2022");
        data.put("color", "Verde Test");
        mvc.perform(put("/api/vehicles/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("data", data)))).andExpect(status().isOk());

        // 5. ValidaciÃ³n humana
        mvc.perform(post("/api/vehicles/{id}/validate", id)).andExpect(status().isOk());
        JsonNode validated = detail(id);
        assertThat(validated.get("status").asText()).isEqualTo("VALIDATED");
        assertThat(validated.get("validatedBy").asText()).isEqualTo("demo");
        // historial: capturado, procesado, editado, validado
        assertThat(validated.get("history").findValuesAsText("action"))
                .contains("CAPTURED", "PROCESSED", "EDITED", "VALIDATED");

        // 6. Misma foto otra vez => idempotente, no duplica
        assertThat(upload(photo, 200)).isEqualTo(id);

        // 7. Campo desconocido => 400
        mvc.perform(put("/api/vehicles/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"data\":{\"inventado\":\"x\"}}")).andExpect(status().isBadRequest());

        // 8. ExportaciÃ³n: incluye el validado, lo marca EXPORTED y no se puede exportar dos veces
        String exportBody = mvc.perform(post("/api/exports").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("recordIds", java.util.List.of(id)))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String batchId = json.readTree(exportBody).get("id").asText();
        byte[] xlsx = mvc.perform(get("/api/exports/{id}/download", batchId)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(xlsx, 0, 2)).isEqualTo("PK"); // .xlsx es un zip
        assertThat(detail(id).get("status").asText()).isEqualTo("EXPORTED");

        mvc.perform(post("/api/exports").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("recordIds", java.util.List.of(id)))))
                .andExpect(status().isUnprocessableEntity());
        // y un registro exportado ya no se puede editar
        mvc.perform(put("/api/vehicles/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("data", Map.of("vin", "OTRO")))))
                .andExpect(status().isConflict());
    }

    @Test
    void nonValidatedRecordsCannotBeExported() throws Exception {
        String id = upload(randomBytes(2), 201);
        awaitProcessed(id);
        mvc.perform(post("/api/exports").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("recordIds", java.util.List.of(id)))))
                .andExpect(status().isUnprocessableEntity());
    }

    // ---- helpers ----

    private String upload(byte[] bytes, int expectedStatus) throws Exception {
        var res = mvc.perform(multipart("/api/captures").file(new MockMultipartFile("file", "ficha.jpg",
                "image/jpeg", bytes))).andExpect(status().is(expectedStatus)).andReturn();
        return json.readTree(res.getResponse().getContentAsString()).get("recordId").asText();
    }

    private JsonNode detail(String id) throws Exception {
        return json.readTree(mvc.perform(get("/api/vehicles/{id}", id)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
    }

    private JsonNode awaitProcessed(String id) throws Exception {
        for (int i = 0; i < 100; i++) {
            JsonNode d = detail(id);
            String s = d.get("status").asText();
            if (!s.equals("PENDING_PROCESSING") && !s.equals("PROCESSING")) return d;
            Thread.sleep(100);
        }
        throw new AssertionError("El procesamiento no terminÃ³ a tiempo");
    }

    private static byte[] randomBytes(long seed) {
        byte[] b = new byte[2048];
        new Random(seed).nextBytes(b);
        return b;
    }
}

