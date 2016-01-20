package org.zstack.header.managementnode;

import org.zstack.header.message.APIReply;

/**
 * Created by frank on 11/14/2015.
 */
public class APIGetVersionReply extends APIReply {
    private String version;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
