package org.zstack.header.console;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by xing5 on 2016/3/15.
 */
public class ReconnectConsoleProxyMsg extends NeedReplyMessage implements ConsoleProxyAgentMessage {
    private String agentUuid;

    public String getAgentUuid() {
        return agentUuid;
    }

    public void setAgentUuid(String agentUuid) {
        this.agentUuid = agentUuid;
    }
}
