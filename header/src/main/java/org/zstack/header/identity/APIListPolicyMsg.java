package org.zstack.header.identity;

import org.zstack.header.message.APIListMessage;

import java.util.List;

public class APIListPolicyMsg extends APIListMessage {
    public APIListPolicyMsg() {
    }

    public APIListPolicyMsg(List<String> uuids) {
        super(uuids);
    }
 
    public static APIListPolicyMsg __example__() {
        APIListPolicyMsg msg = new APIListPolicyMsg();


        return msg;
    }

}
