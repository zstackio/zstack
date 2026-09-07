package org.zstack.core;

public class ManagedComponentEndpoint {
    private final String remoteAddress;
    private final String currentManagementNodeAddress;

    public ManagedComponentEndpoint(String remoteAddress, String currentManagementNodeAddress) {
        this.remoteAddress = remoteAddress;
        this.currentManagementNodeAddress = currentManagementNodeAddress;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public String getCurrentManagementNodeAddress() {
        return currentManagementNodeAddress;
    }
}
