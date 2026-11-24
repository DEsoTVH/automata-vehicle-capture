package cl.automata.vehiclecapture.domain;

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

/** Evento de auditoría append-only. */
@Entity
@Table(name = "audit_event")
@Getter
@NoArgsConstructor
public class AuditEvent {
    @Id
    private UUID id;
    private String entityType;
    private UUID entityId;
    private String action;
    private String userName;
    private Instant at;

    @Convert(converter = JsonConverters.StringMap.class)
    private Map<String, String> details = new LinkedHashMap<>();

    public AuditEvent(String entityType, UUID entityId, String action, String userName, Map<String, String> details) {
        this.id = UUID.randomUUID();
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.userName = userName;
        this.at = Instant.now();
        this.details = details == null ? new LinkedHashMap<>() : new LinkedHashMap<>(details);
    }
}
