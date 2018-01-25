package org.zstack.storage.surfs.backup;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.BackupStorageMessage;

import java.util.List;

import static org.codehaus.groovy.runtime.InvokerHelper.asList;

/**
 * Created by zhouhaiping 2017-09-12
 */
@RestRequest(
        path = "/backup-storage/surfs/{uuid}/nodes",
        method = HttpMethod.DELETE,
        responseClass = APIRemoveNodeFromSurfsBackupStorageEvent.class
)
public class APIRemoveNodeFromSurfsBackupStorageMsg extends APIMessage implements BackupStorageMessage {
    @APIParam(resourceType = SurfsBackupStorageVO.class)
    private String uuid;
    @APIParam(nonempty = true)
    private List<String> nodeHostnames;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public List<String> getNodeHostnames() {
        return nodeHostnames;
    }

    public void setNodeHostnames(List<String> nodeHostnames) {
        this.nodeHostnames = nodeHostnames;
    }

    @Override
    public String getBackupStorageUuid() {
        return uuid;
    }
 
    public static APIRemoveNodeFromSurfsBackupStorageMsg __example__() {
        APIRemoveNodeFromSurfsBackupStorageMsg msg = new APIRemoveNodeFromSurfsBackupStorageMsg();
        msg.setUuid(uuid());
        msg.setNodeHostnames(asList("192.20.12.12"));
        return msg;
    }

}
