package org.zstack.header.image;

import org.zstack.header.message.APIListMessage;

import java.util.List;

public class APIListImageMsg extends APIListMessage {

    public APIListImageMsg() {
    }

    public APIListImageMsg(List<String> uuids) {
        super(uuids);
    }
 

    public static APIListImageMsg __example__() {
        APIListImageMsg msg = new APIListImageMsg();
        return msg;
    }
    
}