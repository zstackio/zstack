package org.zstack.storage.ceph.primary;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.notification.ApiNotification;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.primary.PrimaryStorageMessage;
import org.zstack.header.storage.primary.PrimaryStorageVO;

/**
 * Created by xing5 on 2017/2/28.
 */
@RestRequest(
        path = "/primary-storage/ceph/{primaryStorageUuid}/pools",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APIAddCephPrimaryStoragePoolEvent.class
)
public class APIAddCephPrimaryStoragePoolMsg extends APICreateMessage implements PrimaryStorageMessage {
    @APIParam(resourceType = PrimaryStorageVO.class)
    private String primaryStorageUuid;
    @APIParam(maxLength = 255)
    private String poolName;
    @APIParam(maxLength = 2048, required = false)
    private String description;
    private boolean errorIfNotExist;

    public boolean isErrorIfNotExist() {
        return errorIfNotExist;
    }

    public void setErrorIfNotExist(boolean errorIfNotExist) {
        this.errorIfNotExist = errorIfNotExist;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }

    public String getPoolName() {
        return poolName;
    }

    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }

    public static APIAddCephPrimaryStoragePoolMsg __example__() {
        APIAddCephPrimaryStoragePoolMsg msg = new APIAddCephPrimaryStoragePoolMsg();
        msg.setPoolName("highPerformance");
        msg.setDescription("for high performance data volumes");
        msg.setPrimaryStorageUuid(uuid());
        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;
        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                ntfy("Added a Ceph pool").resource(primaryStorageUuid, PrimaryStorageVO.class.getSimpleName())
                        .messageAndEvent(that, evt).done();
            }
        };
    }
}
