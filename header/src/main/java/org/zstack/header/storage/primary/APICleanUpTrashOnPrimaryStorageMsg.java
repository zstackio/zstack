package org.zstack.header.storage.primary;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by mingjian.deng on 2018/12/10.
 */
@RestRequest(
        path = "/primary-storage/{uuid}/trash/actions",
        isAction = true,
        responseClass = APICleanUpTrashOnPrimaryStorageEvent.class,
        method = HttpMethod.PUT
)
public class APICleanUpTrashOnPrimaryStorageMsg extends APIMessage implements PrimaryStorageMessage {
    @APIParam(resourceType = PrimaryStorageVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getPrimaryStorageUuid() {
        return uuid;
    }

    public static APICleanUpTrashOnPrimaryStorageMsg __example__() {
        APICleanUpTrashOnPrimaryStorageMsg msg = new APICleanUpTrashOnPrimaryStorageMsg();
        msg.setUuid(uuid());

        return msg;
    }
}
