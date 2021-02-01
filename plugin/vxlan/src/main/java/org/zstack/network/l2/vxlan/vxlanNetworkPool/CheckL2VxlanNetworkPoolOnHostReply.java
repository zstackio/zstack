package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import org.zstack.header.network.l2.CheckL2NetworkOnHostReply;

public class CheckL2VxlanNetworkPoolOnHostReply extends CheckL2NetworkOnHostReply {
    private boolean needPopulateVtepPeers;

    public boolean isNeedPopulateVtepPeers() {
        return needPopulateVtepPeers;
    }

    public void setNeedPopulateVtepPeers(boolean needPopulateVtepPeers) {
        this.needPopulateVtepPeers = needPopulateVtepPeers;
    }
}
