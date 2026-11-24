package cl.automata.vehiclecapture.service;

import cl.automata.vehiclecapture.config.AppProperties;
import cl.automata.vehiclecapture.config.CurrentUser;
import cl.automata.vehiclecapture.domain.AuditEvent;
import cl.automata.vehiclecapture.domain.CaptureImage;
import cl.automata.vehiclecapture.domain.Extraction;
import cl.automata.vehiclecapture.domain.RecordStatus;
import cl.automata.vehiclecapture.domain.VehicleRecord;
import cl.automata.vehiclecapture.processing.FieldSchemaProvider;
import cl.automata.vehiclecapture.processing.FieldSchemaProvider.FieldDef;
import cl.automata.vehiclecapture.processing.LabelValueFieldExtractor;
import cl.automata.vehiclecapture.repository.Repositories.AuditEventRepository;
import cl.automata.vehiclecapture.repository.Repositories.CaptureImageRepository;
import cl.automata.vehiclecapture.repository.Repositories.ExtractionRepository;
import cl.automata.vehiclecapture.repository.VehicleRecordRepository;
import cl.automata.vehiclecapture.service.Dtos.AuditDto;
import cl.automata.vehiclecapture.service.Dtos.FileContent;
import cl.automata.vehiclecapture.service.Dtos.SchemaDto;
import cl.automata.vehiclecapture.service.Dtos.Stats;
import cl.automata.vehiclecapture.service.Dtos.VehicleDetail;
import cl.automata.vehiclecapture.service.Dtos.VehicleSummary;
import cl.automata.vehiclecapture.storage.BlobStorage;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VehicleService {

    private static final ZoneId ZONE = ZoneId.of("America/Santiago");
    private static final Set<RecordStatus> EDITABLE =
            Set.of(RecordStatus.NEEDS_REVIEW, RecordStatus.PENDING_VALIDATION, RecordStatus.VALIDATED);

    private final VehicleRecordRepository records;
    private final CaptureImageRepository images;
    private final ExtractionRepository extractions;
    private final AuditEventRepository audit;
    private final FieldSchemaProvider schema;
    private final BlobStorage storage;
    private final ApplicationEventPublisher events;
    private final AppProperties props;
    private final CurrentUser user;

    public VehicleService(VehicleRecordRepository records, CaptureImageRepository images,
                          ExtractionRepository extractions, AuditEventRepository audit, FieldSchemaProvider schema,
                          BlobStorage storage, ApplicationEventPublisher events, AppProperties props,
                          CurrentUser user) {
        this.records = records;
        this.images = images;
        this.extractions = extractions;
        this.audit = audit;
        this.schema = schema;
        this.storage = storage;
        this.events = events;
        this.props = props;
        this.user = user;
    }

    public SchemaDto schema() {
        return new SchemaDto(schema.fields(), props.ocr().lowConfidenceThreshold());
    }

    @Transactional(readOnly = true)
    public List<VehicleSummary> list(RecordStatus status, String text) {
        Specification<VehicleRecord> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (status != null) p.add(cb.equal(root.get("status"), status));
            if (text != null && !text.isBlank()) {
                String like = "%" + text.trim().toLowerCase() + "%";
                p.add(cb.or(
                        cb.like(cb.lower(root.<String>get("vin")), like),
                        cb.like(cb.lower(root.<String>get("brand")), like),
                        cb.like(cb.lower(root.<String>get("model")), like)));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
        return records.findAll(spec, Sort.by(Sort.Direction.DESC, "capturedAt")).stream()
                .map(r -> new VehicleSummary(r.getId(), r.getStatus(), r.getVin(), r.getBrand(), r.getModel(),
                        r.getCapturedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public VehicleDetail get(UUID id) {
        return toDetail(find(id));
    }

    @Transactional
    public VehicleDetail update(UUID id, Map<String, String> incoming) {
        VehicleRecord r = find(id);

        Map<String, String> next = new LinkedHashMap<>();
        for (String key : incoming.keySet()) {
            if (schema.byKey(key).isEmpty()) {
                throw new BusinessException(400, "Campo desconocido: " + key);
            }
        }
        for (FieldDef f : schema.fields()) {
            String v = incoming.get(f.key());
            if (v != null && !v.isBlank()) {
                String norm = LabelValueFieldExtractor.normalize(f.key(), v);
                if ("number".equals(f.type()) && !norm.matches("\\d+")) {
                    throw new BusinessException(422, "El campo \"" + f.label() + "\" debe ser numérico");
                }
                next.put(f.key(), norm);
            }
        }

        Map<String, String> changes = diff(r.getData(), next);
        if (!changes.isEmpty()) {
            r.applyCorrections(next);
            records.save(r);
            audit.save(new AuditEvent("VEHICLE", id, "EDITED", user.name(), changes));
        }
        return toDetail(r);
    }

    @Transactional
    public VehicleDetail validate(UUID id) {
        VehicleRecord r = find(id);
        List<String> missing = missingRequired(r.getData());
        if (!missing.isEmpty()) {
            throw new BusinessException(422, "Faltan campos obligatorios: " + String.join(", ", missing));
        }
        r.validate(user.name());
        records.save(r);
        audit.save(new AuditEvent("VEHICLE", id, "VALIDATED", user.name(), Map.of()));
        return toDetail(r);
    }

    /** Reintenta el procesamiento de un registro que falló. */
    @Transactional
    public VehicleDetail retry(UUID id) {
        VehicleRecord r = find(id);
        if (r.getStatus() != RecordStatus.PROCESSING_FAILED) {
            throw new BusinessException(409, "Solo se puede reintentar un registro con procesamiento fallido");
        }
        audit.save(new AuditEvent("VEHICLE", id, "RETRY_REQUESTED", user.name(), Map.of()));
        events.publishEvent(new CaptureCreatedEvent(id));
        return toDetail(r);
    }

    @Transactional(readOnly = true)
    public FileContent image(UUID id) {
        find(id);
        CaptureImage img = images.findFirstByRecordId(id)
                .orElseThrow(() -> new BusinessException(404, "El registro no tiene imagen"));
        String name = img.getOriginalFilename() != null ? img.getOriginalFilename() : "ficha-" + id;
        return new FileContent(storage.load(img.getStorageKey()), img.getMimeType(), name);
    }

    @Transactional(readOnly = true)
    public Stats stats() {
        Map<RecordStatus, Long> by = new EnumMap<>(RecordStatus.class);
        for (RecordStatus s : RecordStatus.values()) by.put(s, 0L);
        for (Object[] row : records.countGroupedByStatus()) by.put((RecordStatus) row[0], (Long) row[1]);
        long total = by.values().stream().mapToLong(Long::longValue).sum();
        long pendingReview = by.get(RecordStatus.NEEDS_REVIEW) + by.get(RecordStatus.PENDING_VALIDATION);
        // "Capturas del día": desde las 00:00 en la zona horaria de operación (Chile).
        var startOfDay = LocalDate.now(ZONE).atStartOfDay(ZONE).toInstant();
        return new Stats(total, pendingReview, by.get(RecordStatus.VALIDATED), by.get(RecordStatus.EXPORTED),
                records.countByCapturedAtGreaterThanEqual(startOfDay), by);
    }

    // ---- helpers ----

    private VehicleRecord find(UUID id) {
        return records.findById(id).orElseThrow(() -> new BusinessException(404, "Registro no encontrado"));
    }

    private VehicleDetail toDetail(VehicleRecord r) {
        Extraction ex = extractions.findFirstByRecordIdOrderByStartedAtDesc(r.getId()).orElse(null);
        Map<String, Double> confidences = ex != null && ex.getConfidences() != null ? ex.getConfidences() : Map.of();

        // Baja confianza solo mientras el valor siga siendo el que leyó el OCR (si una persona lo cambió, ya no).
        double threshold = props.ocr().lowConfidenceThreshold();
        List<String> low = new ArrayList<>();
        for (FieldDef f : schema.fields()) {
            Double c = confidences.get(f.key());
            if (c != null && c < threshold && ex != null
                    && Objects.equals(r.getData().get(f.key()), ex.getValues().get(f.key()))) {
                low.add(f.key());
            }
        }

        List<UUID> duplicates = r.getVin() == null ? List.of()
                : records.findByVinAndIdNot(r.getVin(), r.getId()).stream().map(VehicleRecord::getId).toList();

        List<AuditDto> history = audit.findByEntityIdOrderByAtDesc(r.getId()).stream()
                .map(a -> new AuditDto(a.getAction(), a.getUserName(), a.getAt(), a.getDetails()))
                .toList();

        return new VehicleDetail(r.getId(), r.getStatus(), r.getData(), confidences, low,
                missingRequired(r.getData()), r.getCapturedAt(), r.getProcessedAt(), r.getValidatedBy(),
                r.getValidatedAt(), "/api/vehicles/" + r.getId() + "/image", duplicates, history,
                EDITABLE.contains(r.getStatus()));
    }

    private List<String> missingRequired(Map<String, String> data) {
        return schema.fields().stream()
                .filter(FieldDef::required)
                .filter(f -> data.getOrDefault(f.key(), "").isBlank())
                .map(FieldDef::key)
                .toList();
    }

    private static Map<String, String> diff(Map<String, String> before, Map<String, String> after) {
        Map<String, String> changes = new LinkedHashMap<>();
        Set<String> keys = new java.util.LinkedHashSet<>(before.keySet());
        keys.addAll(after.keySet());
        for (String k : keys) {
            String a = before.getOrDefault(k, "");
            String b = after.getOrDefault(k, "");
            if (!a.equals(b)) changes.put(k, "\"" + a + "\" → \"" + b + "\"");
        }
        return changes;
    }
}
