package org.zstack.header.server;

import java.io.Serializable;

public class ProvisionResult implements Serializable {
    private String serverUuid;
    private String networkUuid;
    private String providerType;
    private String providerResourceUuid;

    public String getServerUuid() {
        return serverUuid;
    }

    public ProvisionResult setServerUuid(String serverUuid) {
        this.serverUuid = serverUuid;
        return this;
    }

    public String getNetworkUuid() {
        return networkUuid;
    }

    public ProvisionResult setNetworkUuid(String networkUuid) {
        this.networkUuid = networkUuid;
        return this;
    }

    public String getProviderType() {
        return providerType;
    }

    public ProvisionResult setProviderType(String providerType) {
        this.providerType = providerType;
        return this;
    }

    public String getProviderResourceUuid() {
        return providerResourceUuid;
    }

    public ProvisionResult setProviderResourceUuid(String providerResourceUuid) {
        this.providerResourceUuid = providerResourceUuid;
        return this;
    }
}
