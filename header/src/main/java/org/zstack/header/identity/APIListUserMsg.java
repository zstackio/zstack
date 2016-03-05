package org.zstack.header.identity;

import org.zstack.header.message.APIListMessage;

import java.util.List;

public class APIListUserMsg extends APIListMessage {
    public APIListUserMsg() {
    }
    
    public APIListUserMsg(List<String> uuids) {
        super(uuids);
    }
}
