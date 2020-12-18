package org.zstack.header.network.l2;

import org.springframework.http.HttpMethod;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.message.APIGetMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.APIGetCandidateIsoForAttachingVmMsg;

@RestRequest(
        path = "/cluster/{clusterUuid}/l2-candidates",
        method = HttpMethod.GET,
        responseClass = APIGetCandidateL2ForAttachingClusterReply.class
)
public class APIGetCandidateL2ForAttachingClusterMsg extends APIGetMessage {
    @APIParam(resourceType = ClusterVO.class)
    private String clusterUuid;

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }

    public static APIGetCandidateL2ForAttachingClusterMsg __example__() {
        APIGetCandidateL2ForAttachingClusterMsg msg = new APIGetCandidateL2ForAttachingClusterMsg();
        msg.clusterUuid = uuid();
        return msg;
    }
}
