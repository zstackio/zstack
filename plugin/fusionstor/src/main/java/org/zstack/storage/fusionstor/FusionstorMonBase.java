package org.zstack.storage.fusionstor;

import org.zstack.header.core.Completion;
import org.zstack.utils.ssh.Ssh;

/**
 * Created by frank on 7/27/2015.
 */
public abstract class FusionstorMonBase {
    protected FusionstorMonAO self;

    public abstract void connect(Completion completion);

    public FusionstorMonBase(FusionstorMonAO self) {
        this.self = self;
    }

    protected void checkTools() {
        Ssh ssh = new Ssh();
        ssh.setHostname(self.getHostname()).setUsername(self.getSshUsername()).setPassword(self.getSshPassword())
                .checkTool("/opt/fusionstack/lich/bin/lich", "/opt/fusionstack/lich/sbin/lichd").runErrorByExceptionAndClose();
    }

    public FusionstorMonAO getSelf() {
        return self;
    }
}
