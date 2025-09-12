package org.zstack.network.hostNetworkInterface.lldp.api;

import org.zstack.header.message.NeedReplyMessage;

public class GetHostNetworkInterfaceLldpMsg extends NeedReplyMessage {
    private String interfaceUuid;

    public String getInterfaceUuid() {
        return interfaceUuid;
    }

    public void setInterfaceUuid(String interfaceUuid) {
        this.interfaceUuid = interfaceUuid;
    }
}
