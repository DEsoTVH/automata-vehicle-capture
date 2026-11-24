package cl.automata.vehiclecapture.storage;

import org.springframework.core.io.Resource;

/**
 * Puerto de almacenamiento de archivos (fotos originales y exportaciones).
 * Implementación actual: disco local. Futuro: S3 / MinIO / R2 sin tocar la lógica de negocio.
 */
public interface BlobStorage {
    void save(String key, byte[] content);

    Resource load(String key);
}
