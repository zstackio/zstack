package org.zstack.network.hostNetworkInterface;

import org.zstack.header.message.NeedJsonSchema;

public class HostNetworkInterfaceCanonicalEvents {
    public static final String PEER_PORT_CHANGED = "/network/host-network-interface/peer-changed";

    @NeedJsonSchema
    public static class PeerPortChangedData {
        private String interfaceUuid;
        private PhysicalSwitchPortInventory oldPhysicalSwitchPort;
        private PhysicalSwitchPortInventory newPhysiaclSwitchPort;

        public String getInterfaceUuid() {
            return interfaceUuid;
        }

        public void setInterfaceUuid(String interfaceUuid) {
            this.interfaceUuid = interfaceUuid;
        }

        public PhysicalSwitchPortInventory getOldPhysicalSwitchPort() {
            return oldPhysicalSwitchPort;
        }

        public void setOldPhysicalSwitchPort(PhysicalSwitchPortInventory oldPhysicalSwitchPort) {
            this.oldPhysicalSwitchPort = oldPhysicalSwitchPort;
        }

        public PhysicalSwitchPortInventory getNewPhysiaclSwitchPort() {
            return newPhysiaclSwitchPort;
        }

        public void setNewPhysiaclSwitchPort(PhysicalSwitchPortInventory newPhysiaclSwitchPort) {
            this.newPhysiaclSwitchPort = newPhysiaclSwitchPort;
        }
    }
}
