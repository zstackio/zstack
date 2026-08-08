package org.zstack.header.network;

import java.util.Collections;
import java.util.Map;

/** Immutable description of a versioned network mutation shared by owner and controller MNs. */
public final class NetworkConfigMutation {
    private final String resourceUuid;
    private final String operationUuid;
    private final long expectedConfigVersion;
    private final String operationStep;
    private final Map<String, Object> fields;

    public NetworkConfigMutation(String resourceUuid, String operationUuid, long expectedConfigVersion,
                                 String operationStep, Map<String, Object> fields) {
        this.resourceUuid = resourceUuid;
        this.operationUuid = operationUuid;
        this.expectedConfigVersion = expectedConfigVersion;
        this.operationStep = operationStep;
        this.fields = fields == null ? Collections.emptyMap() : Collections.unmodifiableMap(new java.util.HashMap<>(fields));
    }

    public String getResourceUuid() { return resourceUuid; }
    public String getOperationUuid() { return operationUuid; }
    public long getExpectedConfigVersion() { return expectedConfigVersion; }
    public String getOperationStep() { return operationStep; }
    public Map<String, Object> getFields() { return fields; }
}
