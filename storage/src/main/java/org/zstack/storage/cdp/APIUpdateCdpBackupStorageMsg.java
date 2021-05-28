package org.zstack.storage.cdp;

import org.springframework.http.HttpMethod;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.storage.backup.APIUpdateBackupStorageEvent;
import org.zstack.header.storage.backup.APIUpdateBackupStorageMsg;

import java.io.Serializable;

@RestRequest(
        path = "/cdp-backup-storage/{uuid}/actions",
        responseClass = APIUpdateBackupStorageEvent.class,
        method = HttpMethod.PUT,
        isAction = true
)
public class APIUpdateCdpBackupStorageMsg extends APIUpdateBackupStorageMsg implements Serializable {
    @APIParam(maxLength = 255, required = false)
    private String username;
    @APIParam(maxLength = 255, required = false,password = true)
    @NoLogging
    private String password;
    @APIParam(maxLength = 255, required = false)
    private String hostname;
    @APIParam(numberRange = {1, 65535}, required = false)
    private Integer sshPort;

    public Integer getSshPort() {
        return sshPort;
    }

    public void setSshPort(Integer sshPort) {
        this.sshPort = sshPort;
    }
    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public static APIUpdateCdpBackupStorageMsg __example__() {
        APIUpdateCdpBackupStorageMsg msg = new APIUpdateCdpBackupStorageMsg();

        msg.setUuid(uuid());
        msg.setHostname("192.168.1.18");

        return msg;
    }
}
