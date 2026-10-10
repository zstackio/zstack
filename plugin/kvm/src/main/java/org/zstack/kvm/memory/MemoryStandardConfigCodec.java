package org.zstack.kvm.memory;

import com.google.gson.*;
import java.math.BigDecimal;

/** Strict conversion between memory-policy scalar paths and standard config strings. */
public final class MemoryStandardConfigCodec {
    private static final Gson GSON = new Gson();
    private MemoryStandardConfigCodec() { }

    public static String encode(String path, Object value) {
        MemoryStandardField field = MemoryStandardField.forPath(path);
        Object normalized = field.normalize(value);
        if (normalized == null) { return null; }
        if (field == MemoryStandardField.KSM_ENABLED && Boolean.FALSE.equals(normalized)) { return "false"; }
        return String.valueOf(normalized);
    }

    public static Object decode(String path, String value) {
        MemoryStandardField field = MemoryStandardField.forPath(path);
        if (value == null || value.trim().isEmpty()) { return null; }
        if (field == MemoryStandardField.KSM_ENABLED && "none".equalsIgnoreCase(value.trim())) { return null; }
        try {
            switch (field.kind()) {
                case BOOLEAN:
                    if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) { throw malformed(path, value); }
                    return Boolean.parseBoolean(value);
                case LONG: return new BigDecimal(value).longValueExact();
                case DECIMAL:
                    double number = Double.parseDouble(value);
                    if (Double.isNaN(number) || Double.isInfinite(number)) { throw malformed(path, value); }
                    return number;
                case STRING:
                    Object checked = field.normalize(value);
                    return checked;
                default: throw new IllegalStateException("unknown memory field type");
            }
        } catch (ArithmeticException | NumberFormatException e) { throw malformed(path, value); }
    }

    public static Object get(String policyJson, String path) {
        MemoryStandardField field = MemoryStandardField.forPath(path);
        JsonObject root = parse(policyJson);
        String[] parts = field.path().split("\\.", -1);
        JsonElement section = root.get(parts[0]);
        if (section == null || !section.isJsonObject()) { return null; }
        JsonElement element = section.getAsJsonObject().get(parts[1]);
        if (element == null || element.isJsonNull()) { return null; }
        if (!element.isJsonPrimitive()) { throw malformed(path, element.toString()); }
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        Object value;
        switch (field.kind()) {
            case BOOLEAN:
                if (!primitive.isBoolean()) { throw malformed(path, element.toString()); }
                value = primitive.getAsBoolean(); break;
            case LONG:
                if (!primitive.isNumber()) { throw malformed(path, element.toString()); }
                try { value = new BigDecimal(primitive.getAsString()).longValueExact(); }
                catch (ArithmeticException | NumberFormatException e) { throw malformed(path, element.toString()); }
                break;
            case DECIMAL:
                if (!primitive.isNumber()) { throw malformed(path, element.toString()); }
                value = decode(path, primitive.getAsString()); break;
            case STRING:
                if (!primitive.isString()) { throw malformed(path, element.toString()); }
                value = field.normalize(primitive.getAsString()); break;
            default: throw new IllegalStateException("unknown memory field type");
        }
        return field.normalize(value);
    }

    public static String put(String policyJson, String path, Object value) {
        MemoryStandardField field = MemoryStandardField.forPath(path);
        Object normalized = field.normalize(value);
        JsonObject root = parse(policyJson);
        String[] parts = field.path().split("\\.", -1);
        JsonObject section = root.has(parts[0]) && root.get(parts[0]).isJsonObject()
                ? root.getAsJsonObject(parts[0]) : new JsonObject();
        if (normalized == null) { section.remove(parts[1]); }
        else { section.add(parts[1], GSON.toJsonTree(normalized)); }
        root.add(parts[0], section);
        return GSON.toJson(root);
    }

    private static JsonObject parse(String json) {
        try {
            JsonElement value = new JsonParser().parse(json);
            if (!value.isJsonObject()) { throw new IllegalArgumentException("memory policy must be a JSON object"); }
            return value.getAsJsonObject();
        } catch (JsonParseException | NullPointerException e) {
            throw new IllegalArgumentException("invalid memory policy JSON", e);
        }
    }

    private static IllegalArgumentException malformed(String path, String value) {
        return new IllegalArgumentException("invalid value for memory field " + path + ": " + value);
    }
}
