package cl.automata.vehiclecapture.domain;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Qué se exportó exactamente: guarda una copia (snapshot) de los datos tal como salieron. */
@Entity
@Table(name = "export_item")
@Getter
@NoArgsConstructor
public class ExportItem {
    @Id
    private UUID id;
    private UUID batchId;
    private UUID recordId;

    @Convert(converter = JsonConverters.StringMap.class)
    private Map<String, String> snapshot;

    public ExportItem(UUID batchId, UUID recordId, Map<String, String> snapshot) {
        this.id = UUID.randomUUID();
        this.batchId = batchId;
        this.recordId = recordId;
        this.snapshot = snapshot;
    }
}
