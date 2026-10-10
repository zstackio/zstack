package org.zstack.core.config;

import org.zstack.header.message.APIMessage;
import java.util.Objects;
import java.util.UUID;

/** Correlation from the real ingress, not a synthesized client CAS or target selection. */
public final class ConfigMutationContext {
    private final String requestId, actor;
    private final boolean internal;

    private ConfigMutationContext(String requestId, String actor, boolean internal) {
        this.requestId = Objects.requireNonNull(requestId, "requestId");
        this.actor = Objects.requireNonNull(actor, "actor"); this.internal = internal;
    }

    public static ConfigMutationContext fromApiMessage(APIMessage message) {
        if (message.getSession() == null || message.getSession().getAccountUuid() == null) {
            throw new IllegalArgumentException("an authenticated session is required for a configuration mutation");
        }
        return new ConfigMutationContext(message.getId(), message.getSession().getAccountUuid()
                + ":" + String.valueOf(message.getSession().getUserUuid()), false);
    }

    /** Used by server-side setters, never populated from a REST field. */
    public static ConfigMutationContext internal() {
        return new ConfigMutationContext(UUID.randomUUID().toString().replace("-", ""),
                "system:standard-configuration", true);
    }

    public String getRequestId() { return requestId; }
    public String getActor() { return actor; }
    public boolean isInternal() { return internal; }
}
