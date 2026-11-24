package cl.automata.vehiclecapture.service;

import cl.automata.vehiclecapture.domain.RecordStatus;
import cl.automata.vehiclecapture.processing.FieldSchemaProvider;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Dtos {
    private Dtos() {}

    public record VehicleSummary(UUID id, RecordStatus status, String vin, String brand, String model,
                                 Instant capturedAt) {}

    public record AuditDto(String action, String user, Instant at, Map<String, String> details) {}

    public record VehicleDetail(UUID id, RecordStatus status, Map<String, String> data,
                                Map<String, Double> confidences, List<String> lowConfidenceFields,
                                List<String> missingRequiredFields, Instant capturedAt, Instant processedAt,
                                String validatedBy, Instant validatedAt, String imageUrl,
                                List<UUID> possibleDuplicates, List<AuditDto> history, boolean editable) {}

    public record UpdateRequest(@NotNull Map<String, String> data) {}

    public record CaptureResult(UUID recordId, boolean duplicate) {}

    public record Stats(long total, long pendingReview, long validated, long exported, long capturedToday,
                        Map<RecordStatus, Long> byStatus) {}

    public record SchemaDto(List<FieldSchemaProvider.FieldDef> fields, double lowConfidenceThreshold) {}

    public record ExportRequest(List<UUID> recordIds) {}

    public record ExportDto(UUID id, Instant createdAt, String createdBy, int recordCount, String fileName,
                            String downloadUrl) {}

    public record FileContent(org.springframework.core.io.Resource resource, String mimeType, String fileName) {}
}
