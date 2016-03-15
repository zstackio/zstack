package org.zstack.header.console;

import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;

import java.util.List;

/**
 * Created by xing5 on 2016/3/15.
 */
public class APIReconnectConsoleProxyAgentMsg extends APIMessage implements ConsoleProxyAgentMessage {
    @APIParam(required = false, nonempty = true)
    private List<String> agentUuids;

    public List<String> getAgentUuids() {
        return agentUuids;
    }

    public void setAgentUuids(List<String> agentUuids) {
        this.agentUuids = agentUuids;
    }
}
