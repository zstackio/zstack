package org.zstack.header.secret;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.MessageReply;

/** Reply for SecretHostGetMsg. */
public class SecretHostGetReply extends MessageReply {
    public static final String ERROR_CODE_SECRET_NOT_FOUND = "KEY_AGENT_SECRET_NOT_FOUND";

    private String secretUuid;

    public String getSecretUuid() {
        return secretUuid;
    }

    public void setSecretUuid(String secretUuid) {
        this.secretUuid = secretUuid;
    }

    /**
     * Distinguish "secret not present on host" (idempotent re-define needed)
     * from genuine RPC / agent failures. key-agent's not-found surfaces either
     * as the canonical {@link #ERROR_CODE_SECRET_NOT_FOUND} code or embedded
     * in {@code details} depending on the bus hop.
     */
    public static boolean isSecretNotFound(ErrorCode err) {
        if (err == null) {
            return false;
        }
        if (ERROR_CODE_SECRET_NOT_FOUND.equals(err.getCode())) {
            return true;
        }
        String details = err.getDetails();
        return details != null && details.contains(ERROR_CODE_SECRET_NOT_FOUND);
    }
}
