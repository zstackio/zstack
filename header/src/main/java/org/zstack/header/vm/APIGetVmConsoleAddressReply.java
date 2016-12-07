package org.zstack.header.vm;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

/**
 * Created by frank on 1/25/2016.
 */
@RestResponse(fieldsTo = {"all"})
public class APIGetVmConsoleAddressReply extends APIReply {
    private String hostIp;
    private int port;
    private String protocol;

    public String getHostIp() {
        return hostIp;
    }

    public void setHostIp(String hostIp) {
        this.hostIp = hostIp;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
}
