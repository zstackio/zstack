package org.zstack.network.service.flat;

import org.zstack.header.message.APIReply;
import org.zstack.header.rest.RestResponse;

/**
 * Created by miao on 16-7-19.
 */
@RestResponse(fieldsTo = {"all"})
public class APIGetL3NetworkDhcpIpAddressReply extends APIReply {
    private String ip;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }
}
