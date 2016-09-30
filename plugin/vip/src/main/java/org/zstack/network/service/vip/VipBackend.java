package org.zstack.network.service.vip;

import org.zstack.header.core.Completion;
import org.zstack.header.network.l3.L3NetworkInventory;

/**
 */
public interface VipBackend {
    void acquireVip(VipInventory vip, L3NetworkInventory guestNetwork, Completion completion);

    void releaseVip(VipInventory vip, Completion completion);

    String getServiceProviderTypeForVip();
}
