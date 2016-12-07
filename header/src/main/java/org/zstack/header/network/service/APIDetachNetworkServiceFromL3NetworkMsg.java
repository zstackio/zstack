package org.zstack.header.network.service;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.network.l3.L3NetworkConstant;
import org.zstack.header.network.l3.L3NetworkMessage;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.rest.RestRequest;

import java.util.List;
import java.util.Map;

/**
 * Created by frank on 1/4/2016.
 */
@Action(category = L3NetworkConstant.ACTION_CATEGORY)
@RestRequest(
        path = "/l3-networks/{l3NetworkUuid}/network-services",
        method = HttpMethod.DELETE,
        parameterName = "params",
        responseClass = APIDetachNetworkServiceFromL3NetworkEvent.class
)
public class APIDetachNetworkServiceFromL3NetworkMsg extends APIMessage implements L3NetworkMessage {
    /**
     * @desc l3Network uuid
     */
    @APIParam(resourceType = L3NetworkVO.class, checkAccount = true, operationTarget = true)
    private String l3NetworkUuid;
    /**
     * @desc a map where key is network service provider uuid and value is list of network service types
     */
    @APIParam
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
}
