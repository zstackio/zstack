package org.zstack.appliancevm;

import org.zstack.kvm.KVMAgentCommands;

import java.util.Map;

/**
 */
public interface ApplianceVmKvmCommands {
    public static class PrepareBootstrapInfoCmd extends KVMAgentCommands.AgentCommand {
        public static final String PATH = "/appliancevm/setbootstrapinfo";

        private Map<String, Object> info;
        private String socketPath;

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
    }

    public static class PrepareBootstrapInfoRsp extends KVMAgentCommands.AgentResponse {
    }
}
