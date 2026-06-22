package org.zstack.header.console;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by GuoYi on 2018-09-13.
 */
public class UpdateConsoleProxyAgentMsg extends NeedReplyMessage implements ConsoleProxyAgentMessage {
    private String uuid;
    private String consoleProxyOverriddenIp;
    private String consoleProxyOverriddenIpv4;
    private String consoleProxyOverriddenIpv6;
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
}
