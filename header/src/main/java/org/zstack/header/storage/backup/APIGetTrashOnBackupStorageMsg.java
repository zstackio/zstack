package org.zstack.header.storage.backup;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.APISyncCallMessage;
import org.zstack.header.rest.RestRequest;

/**
 * Created by mingjian.deng on 2018/12/10.
 */
@RestRequest(
        path = "/backup-storage/trash",
        method = HttpMethod.GET,
        responseClass = APIGetTrashOnBackupStorageReply.class
)
public class APIGetTrashOnBackupStorageMsg extends APISyncCallMessage implements BackupStorageMessage {
    @APIParam(resourceType = BackupStorageVO.class)
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getBackupStorageUuid() {
        return uuid;
    }

    public static APIGetTrashOnBackupStorageMsg __example__() {
        APIGetTrashOnBackupStorageMsg msg = new APIGetTrashOnBackupStorageMsg();
        msg.setUuid(uuid());

        return msg;
    }
}
