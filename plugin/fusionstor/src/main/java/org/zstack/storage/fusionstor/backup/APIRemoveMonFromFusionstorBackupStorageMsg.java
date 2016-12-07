package org.zstack.storage.fusionstor.backup;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.BackupStorageMessage;

import java.util.List;

/**
 * Created by frank on 8/1/2015.
 */
@RestRequest(
        path = "/backup-storage/fusionstor/{uuid}/mons",
        method = HttpMethod.DELETE,
        responseClass = APIRemoveMonFromFusionstorBackupStorageEvent.class,
        parameterName = "params"
)
public class APIRemoveMonFromFusionstorBackupStorageMsg extends APIMessage implements BackupStorageMessage {
    @APIParam(resourceType = FusionstorBackupStorageVO.class)
    private String uuid;
    @APIParam(nonempty = true)
    private List<String> monHostnames;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public List<String> getMonHostnames() {
        return monHostnames;
    }

    public void setMonHostnames(List<String> monHostnames) {
        this.monHostnames = monHostnames;
    }

    @Override
    public String getBackupStorageUuid() {
        return uuid;
    }
}
