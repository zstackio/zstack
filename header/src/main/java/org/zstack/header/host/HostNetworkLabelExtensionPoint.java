package org.zstack.header.host;

import org.zstack.header.core.Completion;

/**
 * Created by boce.wang on 10/25/2024.
 */
public interface HostNetworkLabelExtensionPoint {
    void deleteHostNetworkLabel(HostNetworkLabelInventory hostNetworkLabel, Completion completion);

    void updateHostNetworkLabel(HostNetworkLabelInventory hostNetworkLabel, String newLabel, Completion completion);
}
