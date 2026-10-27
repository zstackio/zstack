package org.zstack.kvm.memory;

import java.math.BigDecimal;

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
        try {
            switch (field.kind()) {
                case BOOLEAN:
                    if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) { throw malformed(path, value); }
                    return Boolean.parseBoolean(value);
                case LONG: return new BigDecimal(value).longValueExact();
                case STRING:
                    Object checked = field.normalize(value);
                    return checked;
                default: throw new IllegalStateException("unknown memory field type");
            }
        } catch (ArithmeticException | NumberFormatException e) { throw malformed(path, value); }
    }

    private static IllegalArgumentException malformed(String path, String value) {
        return new IllegalArgumentException("invalid value for memory field " + path + ": " + value);
    }
}
