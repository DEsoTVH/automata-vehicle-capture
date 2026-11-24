package cl.automata.vehiclecapture.processing;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Extractor simple por líneas "Etiqueta: valor". Sirve mientras no se conozca la estructura real
 * de la ficha; después se reemplaza/complementa por extractores por plantilla (reglas, LLM, etc.).
 */
@Component
public class LabelValueFieldExtractor implements FieldExtractor {

    private final FieldSchemaProvider schema;

    public LabelValueFieldExtractor(FieldSchemaProvider schema) {
        this.schema = schema;
    }

    @Override
    public ExtractionResult extract(OcrProvider.OcrResult ocr) {
        Map<String, String> values = new LinkedHashMap<>();
        Map<String, Double> confidences = new LinkedHashMap<>();
        for (OcrProvider.OcrLine line : ocr.lines()) {
            int idx = line.text().indexOf(':');
            if (idx < 0) continue;
            String label = line.text().substring(0, idx);
            String value = line.text().substring(idx + 1).trim();
            schema.byLabel(label).ifPresent(def -> {
                if (value.isEmpty() || value.contains("???")) {
                    return; // ilegible: queda vacío para que lo complete una persona
                }
                values.put(def.key(), normalize(def.key(), value));
                confidences.put(def.key(), line.confidence());
            });
        }
        return new ExtractionResult(values, confidences);
    }

    /** Normalización básica compartida con la edición manual. */
    public static String normalize(String key, String value) {
        String v = value.trim();
        if ("vin".equals(key)) {
            return v.replaceAll("\\s+", "").toUpperCase();
        }
        return v;
    }
}
