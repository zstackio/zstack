package org.zstack.storage.surfs.backup;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.APIQueryBackupStorageReply;
import org.zstack.header.storage.backup.BackupStorageConstant;

import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by zhouhaiping 2017-09-14
 */
@Action(category = BackupStorageConstant.ACTION_CATEGORY, names = {"read"})
@AutoQuery(replyClass = APIQueryBackupStorageReply.class, inventoryClass = SurfsBackupStorageInventory.class)
@RestRequest(
        path = "/backup-storage/surfs",
        optionalPaths = {"/backup-storage/surfs/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryBackupStorageReply.class
)
public class APIQuerySurfsBackupStorageMsg extends APIQueryMessage {

    public static List<String> __example__() {
        return asList();
    }

}
