package org.zstack.storage.surfs.backup;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.BackupStorageMessage;

import java.util.List;

import static org.codehaus.groovy.runtime.InvokerHelper.asList;

/**
 * Created by zhouhaiping 2017-08-23
 */
@RestRequest(
        path = "/backup-storage/surfs/{uuid}/nodes",
        method = HttpMethod.POST,
        parameterName = "params",
        responseClass = APIAddNodeToSurfsBackupStorageEvent.class
)
public class APIAddNodeToSurfsBackupStorageMsg extends APIMessage implements BackupStorageMessage {
    @APIParam(resourceType = SurfsBackupStorageVO.class)
    private String uuid;
    @APIParam(nonempty = true)
    private List<String> nodeUrls;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public List<String> getNodeUrls() {
        return nodeUrls;
    }

    public void setNodeUrls(List<String> nodeUrls) {
        this.nodeUrls = nodeUrls;
    }

    @Override
    public String getBackupStorageUuid() {
        return uuid;
    }
 
    public static APIAddNodeToSurfsBackupStorageMsg __example__() {
        APIAddNodeToSurfsBackupStorageMsg msg = new APIAddNodeToSurfsBackupStorageMsg();
        msg.setUuid(uuid());
        msg.setNodeUrls(asList("root:password@localhost:23"));
        return msg;
    }

}
