package org.zstack.header.console;

import org.springframework.http.HttpMethod;
import org.zstack.header.identity.Action;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;

@Action(adminOnly = true, category = ConsoleConstants.ACTION_CATEGORY)
@RestRequest(
        path = "/consoles/agents/{uuid}/actions",
        isAction = true,
        responseClass = APIUpdateConsoleProxyAgentEvent.class,
        method = HttpMethod.PUT
)
public class APIUpdateConsoleProxyAgentMsg extends APIMessage implements ConsoleProxyAgentMessage {
    @APIParam(resourceType = ConsoleProxyAgentVO.class)
    private String uuid;
    @APIParam
    private String consoleProxyOverriddenIp;
    @APIParam(required = false)
    private String consoleProxyOverriddenIpv4;
    @APIParam(required = false)
    private String consoleProxyOverriddenIpv6;
    @APIParam(required = false, numberRange={1, 65535})
    private Integer consoleProxyPort;

    public Integer getConsoleProxyPort() {
        return consoleProxyPort;
    }

    public void setConsoleProxyPort(Integer consoleProxyPort) {
        this.consoleProxyPort = consoleProxyPort;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getConsoleProxyOverriddenIp() {
        return consoleProxyOverriddenIp;
    }

    public void setConsoleProxyOverriddenIp(String consoleProxyOverriddenIp) {
        this.consoleProxyOverriddenIp = consoleProxyOverriddenIp;
    }

    public String getConsoleProxyOverriddenIpv4() {
        return consoleProxyOverriddenIpv4;
    }

    public void setConsoleProxyOverriddenIpv4(String consoleProxyOverriddenIpv4) {
        this.consoleProxyOverriddenIpv4 = consoleProxyOverriddenIpv4;
    }

    public String getConsoleProxyOverriddenIpv6() {
        return consoleProxyOverriddenIpv6;
    }

    public void setConsoleProxyOverriddenIpv6(String consoleProxyOverriddenIpv6) {
        this.consoleProxyOverriddenIpv6 = consoleProxyOverriddenIpv6;
    }

    public static APIUpdateConsoleProxyAgentMsg __example__() {
        APIUpdateConsoleProxyAgentMsg msg = new APIUpdateConsoleProxyAgentMsg();
        msg.setUuid(uuid());
        msg.setConsoleProxyOverriddenIp("127.0.0.1");
        msg.setConsoleProxyOverriddenIpv4("127.0.0.1");
        msg.setConsoleProxyOverriddenIpv6("2001:db8::100");
        msg.setConsoleProxyPort(4789);
        return msg;
    }
}
