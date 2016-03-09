package org.zstack.header.network.l3;

import org.zstack.header.message.APIListMessage;

import java.util.List;

public class APIListL3NetworkMsg extends APIListMessage {
    public APIListL3NetworkMsg() {
    }
    
    public APIListL3NetworkMsg(List<String> uuids) {
        super(uuids);
    }
}
