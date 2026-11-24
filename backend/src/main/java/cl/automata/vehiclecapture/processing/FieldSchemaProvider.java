package cl.automata.vehiclecapture.processing;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Esquema de campos de la ficha.
 *
 * DUMMY: estos campos son ficticios (datos técnicos genéricos de un vehículo). Se reemplazan por el
 * esquema real cuando se reciba la ficha oficial. En el diseño final esto sale de la tabla
 * document_template (un esquema por formato de ficha); por ahora es una lista fija.
 */
@Component
public class FieldSchemaProvider {

    public record FieldDef(String key, String label, boolean required, String type) {}

    private static final List<FieldDef> FIELDS = List.of(
            new FieldDef("vin", "VIN / N° de chasis", true, "text"),
            new FieldDef("brand", "Marca", true, "text"),
            new FieldDef("model", "Modelo", true, "text"),
            new FieldDef("year", "Año", true, "number"),
            new FieldDef("color", "Color", false, "text"),
            new FieldDef("engineNumber", "N° de motor", false, "text"),
            new FieldDef("fuelType", "Combustible", false, "text"),
            new FieldDef("bodyType", "Tipo de carrocería", false, "text"),
            new FieldDef("displacementCc", "Cilindrada (cc)", false, "number"),
            new FieldDef("originCountry", "País de origen", false, "text"));

    public List<FieldDef> fields() {
        return FIELDS;
    }

    public Optional<FieldDef> byKey(String key) {
        return FIELDS.stream().filter(f -> f.key().equals(key)).findFirst();
    }

    public Optional<FieldDef> byLabel(String label) {
        return FIELDS.stream().filter(f -> f.label().equalsIgnoreCase(label.trim())).findFirst();
    }
}
