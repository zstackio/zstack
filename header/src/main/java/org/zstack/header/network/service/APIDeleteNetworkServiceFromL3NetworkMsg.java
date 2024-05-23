package org.zstack.header.network.service;

import org.springframework.http.HttpMethod;
import org.zstack.header.MapField;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.network.l3.L3NetworkConstant;
import org.zstack.header.network.l3.L3NetworkMessage;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.rest.RestRequest;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Action(category = L3NetworkConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/l3-networks/{l3NetworkUuid}/network-services/delete",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APIDeleteNetworkServiceFromL3NetworkEvent.class
)
public class APIDeleteNetworkServiceFromL3NetworkMsg extends APIMessage implements L3NetworkMessage {
    /**
     * @desc l3Network uuid
     */
    @APIParam(resourceType = L3NetworkVO.class, checkAccount = true, operationTarget = true)
    private String l3NetworkUuid;
    /**
     * @desc a map where key is network service provider uuid and value is list of network service types
     */
    @APIParam
    @MapField(keyType = String.class, valueType = List.class)
    private Map<String, List<String>> networkServices;

    @Override
    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    public Map<String, List<String>> getNetworkServices() {
        return networkServices;
    }

    public void setNetworkServices(Map<String, List<String>> networkServices) {
        this.networkServices = networkServices;
    }
 
    public static APIDeleteNetworkServiceFromL3NetworkMsg __example__() {
        APIDeleteNetworkServiceFromL3NetworkMsg msg = new APIDeleteNetworkServiceFromL3NetworkMsg();

        Map<String, List<String>> m = new HashMap<>();
        m.put(uuid(), Arrays.asList("PortForwarding","EIP"));

        msg.setL3NetworkUuid(uuid());
        msg.setNetworkServices(m);


        return msg;
    }
}
