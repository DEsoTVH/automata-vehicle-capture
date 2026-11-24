package cl.automata.vehiclecapture.processing;

import cl.automata.vehiclecapture.config.AppProperties;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * OCR FALSO para desarrollo: no lee la imagen. Genera el texto de una "ficha" ficticia, determinista
 * según el contenido de la foto (misma foto => mismos datos). Simula latencia, campos faltantes,
 * ilegibles y confianzas bajas para ejercitar todo el flujo de revisión.
 */
@Component
@ConditionalOnProperty(name = "app.ocr.provider", havingValue = "dummy", matchIfMissing = true)
public class DummyOcrProvider implements OcrProvider {

    private static final Map<String, List<String>> CATALOG = Map.of(
            "Toyota", List.of("Hilux", "Corolla Cross", "Yaris"),
            "Hyundai", List.of("Tucson", "Accent", "Santa Fe"),
            "Kia", List.of("Sportage", "Morning", "Sorento"),
            "Nissan", List.of("Navara", "Qashqai", "Kicks"),
            "Chevrolet", List.of("Silverado", "Tracker", "Sail"),
            "Ford", List.of("Ranger", "Territory", "Explorer"),
            "Mazda", List.of("CX-5", "Mazda3", "BT-50"),
            "JAC", List.of("T8", "JS4", "Sunray"));
    private static final List<String> COLORS = List.of("Blanco", "Negro", "Gris", "Rojo", "Azul", "Plata");
    private static final List<String> FUEL = List.of("Bencina", "Diésel", "Híbrido");
    private static final List<String> BODY = List.of("SUV", "Pick-up", "Sedán", "Hatchback");
    private static final List<String> ORIGIN = List.of("Japón", "Corea del Sur", "China", "México", "Tailandia", "Brasil");
    private static final String VIN_CHARS = "ABCDEFGHJKLMNPRSTUVWXYZ0123456789";

    private final AppProperties props;
    private final FieldSchemaProvider schema;

    public DummyOcrProvider(AppProperties props, FieldSchemaProvider schema) {
        this.props = props;
        this.schema = schema;
    }

    @Override
    public String name() {
        return "dummy";
    }

    @Override
    public OcrResult read(byte[] image, String mimeType) {
        sleep(props.ocr().dummyLatencyMs());
        Random rnd = new Random(seed(image));

        List<String> brands = CATALOG.keySet().stream().sorted().toList();
        String brand = brands.get(rnd.nextInt(brands.size()));
        List<String> models = CATALOG.get(brand);

        StringBuilder vin = new StringBuilder();
        for (int i = 0; i < 17; i++) vin.append(VIN_CHARS.charAt(rnd.nextInt(VIN_CHARS.length())));

        Map<String, String> truth = new java.util.LinkedHashMap<>();
        truth.put("vin", vin.toString());
        truth.put("brand", brand);
        truth.put("model", models.get(rnd.nextInt(models.size())));
        truth.put("year", String.valueOf(2019 + rnd.nextInt(7)));
        truth.put("color", pick(rnd, COLORS));
        truth.put("engineNumber", "EN" + (100000 + rnd.nextInt(900000)));
        truth.put("fuelType", pick(rnd, FUEL));
        truth.put("bodyType", pick(rnd, BODY));
        truth.put("displacementCc", String.valueOf(1000 + 200 * rnd.nextInt(12)));
        truth.put("originCountry", pick(rnd, ORIGIN));

        List<OcrLine> lines = new ArrayList<>();
        lines.add(new OcrLine("FICHA DE VEHICULO (DEMO)", 0.99));
        for (FieldSchemaProvider.FieldDef f : schema.fields()) {
            double roll = rnd.nextDouble();
            double confidence = 0.80 + rnd.nextDouble() * 0.19; // 0.80 - 0.99
            if (roll < 0.10 && !f.required()) {
                continue; // campo ausente en la foto
            } else if (roll < 0.20) {
                lines.add(new OcrLine(f.label() + ": ???", 0.20)); // ilegible
            } else {
                if (roll < 0.35) confidence = 0.45 + rnd.nextDouble() * 0.30; // lectura dudosa
                lines.add(new OcrLine(f.label() + ": " + truth.get(f.key()), confidence));
            }
        }
        return new OcrResult(lines);
    }

    private static String pick(Random rnd, List<String> list) {
        return list.get(rnd.nextInt(list.size()));
    }

    private static long seed(byte[] image) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(image);
            return ByteBuffer.wrap(h, 0, 8).getLong();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void sleep(long ms) {
        if (ms <= 0) return;
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
