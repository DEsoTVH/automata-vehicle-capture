-- Esquema inicial. SQL portable (H2 en modo PostgreSQL y PostgreSQL).
-- Los campos variables de la ficha viven en field_values (JSON como texto) para soportar
-- distintos formatos de ficha sin migraciones. Candidato a migrar a JSONB cuando se necesite consultar dentro.

CREATE TABLE vehicle_record (
    id            UUID PRIMARY KEY,
    status        VARCHAR(32)  NOT NULL,
    vin           VARCHAR(64),
    brand         VARCHAR(100),
    model         VARCHAR(100),
    field_values  TEXT         NOT NULL,
    captured_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_at  TIMESTAMP WITH TIME ZONE,
    validated_by  VARCHAR(100),
    validated_at  TIMESTAMP WITH TIME ZONE,
    created_by    VARCHAR(100) NOT NULL,
    version       BIGINT       NOT NULL DEFAULT 0
);
CREATE INDEX idx_vehicle_record_status ON vehicle_record (status);
CREATE INDEX idx_vehicle_record_vin ON vehicle_record (vin);
CREATE INDEX idx_vehicle_record_captured_at ON vehicle_record (captured_at);

CREATE TABLE capture_image (
    id                 UUID PRIMARY KEY,
    record_id          UUID         NOT NULL REFERENCES vehicle_record (id),
    storage_key        VARCHAR(500) NOT NULL,
    sha256             VARCHAR(64)  NOT NULL,
    mime_type          VARCHAR(100) NOT NULL,
    size_bytes         BIGINT       NOT NULL,
    original_filename  VARCHAR(255),
    captured_at        TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_capture_image_record ON capture_image (record_id);
CREATE INDEX idx_capture_image_sha ON capture_image (sha256);

CREATE TABLE extraction (
    id           UUID PRIMARY KEY,
    record_id    UUID         NOT NULL REFERENCES vehicle_record (id),
    provider     VARCHAR(50)  NOT NULL,
    status       VARCHAR(20)  NOT NULL,
    raw_text     TEXT,
    field_values TEXT,
    confidences  TEXT,
    error        TEXT,
    started_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    finished_at  TIMESTAMP WITH TIME ZONE
);
CREATE INDEX idx_extraction_record ON extraction (record_id);

CREATE TABLE audit_event (
    id           UUID PRIMARY KEY,
    entity_type  VARCHAR(50)  NOT NULL,
    entity_id    UUID         NOT NULL,
    action       VARCHAR(50)  NOT NULL,
    user_name    VARCHAR(100) NOT NULL,
    at           TIMESTAMP WITH TIME ZONE NOT NULL,
    details      TEXT
);
CREATE INDEX idx_audit_entity ON audit_event (entity_id);

CREATE TABLE export_batch (
    id            UUID PRIMARY KEY,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by    VARCHAR(100) NOT NULL,
    record_count  INT          NOT NULL,
    file_name     VARCHAR(255) NOT NULL,
    storage_key   VARCHAR(500) NOT NULL
);

CREATE TABLE export_item (
    id         UUID PRIMARY KEY,
    batch_id   UUID NOT NULL REFERENCES export_batch (id),
    record_id  UUID NOT NULL REFERENCES vehicle_record (id),
    snapshot   TEXT NOT NULL
);
CREATE INDEX idx_export_item_batch ON export_item (batch_id);
CREATE INDEX idx_export_item_record ON export_item (record_id);
