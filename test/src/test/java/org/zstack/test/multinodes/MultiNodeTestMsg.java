package org.zstack.test.multinodes;

import org.zstack.header.message.NeedReplyMessage;

/**
 */
public class MultiNodeTestMsg extends NeedReplyMessage {
    public static final String EXIT = "exit";
    public static final String READY = "ready";

    private String opCode;

    public String getOpCode() {
        return opCode;
    }

    public void setOpCode(String opCode) {
        this.opCode = opCode;
    }

    public boolean isCode(String code) {
        return opCode.equals(code);
    }
}
