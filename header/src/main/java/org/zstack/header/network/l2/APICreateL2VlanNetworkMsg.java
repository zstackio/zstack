package org.zstack.header.network.l2;

import org.zstack.header.message.APIParam;

/**
 * @api
 * create a l2VlanNetwork
 *
 * @category l2network
 *
 * @since 0.1.0
 *
 * @cli
 *
 * @httpMsg
 * {
"org.zstack.header.network.l2.APICreateL2VlanNetworkMsg": {
"vlan": 10,
"name": "TestL2VlanNetwork",
"description": "Test",
"zoneUuid": "d81c3d3d008e46038b8a38fee595fe41",
"physicalInterface": "eth0",
"type": "L2VlanNetwork",
"session": {
"uuid": "8be9f1f0d55b4f1cb6a088c376dc8128"
}
}
}
 *
 * @msg
 * {
"org.zstack.header.network.l2.APICreateL2VlanNetworkMsg": {
"vlan": 10,
"name": "TestL2VlanNetwork",
"description": "Test",
"zoneUuid": "d81c3d3d008e46038b8a38fee595fe41",
"physicalInterface": "eth0",
"type": "L2VlanNetwork",
"session": {
"uuid": "8be9f1f0d55b4f1cb6a088c376dc8128"
},
"timeout": 1800000,
"id": "a0a5829f12fe4c45855967f6fa0c0afa",
"serviceId": "api.portal"
}
}
 *
 * @result
 * see :ref:`APICreateL2VlanNetworkEvent`
 */
public class APICreateL2VlanNetworkMsg extends APICreateL2NetworkMsg {
    /**
     * @desc vlan id
     */
    @APIParam(numberRange = {0, 4094})
    private Integer vlan;

    public int getVlan() {
        return vlan;
    }

    public void setVlan(int vlan) {
        this.vlan = vlan;
    }
    
    @Override
    public String getType() {
        return L2NetworkConstant.L2_VLAN_NETWORK_TYPE;
    }
}
