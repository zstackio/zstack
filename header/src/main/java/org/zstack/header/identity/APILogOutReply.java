package org.zstack.header.identity;

import org.zstack.header.rest.RestResponse;

@RestResponse
public class APILogOutReply extends APILogInAuditorReply {

    public static APILogOutReply __example__() {
        APILogOutReply reply = new APILogOutReply();
        return reply;
    }
}
