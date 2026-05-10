package org.zstack.header.server;

import org.springframework.http.HttpMethod;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@Action(adminOnly = true, category = PhysicalServerConstant.SERVER_POOL_ACTION_CATEGORY)
@RestRequest(path = "/clusters/{clusterUuid}/server-pool/actions", method = HttpMethod.PUT, isAction = true, responseClass = APIChangeClusterServerPoolEvent.class)
public class APIChangeClusterServerPoolMsg extends APIMessage {
    @APIParam(resourceType = ClusterVO.class)
    private String clusterUuid;

    @APIParam(resourceType = ServerPoolVO.class)
    private String serverPoolUuid;

    public String getClusterUuid() { return clusterUuid; }
    public void setClusterUuid(String clusterUuid) { this.clusterUuid = clusterUuid; }
    public String getServerPoolUuid() { return serverPoolUuid; }
    public void setServerPoolUuid(String serverPoolUuid) { this.serverPoolUuid = serverPoolUuid; }

    public static APIChangeClusterServerPoolMsg __example__() {
        APIChangeClusterServerPoolMsg msg = new APIChangeClusterServerPoolMsg();
        msg.setClusterUuid(uuid());
        msg.setServerPoolUuid(uuid());
        return msg;
    }
}
