package cl.automata.vehiclecapture.service;

import cl.automata.vehiclecapture.config.CurrentUser;
import cl.automata.vehiclecapture.domain.AuditEvent;
import cl.automata.vehiclecapture.domain.ExportBatch;
import cl.automata.vehiclecapture.domain.ExportItem;
import cl.automata.vehiclecapture.domain.RecordStatus;
import cl.automata.vehiclecapture.domain.VehicleRecord;
import cl.automata.vehiclecapture.repository.Repositories.AuditEventRepository;
import cl.automata.vehiclecapture.repository.Repositories.ExportBatchRepository;
import cl.automata.vehiclecapture.repository.Repositories.ExportItemRepository;
import cl.automata.vehiclecapture.repository.VehicleRecordRepository;
import cl.automata.vehiclecapture.service.Dtos.ExportDto;
import cl.automata.vehiclecapture.service.Dtos.FileContent;
import cl.automata.vehiclecapture.storage.BlobStorage;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExportService {

    private static final String XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final VehicleRecordRepository records;
    private final ExportBatchRepository batches;
    private final ExportItemRepository items;
    private final AuditEventRepository audit;
    private final BlobStorage storage;
    private final XlsxExporter exporter;
    private final CurrentUser user;

    public ExportService(VehicleRecordRepository records, ExportBatchRepository batches, ExportItemRepository items,
                         AuditEventRepository audit, BlobStorage storage, XlsxExporter exporter, CurrentUser user) {
        this.records = records;
        this.batches = batches;
        this.items = items;
        this.audit = audit;
        this.storage = storage;
        this.exporter = exporter;
        this.user = user;
    }

    /**
     * Exporta registros VALIDADOS (los indicados, o todos los validados si no se indica ninguno).
     * Los exportados pasan a EXPORTED, por lo que no vuelven a ofrecerse ni a duplicarse.
     */
    @Transactional
    public ExportDto create(List<UUID> recordIds) {
        List<VehicleRecord> selected;
        if (recordIds == null || recordIds.isEmpty()) {
            selected = records.findByStatusOrderByCapturedAtAsc(RecordStatus.VALIDATED);
        } else {
            LinkedHashSet<UUID> unique = new LinkedHashSet<>(recordIds);
            selected = records.findAllById(unique);
            if (selected.size() != unique.size()) {
                throw new BusinessException(404, "Algunos registros seleccionados no existen");
            }
            List<String> notValidated = selected.stream()
                    .filter(r -> r.getStatus() != RecordStatus.VALIDATED)
                    .map(r -> r.getId() + " (" + r.getStatus() + ")")
                    .toList();
            if (!notValidated.isEmpty()) {
                throw new BusinessException(422, "Solo se pueden exportar registros validados. No válidos: "
                        + String.join(", ", notValidated));
            }
        }
        if (selected.isEmpty()) {
            throw new BusinessException(422, "No hay registros validados para exportar");
        }
        selected = selected.stream().sorted((a, b) -> a.getCapturedAt().compareTo(b.getCapturedAt())).toList();

        List<Map<String, String>> rows = selected.stream().map(VehicleRecord::getData).toList();
        byte[] file = exporter.build(rows);

        UUID batchId = UUID.randomUUID();
        String stamp = LocalDateTime.now(ZoneId.of("America/Santiago"))
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "registro_vehiculos_" + stamp + ".xlsx";
        String key = "exports/" + batchId + "/" + fileName;
        storage.save(key, file);

        ExportBatch batch = batches.save(new ExportBatch(batchId, user.name(), selected.size(), fileName, key));
        for (VehicleRecord r : selected) {
            items.save(new ExportItem(batchId, r.getId(), new LinkedHashMap<>(r.getData())));
            r.markExported();
            records.save(r);
            audit.save(new AuditEvent("VEHICLE", r.getId(), "EXPORTED", user.name(),
                    Map.of("batchId", batchId.toString(), "file", fileName)));
        }
        return toDto(batch);
    }

    @Transactional(readOnly = true)
    public List<ExportDto> history() {
        return batches.findAllByOrderByCreatedAtDesc().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public FileContent download(UUID batchId) {
        ExportBatch b = batches.findById(batchId)
                .orElseThrow(() -> new BusinessException(404, "Exportación no encontrada"));
        return new FileContent(storage.load(b.getStorageKey()), XLSX, b.getFileName());
    }

    private ExportDto toDto(ExportBatch b) {
        return new ExportDto(b.getId(), b.getCreatedAt(), b.getCreatedBy(), b.getRecordCount(), b.getFileName(),
                "/api/exports/" + b.getId() + "/download");
    }
}
