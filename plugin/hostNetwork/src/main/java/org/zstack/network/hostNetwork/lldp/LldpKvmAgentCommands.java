package org.zstack.network.hostNetwork.lldp;

import org.zstack.core.validation.ConditionalValidation;
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

    public static class ChangeLldpModeCmd extends org.zstack.header.agent.AgentCommand {
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

    public static class ChangeLldpModeResponse extends org.zstack.header.agent.AgentResponse {
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

    public static class GetLldpInfoResponse extends org.zstack.header.agent.AgentResponse {
        private HostNetworkInterfaceLldpRefInventory lldpInventory;

        public HostNetworkInterfaceLldpRefInventory getLldpInventory() {
            return lldpInventory;
        }

        public void setLldpInventory(HostNetworkInterfaceLldpRefInventory lldpInventory) {
            this.lldpInventory = lldpInventory;
        }
    }
}
