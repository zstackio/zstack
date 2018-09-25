package org.zstack.network.l2.vxlan.vtep;

import org.springframework.http.HttpMethod;
import org.zstack.header.host.HostVO;
import org.zstack.header.identity.Action;
import org.zstack.header.message.*;
import org.zstack.header.network.l2.APICreateL2NetworkMsg;
import org.zstack.header.network.l2.L2NetworkMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.zone.ZoneVO;
import org.zstack.network.l2.vxlan.vxlanNetwork.APICreateL2VxlanNetworkEvent;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkConstant;
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanNetworkPoolConstant;
import org.zstack.network.l2.vxlan.vxlanNetworkPool.VxlanNetworkPoolVO;

@Action(category = VxlanNetworkPoolConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/l2-networks/vxlan/vteps",
        method = HttpMethod.POST,
        responseClass = APICreateVxlanVtepEvent.class,
        parameterName = "params"
)
public class APICreateVxlanVtepMsg extends  APICreateMessage implements L2NetworkMessage {
    @APIParam(resourceType = HostVO.class, checkAccount = true)
    private String hostUuid;

    @APIParam(resourceType = VxlanNetworkPoolVO.class, checkAccount = true)
    private String poolUuid;

    private String vtepIp;

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getVtepIp() {
        return vtepIp;
    }

    public void setVtepIp(String vtepIp) {
        this.vtepIp = vtepIp;
    }

    public String getPoolUuid() {
        return poolUuid;
    }

    public void setPoolUuid(String poolUuid) {
        this.poolUuid = poolUuid;
    }

    @Override
    public String getL2NetworkUuid() {
        return poolUuid;
    }

    public static APICreateVxlanVtepMsg __example__() {
        APICreateVxlanVtepMsg msg = new APICreateVxlanVtepMsg();

        msg.setHostUuid(uuid());
        msg.setVtepIp("1.1.1.1");
        msg.setPoolUuid(uuid());

        return msg;
    }
}
