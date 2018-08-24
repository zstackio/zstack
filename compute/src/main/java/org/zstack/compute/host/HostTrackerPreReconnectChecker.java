package org.zstack.compute.host;

public interface HostTrackerPreReconnectChecker {
    Boolean canDoReconnect(String hostUuid);

    String getHypervisorType();
}
