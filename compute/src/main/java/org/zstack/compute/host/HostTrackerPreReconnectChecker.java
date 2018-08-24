package org.zstack.compute.host;

public interface HostTrackerPreReconnectChecker {
    boolean canDoReconnect(String hostUuid);

    String getHypervisorType();
}
