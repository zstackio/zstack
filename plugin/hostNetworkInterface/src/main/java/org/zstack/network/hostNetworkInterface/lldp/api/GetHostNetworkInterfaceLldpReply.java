package org.zstack.network.hostNetworkInterface.lldp.api;

import org.zstack.header.message.MessageReply;
import org.zstack.network.hostNetworkInterface.lldp.entity.HostNetworkInterfaceLldpRefInventory;

import java.io.Serializable;

public class GetHostNetworkInterfaceLldpReply extends MessageReply implements Serializable {
    private HostNetworkInterfaceLldpRefInventory lldp;

    public HostNetworkInterfaceLldpRefInventory getLldp() {
        return lldp;
    }

    public void setLldp(HostNetworkInterfaceLldpRefInventory lldp) {
        this.lldp = lldp;
    }
}
