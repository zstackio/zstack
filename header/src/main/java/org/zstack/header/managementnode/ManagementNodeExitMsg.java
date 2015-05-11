package org.zstack.header.managementnode;

import org.zstack.header.message.Message;

public class ManagementNodeExitMsg extends Message {
    public static enum Reason {
        Normal,
        HeartBeatStopped
    }

    private Reason reason = Reason.Normal;

    public Reason getReason() {
        return reason;
    }

    public void setReason(Reason reason) {
        this.reason = reason;
    }

    public ManagementNodeExitMsg() {
	}
}
