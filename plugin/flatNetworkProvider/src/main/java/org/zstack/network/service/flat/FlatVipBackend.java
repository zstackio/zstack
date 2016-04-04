package org.zstack.network.service.flat;

import org.zstack.header.core.Completion;
import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.network.service.vip.VipBackend;
import org.zstack.network.service.vip.VipInventory;

/**
 * Created by xing5 on 2016/4/4.
 */
public class FlatVipBackend implements VipBackend {
    @Override
    public void acquireVip(VipInventory vip, L3NetworkInventory guestNetwork, Completion completion) {
        // fake, the VIP is created when the EIP is created
        completion.success();
    }

    @Override
    public void releaseVip(VipInventory vip, Completion completion) {
        // fake, the VIP is deleted when the EIP is deleted
        completion.success();
    }

    @Override
    public String getServiceProviderTypeForVip() {
        return FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING;
    }
}
