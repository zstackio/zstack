package org.zstack.kvm.memory;

public enum MemoryErrors {
    MEMORY_BACKEND_PREPARATION_INVALID("MEMORY_BACKEND_PREPARATION_INVALID", 11001),
    MEMORY_MIGRATION_HOLD_PENDING("MEMORY_MIGRATION_HOLD_PENDING", 11004),
    MEMORY_RESULT_UNKNOWN("MEMORY_RESULT_UNKNOWN", 11006),
    MEMORY_INVALID_RUNTIME_SETTING("MEMORY_INVALID_RUNTIME_SETTING", 11008),
    MEMORY_BACKEND_QUERY_UNAVAILABLE("MEMORY_BACKEND_QUERY_UNAVAILABLE", 11009),
    MEMORY_INSTANCE_CHANGED("MEMORY_INSTANCE_CHANGED", 11011),
    MEMORY_INVALID_REQUEST("MEMORY_INVALID_REQUEST", 11012),
    MEMORY_LICENSE_UNAVAILABLE("MEMORY_LICENSE_UNAVAILABLE", 11013),
    MEMORY_PERMISSION_DENIED("MEMORY_PERMISSION_DENIED", 11016),
    MEMORY_HOST_BUSY("MEMORY_HOST_BUSY", 11021),
    MEMORY_INVALID_ACTION("MEMORY_INVALID_ACTION", 11024),
    MEMORY_INVALID_SCOPE("MEMORY_INVALID_SCOPE", 11027),
    MEMORY_SERVICE_NOT_STOPPED("MEMORY_SERVICE_NOT_STOPPED", 11045),
    MEMORY_HOST_OPERATION_FAILED("MEMORY_HOST_OPERATION_FAILED", 11047),
    MEMORY_RESULT_CONVERGING("MEMORY_RESULT_CONVERGING", 11048),
    MEMORY_HOST_OPERATION_CANCELED("MEMORY_HOST_OPERATION_CANCELED", 11049),
    MEMORY_HOST_OPERATION_SKIPPED("MEMORY_HOST_OPERATION_SKIPPED", 11050),
    UNKNOWN("MEMORY_UNKNOWN_ERROR", 11999);

    private static final String PREFIX = "MEMORY_ERROR";
    private final String businessCode;
    private final int id;
    private final String metadataCode;

    MemoryErrors(String businessCode, int id) {
        this.businessCode = businessCode;
        this.id = id;
        this.metadataCode = String.format("%s.%d", PREFIX, id);
    }

    public String getBusinessCode() {
        return businessCode;
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return metadataCode;
    }

    public static MemoryErrors fromBusinessCode(String code) {
        for (MemoryErrors error : values()) {
            if (error.businessCode.equals(code)) {
                return error;
            }
        }
        return UNKNOWN;
    }
}
