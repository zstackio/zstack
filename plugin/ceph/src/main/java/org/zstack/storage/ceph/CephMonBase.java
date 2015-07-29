package org.zstack.storage.ceph;

import org.zstack.header.core.Completion;
import org.zstack.utils.ssh.Ssh;

/**
 * Created by frank on 7/27/2015.
 */
public abstract class CephMonBase {
    protected CephMonAO self;

    private String hostname;
    private String sshUsername;
    private String sshPassword;

    public abstract void connect(Completion completion);

    public CephMonBase(CephMonAO self) {
        this.self = self;
    }

    protected void checkTools() {
        Ssh ssh = new Ssh();
        ssh.setHostname(hostname).setUsername(sshUsername).setPassword(sshPassword)
                .checkTool("ceph", "rbd").runErrorByExceptionAndClose();
    }

    public CephMonAO getSelf() {
        return self;
    }

    public String getHostname() {
        return hostname;
    }

    public String getSshUsername() {
        return sshUsername;
    }

    public String getSshPassword() {
        return sshPassword;
    }
}
