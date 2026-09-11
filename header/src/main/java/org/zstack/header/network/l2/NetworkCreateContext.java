package org.zstack.header.network.l2;

/** Additive context for local and projection network creation. */
public class NetworkCreateContext {
    public static final String APPLY_LOCAL_STEP = "APPLY_LOCAL";

    private final NetworkOperationOrigin origin;
    private final ExternalNetworkRef externalRef;
    private final String operationUuid;
    private final Long expectedConfigVersion;
    private final boolean remoteCommitted;
    private final String continuationStep;

    private NetworkCreateContext(NetworkOperationOrigin origin, ExternalNetworkRef externalRef,
                                 String operationUuid, Long expectedConfigVersion,
                                 boolean remoteCommitted, String continuationStep) {
        this.origin = origin;
        this.externalRef = externalRef;
        this.operationUuid = operationUuid;
        this.expectedConfigVersion = expectedConfigVersion;
        this.remoteCommitted = remoteCommitted;
        this.continuationStep = continuationStep;
    }

    public static NetworkCreateContext api() {
        return new NetworkCreateContext(NetworkOperationOrigin.API, null, null,
                null, false, null);
    }

    public static NetworkCreateContext cloudCommit(String operationUuid) {
        return cloudCommit(operationUuid, null, APPLY_LOCAL_STEP);
    }

    public static NetworkCreateContext cloudCommit(String operationUuid,
                                                   Long expectedConfigVersion,
                                                   String continuationStep) {
        if (operationUuid == null || operationUuid.isEmpty()) {
            throw new IllegalArgumentException("cloud commit context requires an operation UUID");
        }
        return new NetworkCreateContext(NetworkOperationOrigin.CLOUD_COMMIT, null,
                operationUuid, expectedConfigVersion, true, continuationStep);
    }

    public static NetworkCreateContext projection(NetworkOperationOrigin origin, ExternalNetworkRef ref) {
        return projection(origin, ref, null, null, null);
    }

    public static NetworkCreateContext projection(NetworkOperationOrigin origin, ExternalNetworkRef ref,
                                                  String operationUuid, String operationStep) {
        return projection(origin, ref, operationUuid, null, operationStep);
    }

    public static NetworkCreateContext projection(NetworkOperationOrigin origin, ExternalNetworkRef ref,
                                                  String operationUuid, Long expectedConfigVersion,
                                                  String continuationStep) {
        if (origin != NetworkOperationOrigin.ZNS_PROJECTION && origin != NetworkOperationOrigin.ZNS_REFRESH) {
            throw new IllegalArgumentException("projection context requires a ZNS projection origin");
        }
        if (ref == null || ref.getResourceUuid() == null || ref.getResourceUuid().isEmpty()) {
            throw new IllegalArgumentException("projection context requires an external resource identity");
        }
        return new NetworkCreateContext(origin, ref, operationUuid, expectedConfigVersion,
                true, continuationStep);
    }

    public NetworkOperationOrigin getOrigin() { return origin; }
    public ExternalNetworkRef getExternalRef() { return externalRef; }
    public String getOperationUuid() { return operationUuid; }
    public Long getExpectedConfigVersion() { return expectedConfigVersion; }
    public boolean isRemoteCommitted() { return remoteCommitted; }
    public String getContinuationStep() { return continuationStep; }
    public String getOperationStep() { return continuationStep; }
    public boolean isProjection() { return origin == NetworkOperationOrigin.ZNS_PROJECTION || origin == NetworkOperationOrigin.ZNS_REFRESH; }
    public boolean isRemoteWriteSuppressed() { return isProjection() || origin == NetworkOperationOrigin.CLOUD_COMMIT; }
}
