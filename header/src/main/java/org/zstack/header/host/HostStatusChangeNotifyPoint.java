package org.zstack.header.host;

public interface HostStatusChangeNotifyPoint {
    void notifyHostConnectionStateChange(HostInventory host, HostStatus previousState, HostStatus currentState);
}
