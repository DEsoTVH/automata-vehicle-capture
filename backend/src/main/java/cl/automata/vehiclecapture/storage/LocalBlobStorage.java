package cl.automata.vehiclecapture.storage;

import cl.automata.vehiclecapture.config.AppProperties;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class LocalBlobStorage implements BlobStorage {

    private final Path root;

    public LocalBlobStorage(AppProperties props) {
        this.root = Path.of(props.storage().path()).toAbsolutePath().normalize();
    }

    @Override
    public void save(String key, byte[] content) {
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            // CREATE_NEW: nunca sobrescribe. Las fotos originales son inmutables.
            Files.write(target, content, java.nio.file.StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar " + key, e);
        }
    }

    @Override
    public Resource load(String key) {
        return new FileSystemResource(resolve(key));
    }

    private Path resolve(String key) {
        Path p = root.resolve(key).normalize();
        if (!p.startsWith(root)) {
            throw new IllegalArgumentException("Ruta de almacenamiento inválida");
        }
        return p;
    }
}
