package org.zstack.network.service.vip;

import org.zstack.header.Service;
import org.zstack.header.core.Completion;
import org.zstack.header.network.l3.L3NetworkInventory;

/**
 */
public interface VipManager extends Service {
    VipBackend getVipBackend(String providerType);

    void saveVipInfo(String vipUuid, String networkServiceType, String peerL3NetworkUuid);

    void lockAndAcquireVip(VipInventory vip, L3NetworkInventory peerL3Network,
                           String networkServiceType, String networkServiceProviderType, Completion completion);

    void releaseAndUnlockVip(VipInventory vip, Completion completion);

    void releaseAndUnlockVip(VipInventory vip, boolean releasePeerL3Network, Completion completion);

    void acquireVip(VipInventory vip, L3NetworkInventory peerL3Network, String networkServiceProviderType, Completion completion);

    void releaseVip(VipInventory vip, boolean releasePeerL3Network, Completion completion);

    void releaseVip(VipInventory vip, Completion completion);

    void lockVip(VipInventory vip,  String networkServiceType);

    void unlockVip(VipInventory vip);

    VipReleaseExtensionPoint getVipReleaseExtensionPoint(String useFor);
}
