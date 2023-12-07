package org.zstack.sdnController.header;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.*;
import org.zstack.header.network.l2.APICreateL2NetworkMsg;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.zone.ZoneVO;
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanNetworkPoolConstant;

@Action(category = VxlanNetworkPoolConstant.ACTION_CATEGORY)
@OverriddenApiParams({
        @OverriddenApiParam(field = "physicalInterface", param = @APIParam(maxLength = 1024, required = false)),
        @OverriddenApiParam(field = "zoneUuid", param = @APIParam(maxLength = 1024, required = false, resourceType = ZoneVO.class))
})
@RestRequest(
        path = "/l2-networks/hardware-vxlan",
        method = HttpMethod.POST,
        responseClass = APICreateL2HardwareVxlanNetworkEvent.class,
        parameterName = "params"
)
public class APICreateL2HardwareVxlanNetworkMsg extends APICreateL2NetworkMsg {
    @APIParam(required = false, numberRange = {1, 16777214})
    private Integer vni;

    @APIParam(required = true, resourceType = HardwareL2VxlanNetworkPoolVO.class)
    private String poolUuid;

    public Integer getVni() {
        return vni;
    }

    @Override
    public String getType() {
        return SdnControllerConstant.HARDWARE_VXLAN_NETWORK_TYPE;
    }

    public void setVni(int vni) {
        this.vni = vni;
    }

    public String getPoolUuid() {
        return poolUuid;
    }

    public void setPoolUuid(String poolUuid) {
        this.poolUuid = poolUuid;
    }

    public static APICreateL2HardwareVxlanNetworkMsg __example__() {
        APICreateL2HardwareVxlanNetworkMsg msg = new APICreateL2HardwareVxlanNetworkMsg();

        msg.setName("Test-Net");
        msg.setVni(10);
        msg.setDescription("Test");
        msg.setZoneUuid(uuid());
        msg.setPoolUuid(uuid());

        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APICreateL2HardwareVxlanNetworkEvent) rsp).getInventory().getUuid() : "", L2NetworkVO.class);
    }
}
