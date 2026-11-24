package cl.automata.vehiclecapture.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.LinkedHashMap;
import java.util.Map;

/** Convertidores Map <-> JSON (texto) portables entre H2 y PostgreSQL. */
public final class JsonConverters {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonConverters() {}

    private static String write(Object o) {
        try {
            return MAPPER.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static <T> T read(String s, TypeReference<T> type) {
        try {
            return MAPPER.readValue(s, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    @Converter
    public static class StringMap implements AttributeConverter<Map<String, String>, String> {
        @Override
        public String convertToDatabaseColumn(Map<String, String> attribute) {
            return attribute == null ? null : write(attribute);
        }

        @Override
        public Map<String, String> convertToEntityAttribute(String db) {
            return db == null ? new LinkedHashMap<>() : read(db, new TypeReference<LinkedHashMap<String, String>>() {});
        }
    }

    @Converter
    public static class DoubleMap implements AttributeConverter<Map<String, Double>, String> {
        @Override
        public String convertToDatabaseColumn(Map<String, Double> attribute) {
            return attribute == null ? null : write(attribute);
        }

        @Override
        public Map<String, Double> convertToEntityAttribute(String db) {
            return db == null ? new LinkedHashMap<>() : read(db, new TypeReference<LinkedHashMap<String, Double>>() {});
        }
    }
}
