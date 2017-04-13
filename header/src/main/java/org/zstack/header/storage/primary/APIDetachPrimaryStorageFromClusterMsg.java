package org.zstack.header.storage.primary;

import org.springframework.http.HttpMethod;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;

/**
 * @api detach primary storage from a cluster
 * @cli
 * @httpMsg {
 * "org.zstack.header.storage.primary.APIDetachPrimaryStorageMsg": {
 * "primaryStorageUuid": "f4ac0a3119c94c6fae844c2298615d27",
 * "clusterUuid": "8f02c8853ef4411fa897cfcded972790",
 * "session": {
 * "uuid": "b7fabee084b5403590f77f670d7235ff"
 * }
 * }
 * }
 * @msg {
 * "org.zstack.header.storage.primary.APIDetachPrimaryStorageMsg": {
 * "primaryStorageUuid": "f4ac0a3119c94c6fae844c2298615d27",
 * "clusterUuid": "8f02c8853ef4411fa897cfcded972790",
 * "session": {
 * "uuid": "b7fabee084b5403590f77f670d7235ff"
 * },
 * "timeout": 1800000,
 * "id": "cf065f62e84f4c35b143f70dca83eccd",
 * "serviceId": "api.portal"
 * }
 * }
 * @result see :ref:`APIDetachPrimaryStorageEvent`
 * @since 0.1.0
 */
@RestRequest(
        path = "/clusters/{clusterUuid}/primary-storage/{primaryStorageUuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDetachPrimaryStorageFromClusterEvent.class,
        parameterName = "null"
)
public class APIDetachPrimaryStorageFromClusterMsg extends APIMessage implements PrimaryStorageMessage {
    /**
     * @desc primary storage uuid
     */
    @APIParam(resourceType = PrimaryStorageVO.class)
    private String primaryStorageUuid;
    /**
     * @desc cluster uuid. See :ref:`ClusterInventory`
     */
    @APIParam(resourceType = ClusterVO.class)
    private String clusterUuid;

    public APIDetachPrimaryStorageFromClusterMsg() {
    }

    public APIDetachPrimaryStorageFromClusterMsg(String uuid) {
        super();
        this.primaryStorageUuid = uuid;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String uuid) {
        this.primaryStorageUuid = uuid;
    }

    public String getClusterUuid() {
        return clusterUuid;
    }

    public void setClusterUuid(String clusterUuid) {
        this.clusterUuid = clusterUuid;
    }
 
    public static APIDetachPrimaryStorageFromClusterMsg __example__() {
        APIDetachPrimaryStorageFromClusterMsg msg = new APIDetachPrimaryStorageFromClusterMsg();

        msg.setClusterUuid(uuid());
        msg.setPrimaryStorageUuid(uuid());

        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                if (evt.isSuccess()) {
                    ntfy("Detached from cluster[uuid:%s]", clusterUuid).resource(primaryStorageUuid, PrimaryStorageVO.class.getSimpleName())
                            .context("clusterUuid", clusterUuid)
                            .messageAndEvent(that, evt).done();

                    ntfy("Detached from primary storage[uuid:%s]", primaryStorageUuid).resource(clusterUuid, ClusterVO.class.getSimpleName())
                            .context("primaryStorageUuid", primaryStorageUuid)
                            .messageAndEvent(that,evt).done();
                }
            }
        };
    }

}
