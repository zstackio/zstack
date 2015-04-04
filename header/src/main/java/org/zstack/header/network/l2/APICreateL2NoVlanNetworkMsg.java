package org.zstack.header.network.l2;

/**
 */
public class APICreateL2NoVlanNetworkMsg extends APICreateL2NetworkMsg {
    @Override
    public String getType() {
        return L2NetworkConstant.L2_NO_VLAN_NETWORK_TYPE;
    }
}
