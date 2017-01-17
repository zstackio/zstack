package org.zstack.header.network.l2;

import org.zstack.header.message.APIListMessage;

import java.util.List;

public class APIListL2NetworkMsg extends APIListMessage {
    public APIListL2NetworkMsg(List<String> uuids) {
        super(uuids);
    }

    public APIListL2NetworkMsg() {
    }
 
    public static APIListL2NetworkMsg __example__() {
        APIListL2NetworkMsg msg = new APIListL2NetworkMsg();


        return msg;
    }

}
