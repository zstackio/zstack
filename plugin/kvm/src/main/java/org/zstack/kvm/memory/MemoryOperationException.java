package org.zstack.kvm.memory;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.core.Platform;

public class MemoryOperationException extends IllegalArgumentException {
    private static final String ERROR_PREFIX = "MEMORY_ERROR";
    private static final String GLOBAL_ERROR_PREFIX = "ORG_ZSTACK_MEMORY_";
    private final String code;
    public MemoryOperationException(String code, String detail) {
        super(detail);
        this.code = code;
    }
    public String getCode() { return code; }
    public ErrorCode toErrorCode() {
        MemoryErrors metadata = MemoryErrors.fromBusinessCode(code);
        int metadataId = metadata.getId();
        String globalErrorCode = GLOBAL_ERROR_PREFIX + metadataId;
        // Platform.err resolves the declared MemoryErrors metadata, increments the
        // platform error counter, and populates default-locale message/formatArgs.
        ErrorCode error = Platform.err(globalErrorCode, metadata,
                "%s", getMessage() == null ? "" : getMessage());
        // Preserve the stable public business reason; ErrorFacade's declared numeric code
        // remains the source of description/elaboration and counter classification.
        error.setCode(code);
        return error;
    }
}
