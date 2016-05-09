package org.zstack.storage.ceph;

import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.utils.ssh.Ssh;

/**
 * Created by frank on 7/27/2015.
 */
public abstract class CephMonBase {
    protected CephMonAO self;

    public static class PingResult {
        public boolean operationFailure;
        public boolean success;
        public String error;
    }

    public abstract void connect(Completion completion);

    public abstract void ping(ReturnValueCompletion<PingResult> completion);

    public CephMonBase(CephMonAO self) {
        this.self = self;
    }

    protected void checkTools() {
        Ssh ssh = new Ssh();
        ssh.setHostname(self.getHostname()).setUsername(self.getSshUsername()).setPassword(self.getSshPassword())
                .checkTool("ceph", "rbd").runErrorByExceptionAndClose();
    }

    public CephMonAO getSelf() {
        return self;
    }
}
