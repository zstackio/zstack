package org.zstack.kvm.memory;

import com.google.gson.*;

import java.util.*;

/** Splits ordinary scalar values from the sparse specialized policy row. */
final class MemoryStandardPolicy {
    private static final Gson GSON = new Gson();

    static final class Parts {
        final String specialized;
        final Map<String, Object> standardValues;
        Parts(String specialized, Map<String, Object> standardValues) {
            this.specialized = specialized;
            this.standardValues = Collections.unmodifiableMap(new LinkedHashMap<>(standardValues));
        }
    }

    static final class ClearParts {
        final List<String> specializedPaths;
        final List<String> standardPaths;
        ClearParts(List<String> specializedPaths, List<String> standardPaths) {
            this.specializedPaths = Collections.unmodifiableList(specializedPaths);
            this.standardPaths = Collections.unmodifiableList(standardPaths);
        }
    }

    private MemoryStandardPolicy() { }

    static Parts split(String policy, String scope) {
        JsonObject root = object(policy);
        Map<String, Object> standard = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> sectionEntry : new ArrayList<>(root.entrySet())) {
            if (!sectionEntry.getValue().isJsonObject()) { continue; }
            String sectionName = sectionEntry.getKey();
            JsonObject section = sectionEntry.getValue().getAsJsonObject();
            for (Map.Entry<String, JsonElement> fieldEntry : new ArrayList<>(section.entrySet())) {
                String path = sectionName + "." + fieldEntry.getKey();
                MemoryStandardField field = maybeField(path);
                if (field == null) { continue; }
                requireScope(field, scope);
                if (fieldEntry.getValue() == null || fieldEntry.getValue().isJsonNull()) {
                    throw new IllegalArgumentException("null is not a standard memory override: " + path);
                }
                Object value = MemoryStandardConfigCodec.get(root.toString(), path);
                if (value == null) { throw new IllegalArgumentException("invalid standard memory value: " + path); }
                standard.put(path, value);
                section.remove(fieldEntry.getKey());
            }
        }
        return new Parts(root.toString(), standard);
    }

    static ClearParts splitClearPaths(List<String> paths, String scope) {
        if (paths == null || paths.isEmpty()) { throw new IllegalArgumentException("clearOverrideFields must be explicit"); }
        List<String> specialized = new ArrayList<>(), standard = new ArrayList<>();
        for (String path : paths) {
            MemoryStandardField field = maybeField(path);
            if (field == null) { specialized.add(path); }
            else { requireScope(field, scope); standard.add(path); }
        }
        return new ClearParts(specialized, standard);
    }

    static String combine(String specialized, String standard) {
        JsonObject result = object(specialized);
        JsonObject values = object(standard);
        for (Map.Entry<String, JsonElement> section : values.entrySet()) {
            if (section.getValue().isJsonObject()) {
                JsonObject target = result.has(section.getKey()) && result.get(section.getKey()).isJsonObject()
                        ? result.getAsJsonObject(section.getKey()) : new JsonObject();
                for (Map.Entry<String, JsonElement> value : section.getValue().getAsJsonObject().entrySet()) {
                    target.add(value.getKey(), value.getValue());
                }
                result.add(section.getKey(), target);
            } else { result.add(section.getKey(), section.getValue().deepCopy()); }
        }
        return result.toString();
    }

    static String set(String policy, String path, Object value) {
        return MemoryStandardConfigCodec.put(policy, path, value);
    }

    static JsonObject object(String json) {
        JsonElement parsed = new JsonParser().parse(json);
        if (parsed == null || !parsed.isJsonObject()) { throw new IllegalArgumentException("memory policy must be a JSON object"); }
        return parsed.getAsJsonObject();
    }

    private static MemoryStandardField maybeField(String path) {
        try { return MemoryStandardField.forPath(path); }
        catch (IllegalArgumentException notStandard) { return null; }
    }

    private static void requireScope(MemoryStandardField field, String scope) {
        MemoryStandardField.Scope parsed;
        try { parsed = MemoryStandardField.Scope.valueOf(scope.toUpperCase(Locale.ROOT)); }
        catch (RuntimeException e) { throw new IllegalArgumentException("unsupported policy scope: " + scope); }
        if (!field.allows(parsed)) { throw new IllegalArgumentException("field " + field.path() + " is not supported at " + scope + " scope"); }
    }
}
