package org.zstack.network.hostNetwork.lldp;

public class LldpConfigSyncStruct {

    public static class LLdpModeConfig{
        private String physicalInterfaceName;

        private String mode;

        public String getPhysicalInterfaceName() {
            return physicalInterfaceName;
        }

        public void setPhysicalInterfaceName(String physicalInterfaceName) {
            this.physicalInterfaceName = physicalInterfaceName;
        }

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }
    }

}
