package org.zstack.network.service.vip;

import java.util.List;

public interface AfterAcquireVipExtensionPoint {
    void afterAcquireVip(List<VipInventory> vipList);
}
