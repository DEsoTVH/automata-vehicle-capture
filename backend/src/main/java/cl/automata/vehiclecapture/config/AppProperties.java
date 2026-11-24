package cl.automata.vehiclecapture.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Storage storage, Ocr ocr, Export export) {

    public record Storage(String path) {}

    public record Ocr(String provider, double lowConfidenceThreshold, long dummyLatencyMs) {}

    public record Export(List<Column> columns) {
        /** Columna del archivo de salida: encabezado y clave del campo de la ficha que la alimenta. */
        public record Column(String header, String field) {}
    }
}
