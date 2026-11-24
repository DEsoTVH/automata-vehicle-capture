package cl.automata.vehiclecapture.domain;

public enum RecordStatus {
    PENDING_PROCESSING,
    PROCESSING,
    PROCESSING_FAILED,
    NEEDS_REVIEW,
    PENDING_VALIDATION,
    VALIDATED,
    EXPORTED
}
