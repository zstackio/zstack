package org.zstack.network.l2;

import org.zstack.header.network.l2.L2NetworkInventory;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.network.l2.L2VlanNetworkInventory;
import org.zstack.header.network.l2.L2VlanNetworkVO;

public class L2VlanNetwork extends L2NoVlanNetwork {
    
    public L2VlanNetwork(L2NetworkVO self) {
        super(self);
    }
    
    private L2VlanNetworkVO getSelf() {
        return (L2VlanNetworkVO) self;
    }

    @Override
    protected L2NetworkInventory getSelfInventory() {
        return L2VlanNetworkInventory.valueOf(getSelf());
    }
    
}
