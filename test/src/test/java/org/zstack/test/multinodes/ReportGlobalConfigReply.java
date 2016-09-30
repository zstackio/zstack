package org.zstack.test.multinodes;

import org.zstack.header.message.MessageReply;

/**
 */
public class ReportGlobalConfigReply extends MessageReply {
    private String value;
    private String value2;

    public String getValue2() {
        return value2;
    }

    public void setValue2(String value2) {
        this.value2 = value2;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
