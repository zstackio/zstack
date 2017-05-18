package org.zstack.header.network.l2;

import org.springframework.http.HttpMethod;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;

/**
 * @api detach l2Network from a cluster
 * @category l2Network
 * @cli
 * @httpMsg {
 * "org.zstack.header.network.l2.APIDetachL2NetworkFromClusterMsg": {
 * "l2NetworkUuid": "409bbe05c1714d0a88ed9f4fff5bfe7e",
 * "clusterUuid": "5161770207e1481084d3cc28998b7a0b",
 * "session": {
 * "uuid": "ab776cd039d746569a8a8c47eee41460"
 * }
 * }
 * }
 * @msg {
 * "org.zstack.header.network.l2.APIDetachL2NetworkFromClusterMsg": {
 * "l2NetworkUuid": "409bbe05c1714d0a88ed9f4fff5bfe7e",
 * "clusterUuid": "5161770207e1481084d3cc28998b7a0b",
 * "session": {
 * "uuid": "ab776cd039d746569a8a8c47eee41460"
 * },
 * "timeout": 1800000,
 * "id": "09c37622336341f294cb8d692707ff44",
 * "serviceId": "api.portal"
 * }
 * }
 * @result see :ref:`APIDetachL2NetworkFromClusterEvent`
 * @since 0.1.0
 */
@RestRequest(
        path = "/l2-networks/{l2NetworkUuid}/clusters/{clusterUuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDetachL2NetworkFromClusterEvent.class
)
public class APIDetachL2NetworkFromClusterMsg extends APIMessage implements L2NetworkMessage {
    /**
     * @desc l2Network uuid
     */
    @APIParam(resourceType = L2NetworkVO.class)
    private String l2NetworkUuid;
    /**
     * @desc cluster uuid. See :ref:`ClusterInventory`
     */
    @APIParam(resourceType = ClusterVO.class)
    private String clusterUuid;

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }

    public void setL2NetworkUuid(String l2NetworkUuid) {
        this.l2NetworkUuid = l2NetworkUuid;
    }

    @Override
    public String getL2NetworkUuid() {
        return l2NetworkUuid;
    }

 
    public static APIDetachL2NetworkFromClusterMsg __example__() {
        APIDetachL2NetworkFromClusterMsg msg = new APIDetachL2NetworkFromClusterMsg();
        msg.setL2NetworkUuid(uuid());
        msg.setClusterUuid(uuid());

        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                ntfy("Detached from a cluster[uuid:%s]", clusterUuid)
                        .resource(l2NetworkUuid, L2NetworkVO.class.getSimpleName())
                        .context("clusterUuid", clusterUuid)
                        .messageAndEvent(that, evt).done();

                ntfy("Detached from a L2 network[uuid:%s]", l2NetworkUuid)
                        .resource(clusterUuid, ClusterVO.class.getSimpleName())
                        .context("l2NetworkUuid", l2NetworkUuid)
                        .messageAndEvent(that, evt).done();
            }
        };
    }

}
