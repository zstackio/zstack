package org.zstack.sdnController.header;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.network.l2.APICreateL2NetworkMsg;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/l2-networks/hardware-vxlan-pool",
        method = HttpMethod.POST,
        responseClass = APICreateL2HardwareVxlanNetworkPoolEvent.class,
        parameterName = "params"
)
public class APICreateL2HardwareVxlanNetworkPoolMsg extends APICreateL2NetworkMsg {
    @APIParam(resourceType = SdnControllerVO.class)
    private String sdnControllerUuid;

    @APIParam(required = false, numberRange = {0, 16777214})
    private Integer startVlan;

    @APIParam(required = false, numberRange = {0, 16777214})
    private Integer endVlan;

    @Override
    public String getType() {
        return SdnControllerConstant.HARDWARE_VXLAN_NETWORK_POOL_TYPE;
    }

    public String getSdnControllerUuid() {
        return sdnControllerUuid;
    }

    public void setSdnControllerUuid(String sdnControllerUuid) {
        this.sdnControllerUuid = sdnControllerUuid;
    }

    public Integer getStartVlan() {
        return startVlan;
    }

    public void setStartVlan(Integer startVlan) {
        this.startVlan = startVlan;
    }

    public Integer getEndVlan() {
        return endVlan;
    }

    public void setEndVlan(Integer endVlan) {
        this.endVlan = endVlan;
    }

    public static APICreateL2HardwareVxlanNetworkPoolMsg __example__() {
        APICreateL2HardwareVxlanNetworkPoolMsg msg = new APICreateL2HardwareVxlanNetworkPoolMsg();

        msg.setName("Test-NetPool");
        msg.setDescription("Test");
        msg.setZoneUuid(uuid());
        msg.setSdnControllerUuid(uuid());
        msg.setPhysicalInterface("bond0");

        return msg;
    }
}
