package polimarket;
import java.lang.reflect.RecordComponent;
import java.util.*;
import java.util.stream.Collectors;

/** Serializador de salida: nunca interpreta JSON suministrado por el usuario. */
public final class Json {
    private Json() {}
    public static String encode(Object value) {
        if (value == null) return "null";
        if (value instanceof String || value instanceof Enum<?>) return quote(value.toString());
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        if (value instanceof Map<?,?> m) return m.entrySet().stream().map(e -> quote(e.getKey().toString())+":"+encode(e.getValue())).collect(Collectors.joining(",", "{", "}"));
        if (value instanceof Collection<?> c) return c.stream().map(Json::encode).collect(Collectors.joining(",", "[", "]"));
        if (value.getClass().isRecord()) {
            Map<String,Object> fields = new LinkedHashMap<>();
            try { for (RecordComponent r : value.getClass().getRecordComponents()) fields.put(r.getName(), r.getAccessor().invoke(value)); }
            catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
            if (value instanceof Domain.Existencia e) fields.put("disponibles", e.disponibles());
            return encode(fields);
        }
        throw new IllegalArgumentException("Tipo JSON no admitido: " + value.getClass());
    }
    private static String quote(String text) {
        StringBuilder out = new StringBuilder("\"");
        for (char c : text.toCharArray()) {
            switch (c) {
                case '"' -> out.append("\\\""); case '\\' -> out.append("\\\\");
                case '\n' -> out.append("\\n"); case '\r' -> out.append("\\r"); case '\t' -> out.append("\\t");
                default -> { if (c < 32) out.append(String.format("\\u%04x", (int)c)); else out.append(c); }
            }
        }
        return out.append('"').toString();
    }
}
