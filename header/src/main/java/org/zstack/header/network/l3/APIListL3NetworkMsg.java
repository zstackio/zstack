package org.zstack.header.network.l3;

import org.zstack.header.message.APIListMessage;

import java.util.List;

public class APIListL3NetworkMsg extends APIListMessage {
    public APIListL3NetworkMsg() {
    }

    public APIListL3NetworkMsg(List<String> uuids) {
        super(uuids);
    }
 
    public static APIListL3NetworkMsg __example__() {
        APIListL3NetworkMsg msg = new APIListL3NetworkMsg();


        return msg;
    }

}
