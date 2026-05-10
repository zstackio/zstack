package org.zstack.header.server;

import org.zstack.header.message.MessageReply;

public class PingPhysicalServerReply extends MessageReply {
    private PhysicalServerPowerStatus powerStatus;

    public PhysicalServerPowerStatus getPowerStatus() {
        return powerStatus;
    }

    public void setPowerStatus(PhysicalServerPowerStatus powerStatus) {
        this.powerStatus = powerStatus;
    }
}
