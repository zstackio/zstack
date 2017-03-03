package org.zstack.network.l2.vxlan.vxlanNetwork;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.network.l2.APICreateL2NetworkEvent;
import org.zstack.header.network.l2.APICreateL2NetworkMsg;
import org.zstack.header.rest.RestRequest;
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanNetworkPool;
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanNetworkPoolVO;

/**
 * @api create a VxlanNetwork
 * @category l2network
 * @cli
 * @httpMsg {
 * "org.zstack.header.network.l2.APICreateVxlanNetworkMsg": {
 * "vni": 10,
 * "name": "TestVxlanNetwork",
 * "description": "Test",
 * "zoneUuid": "d81c3d3d008e46038b8a38fee595fe41",
 * "physicalInterface": "eth0.1100",
 * "vtepCidr": "172.20.0.0/24",
 * "poolUuid": "",
 * "type": "VxlanNetwork",
 * "session": {
 * "uuid": "8be9f1f0d55b4f1cb6a088c376dc8128"
 * }
 * }
 * }
 * @msg {
 * "org.zstack.header.network.l2.APICreateVxlanNetworkMsg": {
 * "vni": 10,
 * "name": "TestVxlanNetwork",
 * "description": "Test",
 * "zoneUuid": "d81c3d3d008e46038b8a38fee595fe41",
 * "physicalInterface": "eth0.1100",
 * "vtepCidr": "172.20.0.0/24",
 * "poolUuid": "",
 * "type": "VxlanNetwork",
 * "session": {
 * "uuid": "8be9f1f0d55b4f1cb6a088c376dc8128"
 * },
 * "timeout": 1800000,
 * "id": "a0a5829f12fe4c45855967f6fa0c0afa",
 * "serviceId": "api.portal"
 * }
 * }
 * @result see :ref:`APICreateVxlanNetworkEvent`
 * @since 1.10.0
 */
@RestRequest(
        path = "/l2-networks/vxlan",
        method = HttpMethod.POST,
        responseClass = APICreateL2VxlanNetworkEvent.class,
        parameterName = "params"
)
public class APICreateVxlanNetworkMsg extends APICreateL2NetworkMsg {
    @APIParam(required = false, numberRange = {1, 16777215})
    private Integer vni;

    @APIParam(required = false, maxLength = 32)
    private String vtepCidr;

    @APIParam(required = false, resourceType = VxlanNetworkPoolVO.class)
    private String poolUuid;

    @Override
    public String getType() {
        return VxlanNetworkConstant.VXLAN_NETWORK_TYPE;
    }

    public int getVni() {
        return vni;
    }

    public void setVni(int vni) {
        this.vni = vni;
    }

    public String getVtepCidr() {
        return vtepCidr;
    }

    public void setVtepCidr(String vtepCidr) {
        this.vtepCidr = vtepCidr;
    }

    public String getPoolUuid() {
        return poolUuid;
    }

    public void setPoolUuid(String poolUuid) {
        this.poolUuid = poolUuid;
    }

    public static APICreateVxlanNetworkMsg __example__() {
        APICreateVxlanNetworkMsg msg = new APICreateVxlanNetworkMsg();

        msg.setName("Test-Net");
        msg.setVni(10);
        msg.setDescription("Test");
        msg.setZoneUuid(uuid());
        msg.setPhysicalInterface("eth0");
        msg.setVtepCidr("172.20.0.0/24");
        msg.setPoolUuid("");

        return msg;
    }
}
