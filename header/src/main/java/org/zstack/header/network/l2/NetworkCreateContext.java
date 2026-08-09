package org.zstack.header.network.l2;

/** Additive context for local and projection network creation. */
public class NetworkCreateContext {
    private final NetworkOperationOrigin origin;
    private final ExternalNetworkRef externalRef;

    private NetworkCreateContext(NetworkOperationOrigin origin, ExternalNetworkRef externalRef) {
        this.origin = origin;
        this.externalRef = externalRef;
    }

    public static NetworkCreateContext api() {
        return new NetworkCreateContext(NetworkOperationOrigin.API, null);
    }

    public static NetworkCreateContext projection(NetworkOperationOrigin origin, ExternalNetworkRef ref) {
        if (origin != NetworkOperationOrigin.ZNS_PROJECTION && origin != NetworkOperationOrigin.ZNS_REFRESH) {
            throw new IllegalArgumentException("projection context requires a ZNS projection origin");
        }
        if (ref == null || ref.getResourceUuid() == null || ref.getResourceUuid().isEmpty()) {
            throw new IllegalArgumentException("projection context requires an external resource identity");
        }
        return new NetworkCreateContext(origin, ref);
    }

    public NetworkOperationOrigin getOrigin() { return origin; }
    public ExternalNetworkRef getExternalRef() { return externalRef; }
    public boolean isProjection() { return origin == NetworkOperationOrigin.ZNS_PROJECTION || origin == NetworkOperationOrigin.ZNS_REFRESH; }
}
