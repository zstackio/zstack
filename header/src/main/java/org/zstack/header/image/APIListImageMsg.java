package org.zstack.header.image;

import org.zstack.header.message.APIListMessage;

import java.util.List;

public class APIListImageMsg extends APIListMessage {

    public APIListImageMsg() {
    }
    
    public APIListImageMsg(List<String> uuids) {
        super(uuids);
    }
}
