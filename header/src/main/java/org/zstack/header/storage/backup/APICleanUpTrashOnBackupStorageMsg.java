package org.zstack.header.storage.backup;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

/**
 * Created by mingjian.deng on 2018/12/10.
 */
@RestRequest(
        path = "/backup-storage/{uuid}/trash/actions",
        isAction = true,
        responseClass = APICleanUpTrashOnBackupStorageEvent.class,
        method = HttpMethod.PUT
)
public class APICleanUpTrashOnBackupStorageMsg extends APIMessage implements BackupStorageMessage {
    @APIParam(resourceType = BackupStorageVO.class, checkAccount = true)
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

    public static APICleanUpTrashOnBackupStorageMsg __example__() {
        APICleanUpTrashOnBackupStorageMsg msg = new APICleanUpTrashOnBackupStorageMsg();
        msg.setUuid(uuid());

        return msg;
    }
}
