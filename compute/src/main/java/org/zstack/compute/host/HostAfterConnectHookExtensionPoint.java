package org.zstack.compute.host;

import org.zstack.header.core.workflow.Flow;
import org.zstack.header.host.ConnectHostInfo;
import org.zstack.header.host.HostInventory;

public interface HostAfterConnectHookExtensionPoint {
    Flow createAfterConnectHookFlow(HostInventory host, ConnectHostInfo info, boolean reconnect);
}
