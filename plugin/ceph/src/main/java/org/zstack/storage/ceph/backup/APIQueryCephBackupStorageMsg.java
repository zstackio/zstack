package org.zstack.storage.ceph.backup;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.query.QueryCondition;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.APIQueryBackupStorageReply;
import org.zstack.header.storage.backup.BackupStorageConstant;

import java.util.Collections;

/**
 * Created by frank on 8/6/2015.
 */
@Action(category = BackupStorageConstant.ACTION_CATEGORY, names = {"read"})
@AutoQuery(replyClass = APIQueryBackupStorageReply.class, inventoryClass = CephBackupStorageInventory.class)
@RestRequest(
        path = "/backup-storage/ceph",
        optionalPaths = {"/backup-storage/ceph/{uuid}"},
        method = HttpMethod.GET,
        responseClass = APIQueryBackupStorageReply.class
)
public class APIQueryCephBackupStorageMsg extends APIQueryMessage {
 
    public static APIQueryCephBackupStorageMsg __example__() {
        APIQueryCephBackupStorageMsg msg = new APIQueryCephBackupStorageMsg();

        QueryCondition cond = new QueryCondition();
        cond.setName("uuid");
        cond.setOp("=");
        cond.setValue(uuid());

        msg.setConditions(Collections.singletonList(cond));

        return msg;
    }

}
