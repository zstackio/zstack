package org.zstack.header.network.l2;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.OverriddenApiParam;
import org.zstack.header.message.OverriddenApiParams;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;
import org.zstack.header.zone.ZoneVO;

/**
 */
@TagResourceType(L2NetworkVO.class)
@OverriddenApiParams({
        @OverriddenApiParam(field = "physicalInterface", param = @APIParam(maxLength = 1024, required = false))
})
@RestRequest(
        path = "/l2-networks/no-vlan",
        method = "POST",
        responseClass = APICreateL2NetworkEvent.class,
        parameterName = "params"
)
public class APICreateL2NoVlanNetworkMsg extends APICreateL2NetworkMsg {
    @Override
    public String getType() {
        return L2NetworkConstant.L2_NO_VLAN_NETWORK_TYPE;
    }
 
    public static APICreateL2NoVlanNetworkMsg __example__() {
        APICreateL2NoVlanNetworkMsg msg = new APICreateL2NoVlanNetworkMsg();

        msg.setName("Test-Net");
        msg.setDescription("Test");
        msg.setZoneUuid(uuid());
        msg.setPhysicalInterface("eth0");

        return msg;
    }

}
