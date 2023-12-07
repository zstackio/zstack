package org.zstack.network.l2.vxlan.vxlanNetworkPool;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.network.l2.L2NetworkMessage;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.message.APIDeleteMessage;

@RestRequest(
        path = "/l2-networks/{l2NetworkUuid}/clusters/{clusterUuid}/delete/remote-vtep-ip",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteVxlanPoolRemoteVtepEvent.class
)
public class APIDeleteVxlanPoolRemoteVtepMsg extends APIDeleteMessage implements L2NetworkMessage {

    @APIParam
    private String l2NetworkUuid;

    @APIParam
    private String clusterUuid;
    
    @APIParam(maxLength = 15)
    private String remoteVtepIp;

    @Override
    public String getL2NetworkUuid() {
        return l2NetworkUuid;
    }

    public String getClusterUuid() {
        return clusterUuid;
    }

    public String getRemoteVtepIp() {
        return remoteVtepIp;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }

    public void setL2NetworkUuid(String l2NetworkUuid) {
        this.l2NetworkUuid = l2NetworkUuid;
    }

    public void setRemoteVtepIp(String remoteVtepIp) {
        this.remoteVtepIp = remoteVtepIp;
    }

    public static APIDeleteVxlanPoolRemoteVtepMsg __example__() {
        APIDeleteVxlanPoolRemoteVtepMsg msg = new APIDeleteVxlanPoolRemoteVtepMsg();

        msg.setL2NetworkUuid(uuid());
        msg.setClusterUuid(uuid());
        msg.setRemoteVtepIp("10.10.1.1");

        return msg;
    }


}
