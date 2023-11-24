package org.zstack.network.hostNetwork.lldp;

import org.zstack.core.validation.ConditionalValidation;
import org.zstack.network.hostNetwork.HostNetworkInterfaceInventory;
import org.zstack.network.hostNetwork.lldp.entity.HostNetworkInterfaceLldpInventory;
import org.zstack.network.hostNetwork.lldp.entity.HostNetworkInterfaceLldpRefInventory;

import java.util.List;

public class LldpKvmAgentCommands {

    public static class AgentCommand {
    }

    public static class AgentResponse implements ConditionalValidation {
        private boolean success = true;
        private String error;

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }

        @Override
        public boolean needValidation() {
            return success;
        }
    }

    public static class ChangeLldpModeCmd extends AgentCommand {
        private List<String> physicalInterfaceNames;

        private String mode;

        public List<String> getPhysicalInterfaceNames() {
            return physicalInterfaceNames;
        }

        public void setPhysicalInterfaceNames(List<String> physicalInterfaceNames) {
            this.physicalInterfaceNames = physicalInterfaceNames;
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }
    }

    public static class ChangeLldpModeResponse extends AgentResponse {
    }

    public static class GetLldpInfoCmd extends AgentCommand {
        private String physicalInterfaceName;

        public String getPhysicalInterfaceName() {
            return physicalInterfaceName;
        }

        public void setPhysicalInterfaceName(String physicalInterfaceName) {
            this.physicalInterfaceName = physicalInterfaceName;
        }
    }

    public static class GetLldpInfoResponse extends AgentResponse {
        private LldpInfoStruct lldpInfo;

        public LldpInfoStruct getLldpInfo() {
            return lldpInfo;
        }

        public void setLldpInfo(LldpInfoStruct lldpInfo) {
            this.lldpInfo = lldpInfo;
        }
    }

    public static class ApplyLldpConfigCmd extends AgentCommand {
        public List<LldpConfigSyncStruct.LLdpModeConfig> getLldpConfig() {
            return lldpConfig;
        }

        public void setLldpConfig(List<LldpConfigSyncStruct.LLdpModeConfig> lldpConfig) {
            this.lldpConfig = lldpConfig;
        }

        private List<LldpConfigSyncStruct.LLdpModeConfig> lldpConfig;

    }

    public static class ApplyLldpConfigResponse extends AgentResponse {
    }
}
