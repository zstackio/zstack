package org.zstack.appliancevm;

import org.zstack.core.upgrade.GrayVersion;
import org.zstack.kvm.KVMAgentCommands;

import java.util.Map;

/**
 */
public interface ApplianceVmKvmCommands {
    public static class PrepareBootstrapInfoCmd extends KVMAgentCommands.AgentCommand {
        public static final String PATH = "/appliancevm/setbootstrapinfo";
        @GrayVersion(value = "5.0.0")
        private Map<String, Object> info;
        @GrayVersion(value = "5.0.0")
        private String socketPath;
        @GrayVersion(value = "5.0.0")
        private Integer bootStrapInfoTimeout;

        public Map<String, Object> getInfo() {
            return info;
        }

        public void setInfo(Map<String, Object> info) {
            this.info = info;
        }

        public String getSocketPath() {
            return socketPath;
        }

        public void setSocketPath(String socketPath) {
            this.socketPath = socketPath;
        }

        public Integer getBootStrapInfoTimeout() {
            return bootStrapInfoTimeout;
        }

        public void setBootStrapInfoTimeout(Integer bootStrapInfoTimeout) {
            this.bootStrapInfoTimeout = bootStrapInfoTimeout;
        }
    }

    public static class PrepareBootstrapInfoRsp extends KVMAgentCommands.AgentResponse {
    }
}
