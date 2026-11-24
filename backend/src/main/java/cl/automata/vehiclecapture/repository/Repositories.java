package cl.automata.vehiclecapture.repository;

import cl.automata.vehiclecapture.domain.AuditEvent;
import cl.automata.vehiclecapture.domain.CaptureImage;
import cl.automata.vehiclecapture.domain.ExportBatch;
import cl.automata.vehiclecapture.domain.ExportItem;
import cl.automata.vehiclecapture.domain.Extraction;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public final class Repositories {
    private Repositories() {}

    public interface CaptureImageRepository extends JpaRepository<CaptureImage, UUID> {
        Optional<CaptureImage> findFirstByRecordId(UUID recordId);

        Optional<CaptureImage> findFirstBySha256(String sha256);
    }

    public interface ExtractionRepository extends JpaRepository<Extraction, UUID> {
        Optional<Extraction> findFirstByRecordIdOrderByStartedAtDesc(UUID recordId);
    }

    public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {
        List<AuditEvent> findByEntityIdOrderByAtDesc(UUID entityId);
    }

    public interface ExportBatchRepository extends JpaRepository<ExportBatch, UUID> {
        List<ExportBatch> findAllByOrderByCreatedAtDesc();
    }

    public interface ExportItemRepository extends JpaRepository<ExportItem, UUID> {
        List<ExportItem> findByBatchId(UUID batchId);
    }
}
