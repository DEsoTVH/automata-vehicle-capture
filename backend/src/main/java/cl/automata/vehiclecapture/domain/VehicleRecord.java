package cl.automata.vehiclecapture.domain;

import cl.automata.vehiclecapture.service.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Un vehículo = un registro. Las transiciones de estado viven aquí para que las reglas de negocio
 * no dependan de quién las invoque.
 */
@Entity
@Table(name = "vehicle_record")
@Getter
@NoArgsConstructor
public class VehicleRecord {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    private RecordStatus status;

    // Campos promovidos a columna: se usan para búsqueda y detección de duplicados.
    private String vin;
    private String brand;
    private String model;

    /** Valores vigentes de la ficha (extraídos y/o corregidos por una persona). */
    @Convert(converter = JsonConverters.StringMap.class)
    @Column(name = "field_values", nullable = false)
    private Map<String, String> data = new LinkedHashMap<>();

    private Instant capturedAt;
    private Instant processedAt;
    private String validatedBy;
    private Instant validatedAt;
    private String createdBy;

    @Version
    private Long version; // null = registro nuevo (Spring Data usa esto para decidir persist vs merge)

    public static VehicleRecord newCapture(String user) {
        VehicleRecord r = new VehicleRecord();
        r.id = UUID.randomUUID();
        r.status = RecordStatus.PENDING_PROCESSING;
        r.capturedAt = Instant.now();
        r.createdBy = user;
        return r;
    }

    public void markProcessing() {
        require(Set.of(RecordStatus.PENDING_PROCESSING, RecordStatus.PROCESSING_FAILED), "iniciar el procesamiento");
        status = RecordStatus.PROCESSING;
    }

    public void completeProcessing(Map<String, String> extracted, boolean needsReview) {
        require(Set.of(RecordStatus.PROCESSING), "completar el procesamiento");
        data = new LinkedHashMap<>(extracted);
        syncPromoted();
        processedAt = Instant.now();
        status = needsReview ? RecordStatus.NEEDS_REVIEW : RecordStatus.PENDING_VALIDATION;
    }

    public void failProcessing() {
        status = RecordStatus.PROCESSING_FAILED;
        processedAt = Instant.now();
    }

    /** Aplica correcciones manuales. Un registro validado que se edita vuelve a pendiente de validación. */
    public void applyCorrections(Map<String, String> newData) {
        require(Set.of(RecordStatus.NEEDS_REVIEW, RecordStatus.PENDING_VALIDATION, RecordStatus.VALIDATED),
                "editar");
        data = new LinkedHashMap<>(newData);
        syncPromoted();
        if (status == RecordStatus.VALIDATED) {
            status = RecordStatus.PENDING_VALIDATION;
            validatedBy = null;
            validatedAt = null;
        } else if (status == RecordStatus.NEEDS_REVIEW) {
            // La persona ya revisó y corrigió: pasa a espera de confirmación explícita.
            status = RecordStatus.PENDING_VALIDATION;
        }
    }

    public void validate(String user) {
        require(Set.of(RecordStatus.NEEDS_REVIEW, RecordStatus.PENDING_VALIDATION), "validar");
        status = RecordStatus.VALIDATED;
        validatedBy = user;
        validatedAt = Instant.now();
    }

    public void markExported() {
        require(Set.of(RecordStatus.VALIDATED), "exportar");
        status = RecordStatus.EXPORTED;
    }

    private void require(Set<RecordStatus> allowed, String action) {
        if (!allowed.contains(status)) {
            throw new BusinessException(409, "No se puede " + action + " un registro en estado " + status);
        }
    }

    private void syncPromoted() {
        vin = blankToNull(data.get("vin"));
        brand = blankToNull(data.get("brand"));
        model = blankToNull(data.get("model"));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
