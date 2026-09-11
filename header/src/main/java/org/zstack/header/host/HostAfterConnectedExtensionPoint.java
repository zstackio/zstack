package org.zstack.header.host;

/**
 * Created by frank on 9/19/2015.
 */
public interface HostAfterConnectedExtensionPoint {
    void afterHostConnected(HostInventory host);

    default void afterHostConnected(HostInventory host, ConnectHostInfo info) {
        afterHostConnected(host);
    }
}
