package org.zstack.compute.host;

import org.zstack.header.host.HostInventory;

/**
 * Created by miao on 16-7-20.
 */
public interface PostHostConnectExtensionPoint {
    void postHostConnect(HostInventory host);
}
