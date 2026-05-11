package org.zstack.header.server;

import org.springframework.http.HttpMethod;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@Action(adminOnly = true, category = PhysicalServerConstant.ACTION_CATEGORY)
@RestRequest(
    path = "/provision-networks/{networkUuid}/clusters/{clusterUuid}",
    method = HttpMethod.POST,
    responseClass = APIAttachProvisionNetworkToClusterEvent.class
)
public class APIAttachProvisionNetworkToClusterMsg extends APIMessage {
    @APIParam(resourceType = PhysicalServerProvisionNetworkVO.class)
    private String networkUuid;

    @APIParam(resourceType = ClusterVO.class)
    private String clusterUuid;

    public String getNetworkUuid() { return networkUuid; }
    public void setNetworkUuid(String networkUuid) { this.networkUuid = networkUuid; }
    public String getClusterUuid() { return clusterUuid; }
    public void setClusterUuid(String clusterUuid) { this.clusterUuid = clusterUuid; }

    public static APIAttachProvisionNetworkToClusterMsg __example__() {
        APIAttachProvisionNetworkToClusterMsg msg = new APIAttachProvisionNetworkToClusterMsg();
        msg.setNetworkUuid(uuid());
        msg.setClusterUuid(uuid());
        return msg;
    }
}
