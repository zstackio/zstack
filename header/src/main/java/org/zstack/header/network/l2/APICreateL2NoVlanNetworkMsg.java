package org.zstack.header.network.l2;

import org.springframework.http.HttpMethod;
import org.zstack.header.rest.RestRequest;

/**
 */
@RestRequest(
        path = "/l2-no-vlan-networks",
        method = HttpMethod.POST,
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


        return msg;
    }

}
