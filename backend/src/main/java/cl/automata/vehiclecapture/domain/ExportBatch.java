package cl.automata.vehiclecapture.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "export_batch")
@Getter
@NoArgsConstructor
public class ExportBatch {
    @Id
    private UUID id;
    private Instant createdAt;
    private String createdBy;
    private int recordCount;
    private String fileName;
    private String storageKey;

    public ExportBatch(UUID id, String createdBy, int recordCount, String fileName, String storageKey) {
        this.id = id;
        this.createdAt = Instant.now();
        this.createdBy = createdBy;
        this.recordCount = recordCount;
        this.fileName = fileName;
        this.storageKey = storageKey;
    }
}
