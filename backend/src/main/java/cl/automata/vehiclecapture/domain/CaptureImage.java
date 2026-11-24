package cl.automata.vehiclecapture.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Fotografía original. Inmutable: no existe operación de borrado ni de reemplazo. */
@Entity
@Table(name = "capture_image")
@Getter
@NoArgsConstructor
public class CaptureImage {
    @Id
    private UUID id;
    private UUID recordId;
    private String storageKey;
    private String sha256;
    private String mimeType;
    private long sizeBytes;
    private String originalFilename;
    private Instant capturedAt;

    public CaptureImage(UUID id, UUID recordId, String storageKey, String sha256, String mimeType,
                        long sizeBytes, String originalFilename) {
        this.id = id;
        this.recordId = recordId;
        this.storageKey = storageKey;
        this.sha256 = sha256;
        this.mimeType = mimeType;
        this.sizeBytes = sizeBytes;
        this.originalFilename = originalFilename;
        this.capturedAt = Instant.now();
    }
}
