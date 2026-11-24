package cl.automata.vehiclecapture.service;

import java.util.UUID;

/** Se publica dentro de la transacción de captura; el procesamiento arranca recién tras el commit. */
public record CaptureCreatedEvent(UUID recordId) {}
