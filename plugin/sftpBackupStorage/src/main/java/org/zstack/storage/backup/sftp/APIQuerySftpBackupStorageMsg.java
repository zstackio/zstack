package org.zstack.storage.backup.sftp;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.BackupStorageConstant;

@Action(category = BackupStorageConstant.ACTION_CATEGORY, names={"read"})
@AutoQuery(replyClass = APIQuerySftpBackupStorageReply.class, inventoryClass = SftpBackupStorageInventory.class)
@RestRequest(
        path = "/backup-storage/sftp",
        optionalPaths = {"/backup-storage/sftp/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQuerySftpBackupStorageReply.class
)
public class APIQuerySftpBackupStorageMsg extends APIQueryMessage {

}
