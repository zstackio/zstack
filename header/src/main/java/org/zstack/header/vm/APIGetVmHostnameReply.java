package org.zstack.header.vm;

import org.zstack.header.message.APIReply;

/**
 * Created by xing5 on 2016/4/8.
 */
public class APIGetVmHostnameReply extends APIReply {
    private String hostname;

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }
}
