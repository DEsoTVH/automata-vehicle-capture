package cl.automata.vehiclecapture.processing;

import java.util.Map;

/** Convierte texto OCR en campos estructurados de la ficha. */
public interface FieldExtractor {

    ExtractionResult extract(OcrProvider.OcrResult ocr);

    /** valores por clave de campo y confianza (0..1) por cada campo que se logró leer. */
    record ExtractionResult(Map<String, String> values, Map<String, Double> confidences) {}
}
