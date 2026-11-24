package cl.automata.vehiclecapture.processing;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Puerto OCR. Cada proveedor (Google Vision, Textract, Tesseract…) es un adaptador que implementa
 * esta interfaz; la lógica de negocio no sabe cuál se usa.
 */
public interface OcrProvider {

    String name();

    OcrResult read(byte[] image, String mimeType);

    record OcrLine(String text, double confidence) {}

    record OcrResult(List<OcrLine> lines) {
        public String text() {
            return lines.stream().map(OcrLine::text).collect(Collectors.joining("\n"));
        }
    }
}
