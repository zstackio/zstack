package org.zstack.storage.ceph.primary;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.primary.PrimaryStorageMessage;
import org.zstack.header.storage.primary.PrimaryStorageVO;

/**
 * Created by xing5 on 2017/2/28.
 */
@RestRequest(
        path = "/primary-storage/ceph/pools/{uuid}",
        method = HttpMethod.DELETE,
        responseClass = APIDeleteCephPrimaryStoragePoolEvent.class
)
public class APIDeleteCephPrimaryStoragePoolMsg extends APIMessage implements PrimaryStorageMessage {
    @APIParam(resourceType = CephPrimaryStoragePoolVO.class, successIfResourceNotExisting = true)
    private String uuid;

    @APINoSee
    private String primaryStorageUuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public static APIDeleteCephPrimaryStoragePoolMsg __example__() {
        APIDeleteCephPrimaryStoragePoolMsg msg = new APIDeleteCephPrimaryStoragePoolMsg();
        msg.setUuid(uuid());
        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;
        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                ntfy("Deleted a pool").resource(uuid, PrimaryStorageVO.class.getSimpleName())
                        .messageAndEvent(that, evt).done();
            }
        };
    }
}
