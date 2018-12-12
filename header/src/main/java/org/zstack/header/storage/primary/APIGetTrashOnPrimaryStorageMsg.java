package org.zstack.header.storage.primary;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.APIGetTrashOnBackupStorageMsg;

/**
 * Created by mingjian.deng on 2018/12/10.
 */
@RestRequest(
        path = "/primary-storage/trash",
        method = HttpMethod.GET,
        responseClass = APIGetTrashOnPrimaryStorageReply.class
)
public class APIGetTrashOnPrimaryStorageMsg extends APISyncCallMessage implements PrimaryStorageMessage {
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

    public static APIGetTrashOnPrimaryStorageMsg __example__() {
        APIGetTrashOnPrimaryStorageMsg msg = new APIGetTrashOnPrimaryStorageMsg();
        msg.setUuid(uuid());

        return msg;
    }
}
