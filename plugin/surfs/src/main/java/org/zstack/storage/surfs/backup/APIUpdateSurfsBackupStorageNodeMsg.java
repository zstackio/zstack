package org.zstack.storage.surfs.backup;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;

import org.zstack.header.storage.backup.BackupStorageMessage;


/**
 * Created by zhouhaiping 2017-09-11
 */
@RestRequest(
        path = "/backup-storage/surfs/nodes/{nodeUuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIUpdateNodeToSurfsBackupStorageEvent.class
)
public class APIUpdateSurfsBackupStorageNodeMsg extends APIMessage implements BackupStorageMessage {
    @APINoSee
    private String backupStorageUuid;

    @APIParam(resourceType = SurfsBackupStorageNodeVO.class, emptyString = false)
    private String nodeUuid;

    @APIParam(maxLength = 255, required = false)
    private String hostname;

    @APIParam(maxLength = 255, required = false)
    private String sshUsername;

    @APIParam(maxLength = 255, required = false)
    private String sshPassword;

    @APIParam(numberRange = {1, 65535}, required = false)
    private Integer sshPort;

    @APIParam(numberRange = {1, 65535}, required = false)
    private Integer nodePort;

    public String getNodeUuid() {
        return nodeUuid;
    }

    public void setNodeUuid(String nodeUuid) {
        this.nodeUuid = nodeUuid;
    }

    public String getSshUsername() {
        return sshUsername;
    }

    public void setSshUsername(String sshUsername) {
        this.sshUsername = sshUsername;
    }

    public String getSshPassword() {
        return sshPassword;
    }

    public void setSshPassword(String sshPassword) {
        this.sshPassword = sshPassword;
    }

    public Integer getSshPort() {
        return sshPort;
    }

    public void setSshPort(Integer sshPort) {
        this.sshPort = sshPort;
    }

    public Integer getNodePort() {
        return nodePort;
    }

    public void setNodePort(Integer nodePort) {
        this.nodePort = nodePort;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    @Override
    public String getBackupStorageUuid() {
        return backupStorageUuid;
    }

    public void setBackupStorageUuid(String backupStorageUuid) {
        this.backupStorageUuid = backupStorageUuid;
    }

    public static APIUpdateSurfsBackupStorageNodeMsg __example__() {
        APIUpdateSurfsBackupStorageNodeMsg msg = new APIUpdateSurfsBackupStorageNodeMsg();
        msg.setNodeUuid(uuid());
        msg.setNodePort(7798);

        return msg;
    }
}


