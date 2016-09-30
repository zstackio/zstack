package org.zstack.storage.ceph;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.rest.JsonAsyncRESTCallback;
import org.zstack.header.rest.RESTFacade;
import org.zstack.utils.ssh.Ssh;

/**
 * Created by frank on 7/27/2015.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE, dependencyCheck = true)
public abstract class CephMonBase {
    protected CephMonAO self;

    @Autowired
    protected RESTFacade restf;

    public static class PingResult {
        public String failure;
        public boolean success;
        public String error;
    }

    public abstract void connect(Completion completion);

    public abstract void ping(ReturnValueCompletion<PingResult> completion);

    protected abstract int getAgentPort();

    public CephMonBase(CephMonAO self) {
        this.self = self;
    }

    protected void checkTools() {
        Ssh ssh = new Ssh();
        ssh.setHostname(self.getHostname()).setUsername(self.getSshUsername()).setPassword(self.getSshPassword()).setPort(self.getSshPort())
                .checkTool("ceph", "rbd").runErrorByExceptionAndClose();
    }

    protected String makeHttpPath(String ip, String path) {
        return String.format("http://%s:%s%s", ip, getAgentPort(), path);
    }

    public <T> void httpCall(final String path, final Object cmd, final Class<T> retClass, final ReturnValueCompletion<T> completion) {
        restf.asyncJsonPost(makeHttpPath(self.getHostname(), path), cmd, new JsonAsyncRESTCallback<T>(completion) {
            @Override
            public void fail(ErrorCode err) {
                completion.fail(err);
            }

            @Override
            public void success(T ret) {
                completion.success(ret);
            }

            @Override
            public Class<T> getReturnClass() {
                return retClass;
            }
        });
    }

    public CephMonAO getSelf() {
        return self;
    }
}
