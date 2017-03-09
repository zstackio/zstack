package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.network.l2.APICreateL2NetworkMsg;
import org.zstack.header.rest.RestRequest;

@RestRequest(
        path = "/l2-networks/vxlan-pool",
        method = HttpMethod.POST,
        responseClass = APICreateL2VxlanNetworkPoolEvent.class,
        parameterName = "params"
)
public class APICreateL2VxlanNetworkPoolMsg extends APICreateL2NetworkMsg {

    @APIParam(required = false, maxLength = 1024)
    private String physicalInterface;

    @Override
    public String getType() {
        return VxlanNetworkPoolConstant.VXLAN_NETWORK_POOL_TYPE;
    }

    public static APICreateL2VxlanNetworkPoolMsg __example__() {
        APICreateL2VxlanNetworkPoolMsg msg = new APICreateL2VxlanNetworkPoolMsg();

        msg.setName("Test-NetPool");
        msg.setDescription("Test");
        msg.setZoneUuid(uuid());

        return msg;
    }

}
