package org.zstack.compute.host;

import org.zstack.header.core.workflow.Flow;
import org.zstack.header.host.ConnectHostInfo;
import org.zstack.header.host.HostInventory;

/**
 * Created by GuoYi on 2019-06-03.
 */
public interface PreHostConnectExtensionPoint {
    Flow createPreHostConnectFlow(HostInventory host);

    default Flow createPreHostConnectFlow(HostInventory host, ConnectHostInfo info, boolean reconnect) {
        return createPreHostConnectFlow(host);
    }
}
