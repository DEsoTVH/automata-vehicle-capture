package cl.automata.vehiclecapture.service;

import cl.automata.vehiclecapture.config.CurrentUser;
import cl.automata.vehiclecapture.domain.AuditEvent;
import cl.automata.vehiclecapture.domain.CaptureImage;
import cl.automata.vehiclecapture.domain.VehicleRecord;
import cl.automata.vehiclecapture.repository.Repositories.AuditEventRepository;
import cl.automata.vehiclecapture.repository.Repositories.CaptureImageRepository;
import cl.automata.vehiclecapture.repository.VehicleRecordRepository;
import cl.automata.vehiclecapture.service.Dtos.CaptureResult;
import cl.automata.vehiclecapture.storage.BlobStorage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CaptureService {

    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg", "image/png", "png", "image/webp", "webp",
            "image/heic", "heic", "image/heif", "heif");

    private final VehicleRecordRepository records;
    private final CaptureImageRepository images;
    private final AuditEventRepository audit;
    private final BlobStorage storage;
    private final ApplicationEventPublisher events;
    private final CurrentUser user;

    public CaptureService(VehicleRecordRepository records, CaptureImageRepository images, AuditEventRepository audit,
                          BlobStorage storage, ApplicationEventPublisher events, CurrentUser user) {
        this.records = records;
        this.images = images;
        this.audit = audit;
        this.storage = storage;
        this.events = events;
        this.user = user;
    }

    /**
     * Guarda la foto original, crea el registro y dispara el procesamiento (asíncrono, tras el commit).
     * Es idempotente por contenido: si la misma foto ya fue cargada devuelve el registro existente.
     */
    @Transactional
    public CaptureResult capture(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "Debes adjuntar una fotografía");
        }
        String mime = Optional.ofNullable(file.getContentType()).orElse("").toLowerCase();
        if (!EXTENSIONS.containsKey(mime)) {
            throw new BusinessException(415, "Formato de imagen no soportado: " + mime);
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        String sha = sha256(bytes);

        Optional<CaptureImage> existing = images.findFirstBySha256(sha);
        if (existing.isPresent()) {
            return new CaptureResult(existing.get().getRecordId(), true);
        }

        VehicleRecord record = VehicleRecord.newCapture(user.name());
        UUID imageId = UUID.randomUUID();
        LocalDate d = LocalDate.now();
        String key = "captures/%d/%02d/%s/%s.%s".formatted(d.getYear(), d.getMonthValue(), record.getId(), imageId,
                EXTENSIONS.get(mime));
        storage.save(key, bytes);

        records.save(record);
        images.save(new CaptureImage(imageId, record.getId(), key, sha, mime, bytes.length,
                file.getOriginalFilename()));
        audit.save(new AuditEvent("VEHICLE", record.getId(), "CAPTURED", user.name(),
                Map.of("sha256", sha, "sizeBytes", String.valueOf(bytes.length))));

        events.publishEvent(new CaptureCreatedEvent(record.getId()));
        return new CaptureResult(record.getId(), false);
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
