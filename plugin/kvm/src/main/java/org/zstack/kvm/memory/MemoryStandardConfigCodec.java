package org.zstack.kvm.memory;

/** Strict conversion between memory-policy scalar paths and standard config strings. */
public final class MemoryStandardConfigCodec {
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
        if (field.isFeatureSwitch() && "none".equalsIgnoreCase(value.trim())) { return null; }
        String problem = problem(path, value);
        if (problem != null) { throw new IllegalArgumentException(problem); }
        switch (field.kind()) {
            case BOOLEAN: return Boolean.parseBoolean(value);
            case LONG: return MemoryObservationRules.parseExactLong(value);
            case STRING: return field.normalize(value);
            default: throw new IllegalStateException("unknown memory field type");
        }
    }

    /** Why the stored text is not a value of the field, or null when it is (blank and none included). */
    static String problem(String path, String value) {
        MemoryStandardField field = MemoryStandardField.forPath(path);
        if (value == null || value.trim().isEmpty() || field.isFeatureSwitch() && "none".equalsIgnoreCase(value.trim())) {
            return null;
        }
        boolean valid = field.kind() == MemoryStandardField.Kind.BOOLEAN ? "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)
                : field.kind() != MemoryStandardField.Kind.LONG || MemoryObservationRules.parseExactLong(value) != null;
        return valid ? null : "invalid value for memory field " + path + ": " + value;
    }
}
