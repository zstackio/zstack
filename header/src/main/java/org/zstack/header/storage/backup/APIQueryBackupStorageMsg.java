package org.zstack.header.storage.backup;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.query.APIQueryMessage;
import org.zstack.header.query.AutoQuery;
import org.zstack.header.query.QueryCondition;
import org.zstack.header.rest.RestRequest;

import java.util.Collections;

@Action(category = BackupStorageConstant.ACTION_CATEGORY, names = {"read"})
@AutoQuery(replyClass = APIQueryBackupStorageReply.class, inventoryClass = BackupStorageInventory.class)
@RestRequest(
        path = "/backup-storage",
        method = HttpMethod.GET,
        responseClass = APIQueryBackupStorageReply.class
)
public class APIQueryBackupStorageMsg extends APIQueryMessage {

 
    public static APIQueryBackupStorageMsg __example__() {
        APIQueryBackupStorageMsg msg = new APIQueryBackupStorageMsg();

        QueryCondition cond = new QueryCondition();
        cond.setName("uuid");
        cond.setOp("=");
        cond.setValue(uuid());

        msg.setConditions(Collections.singletonList(cond));

        return msg;
    }

}
