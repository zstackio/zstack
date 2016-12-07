package org.zstack.header.storage.backup;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;

@Action(category = BackupStorageConstant.ACTION_CATEGORY, names = {"read"})
@AutoQuery(replyClass = APIQueryBackupStorageReply.class, inventoryClass = BackupStorageInventory.class)
@RestRequest(
        path = "/backup-storage",
        method = HttpMethod.GET,
        responseClass = APIQueryBackupStorageReply.class
)
public class APIQueryBackupStorageMsg extends APIQueryMessage {

}
