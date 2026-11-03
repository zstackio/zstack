package org.zstack.kvm.memory;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.core.Platform;

/** A memory business rejection; a @MessageSafe handler replies it with its business code. */
public class MemoryOperationException extends OperationFailureException {
    private static final String GLOBAL_ERROR_PREFIX = "ORG_ZSTACK_MEMORY_";
    private final String code;
    private final String detail;
    public MemoryOperationException(String code, String detail) {
        super(null);
        this.code = code;
        this.detail = detail;
    }
    public String getCode() { return code; }
    @Override public String getMessage() { return detail; }
    @Override public ErrorCode getErrorCode() { return toErrorCode(); }
    public ErrorCode toErrorCode() {
        MemoryErrors metadata = MemoryErrors.fromBusinessCode(code);
        int metadataId = metadata.getId();
        String globalErrorCode = GLOBAL_ERROR_PREFIX + metadataId;
        // Platform.err resolves the declared MemoryErrors metadata, increments the
        // platform error counter, and populates default-locale message/formatArgs.
        ErrorCode error = Platform.err(globalErrorCode, metadata,
                "%s", detail == null ? "" : detail);
        // Preserve the stable public business reason; ErrorFacade's declared numeric code
        // remains the source of description/elaboration and counter classification.
        error.setCode(code);
        return error;
    }
}
