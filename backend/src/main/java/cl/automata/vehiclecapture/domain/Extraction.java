package cl.automata.vehiclecapture.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Resultado crudo de un intento de extracción. Se conserva aunque la persona corrija los datos. */
@Entity
@Table(name = "extraction")
@Getter
@NoArgsConstructor
public class Extraction {
    @Id
    private UUID id;
    private UUID recordId;
    private String provider;
    private String status; // OK | FAILED
    private String rawText;

    @Convert(converter = JsonConverters.StringMap.class)
    @Column(name = "field_values")
    private Map<String, String> values = new LinkedHashMap<>();

    @Convert(converter = JsonConverters.DoubleMap.class)
    private Map<String, Double> confidences = new LinkedHashMap<>();

    private String error;
    private Instant startedAt;
    private Instant finishedAt;

    public static Extraction ok(UUID recordId, String provider, String rawText, Map<String, String> values,
                                Map<String, Double> confidences, Instant startedAt) {
        Extraction e = base(recordId, provider, startedAt);
        e.status = "OK";
        e.rawText = rawText;
        e.values = values;
        e.confidences = confidences;
        return e;
    }

    public static Extraction failed(UUID recordId, String provider, String error, Instant startedAt) {
        Extraction e = base(recordId, provider, startedAt);
        e.status = "FAILED";
        e.error = error;
        return e;
    }

    private static Extraction base(UUID recordId, String provider, Instant startedAt) {
        Extraction e = new Extraction();
        e.id = UUID.randomUUID();
        e.recordId = recordId;
        e.provider = provider;
        e.startedAt = startedAt;
        e.finishedAt = Instant.now();
        return e;
    }
}
