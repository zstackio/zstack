package org.zstack.storage.fusionstor.backup;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.storage.backup.APIUpdateBackupStorageMsg;
import org.zstack.header.storage.backup.BackupStorageMessage;
import org.zstack.header.storage.backup.BackupStorageVO;

import java.util.List;
/**
 * Created by Mei Lei on 6/3/2016.
 */
@RestRequest(
        path = "/backup-storage/fusionstor/mons/{monUuid}/actions",
        method = HttpMethod.PUT,
        isAction = true,
        responseClass = APIUpdateMonToFusionstorBackupStorageEvent.class
)
public class APIUpdateFusionstorBackupStorageMonMsg extends APIMessage implements BackupStorageMessage {
    @APINoSee
    private String backupStorageUuid;

    @APIParam(resourceType = FusionstorBackupStorageMonVO.class, emptyString = false)
    private String monUuid;

    @APIParam(maxLength = 255, required = false)
    private String hostname;

    @APIParam(maxLength = 255, required = false)
    private String sshUsername;

    @APIParam(maxLength = 255, required = false)
    private String sshPassword;

    @APIParam(numberRange = {1, 65535}, required = false)
    private Integer sshPort;

    @APIParam(numberRange = {1, 65535}, required = false)
    private Integer monPort;

    public String getMonUuid() {
        return monUuid;
    }

    public void setMonUuid(String monUuid) {
        this.monUuid = monUuid;
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

    public Integer getMonPort() {
        return monPort;
    }

    public void setMonPort(Integer monPort) {
        this.monPort = monPort;
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
}

