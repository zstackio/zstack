package org.zstack.header.network.l2;

/** Stable external identity carried by a projection create without changing the public API message. */
public class ExternalNetworkRef {
    private final String resourceUuid;
    private final String accountUuid;

    public ExternalNetworkRef(String resourceUuid, String accountUuid) {
        this.resourceUuid = resourceUuid;
        this.accountUuid = accountUuid;
    }

    public String getResourceUuid() { return resourceUuid; }
    public String getAccountUuid() { return accountUuid; }
}
