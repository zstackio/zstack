package org.zstack.storage.backup.sftp;

import org.zstack.header.message.APIParam;
import org.zstack.header.storage.backup.APIUpdateBackupStorageMsg;

/**
 * Created by frank on 6/15/2015.
 */
public class APIUpdateSftpBackupStorageMsg extends APIUpdateBackupStorageMsg {
    @APIParam(maxLength = 255, required = false)
    private String username;
    @APIParam(maxLength = 255, required = false)
    private String password;
    @APIParam(maxLength = 255, required = false)
    private String hostname;
    @APIParam(numberRange = {1, 65535}, required = false)
    private Integer sshport;

    public Integer getSshport() {
        return sshport;
    }

    public void setSshport(Integer sshport) {
        this.sshport = sshport;
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
}
