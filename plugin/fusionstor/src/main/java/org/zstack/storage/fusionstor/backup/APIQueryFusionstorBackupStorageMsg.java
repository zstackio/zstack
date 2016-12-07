package org.zstack.storage.fusionstor.backup;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.APIQueryBackupStorageReply;
import org.zstack.header.storage.backup.BackupStorageConstant;

/**
 * Created by frank on 8/6/2015.
 */
@Action(category = BackupStorageConstant.ACTION_CATEGORY, names = {"read"})
@AutoQuery(replyClass = APIQueryBackupStorageReply.class, inventoryClass = FusionstorBackupStorageInventory.class)
@RestRequest(
        path = "/backup-storage/fusionstor",
        optionalPaths = {"/backup-storage/fusionstor/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryBackupStorageReply.class
)
public class APIQueryFusionstorBackupStorageMsg extends APIQueryMessage {
}
