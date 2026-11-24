package cl.automata.vehiclecapture.service;

import cl.automata.vehiclecapture.config.AppProperties;
import cl.automata.vehiclecapture.domain.AuditEvent;
import cl.automata.vehiclecapture.domain.CaptureImage;
import cl.automata.vehiclecapture.domain.Extraction;
import cl.automata.vehiclecapture.domain.VehicleRecord;
import cl.automata.vehiclecapture.processing.FieldExtractor;
import cl.automata.vehiclecapture.processing.FieldExtractor.ExtractionResult;
import cl.automata.vehiclecapture.processing.FieldSchemaProvider;
import cl.automata.vehiclecapture.processing.OcrProvider;
import cl.automata.vehiclecapture.repository.Repositories.AuditEventRepository;
import cl.automata.vehiclecapture.repository.Repositories.CaptureImageRepository;
import cl.automata.vehiclecapture.repository.Repositories.ExtractionRepository;
import cl.automata.vehiclecapture.repository.VehicleRecordRepository;
import cl.automata.vehiclecapture.storage.BlobStorage;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Pipeline: imagen -> OcrProvider -> FieldExtractor -> estado del registro.
 * Los datos extraídos NUNCA quedan validados: terminan en NEEDS_REVIEW o PENDING_VALIDATION.
 */
@Service
public class ProcessingService {

    private static final Logger log = LoggerFactory.getLogger(ProcessingService.class);

    private final VehicleRecordRepository records;
    private final CaptureImageRepository images;
    private final ExtractionRepository extractions;
    private final AuditEventRepository audit;
    private final BlobStorage storage;
    private final OcrProvider ocr;
    private final FieldExtractor extractor;
    private final FieldSchemaProvider schema;
    private final AppProperties props;
    private final TransactionTemplate tx;

    public ProcessingService(VehicleRecordRepository records, CaptureImageRepository images,
                             ExtractionRepository extractions, AuditEventRepository audit, BlobStorage storage,
                             OcrProvider ocr, FieldExtractor extractor, FieldSchemaProvider schema,
                             AppProperties props, PlatformTransactionManager txManager) {
        this.records = records;
        this.images = images;
        this.extractions = extractions;
        this.audit = audit;
        this.storage = storage;
        this.ocr = ocr;
        this.extractor = extractor;
        this.schema = schema;
        this.props = props;
        this.tx = new TransactionTemplate(txManager);
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCaptureCreated(CaptureCreatedEvent event) {
        process(event.recordId());
    }

    void process(UUID recordId) {
        Instant started = Instant.now();
        CaptureImage image;
        try {
            image = tx.execute(s -> {
                VehicleRecord r = records.findById(recordId).orElseThrow();
                r.markProcessing();
                records.save(r);
                return images.findFirstByRecordId(recordId).orElseThrow();
            });
        } catch (RuntimeException e) {
            log.warn("No se pudo iniciar el procesamiento de {}: {}", recordId, e.getMessage());
            return;
        }

        try {
            byte[] bytes = storage.load(image.getStorageKey()).getContentAsByteArray();
            OcrProvider.OcrResult ocrResult = ocr.read(bytes, image.getMimeType());
            ExtractionResult result = extractor.extract(ocrResult);
            boolean needsReview = needsReview(result);

            tx.executeWithoutResult(s -> {
                extractions.save(Extraction.ok(recordId, ocr.name(), ocrResult.text(), result.values(),
                        result.confidences(), started));
                VehicleRecord r = records.findById(recordId).orElseThrow();
                r.completeProcessing(result.values(), needsReview);
                records.save(r);
                audit.save(new AuditEvent("VEHICLE", recordId, "PROCESSED", "system",
                        Map.of("provider", ocr.name(), "result", r.getStatus().name())));
            });
        } catch (Exception e) {
            log.error("Falló el procesamiento de {}", recordId, e);
            tx.executeWithoutResult(s -> {
                extractions.save(Extraction.failed(recordId, ocr.name(), String.valueOf(e.getMessage()), started));
                VehicleRecord r = records.findById(recordId).orElseThrow();
                r.failProcessing();
                records.save(r);
                audit.save(new AuditEvent("VEHICLE", recordId, "PROCESSING_FAILED", "system",
                        Map.of("error", String.valueOf(e.getMessage()))));
            });
        }
    }

    /** Requiere revisión si falta un campo obligatorio o algún campo leído tiene baja confianza. */
    private boolean needsReview(ExtractionResult result) {
        boolean missingRequired = schema.fields().stream()
                .filter(FieldSchemaProvider.FieldDef::required)
                .anyMatch(f -> result.values().getOrDefault(f.key(), "").isBlank());
        boolean lowConfidence = result.confidences().values().stream()
                .anyMatch(c -> c < props.ocr().lowConfidenceThreshold());
        return missingRequired || lowConfidence;
    }
}
