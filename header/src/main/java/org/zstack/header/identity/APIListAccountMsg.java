package org.zstack.header.identity;

import org.zstack.header.message.APIListMessage;

import java.util.List;

public class APIListAccountMsg extends APIListMessage {
    public APIListAccountMsg(List<String> uuids) {
        super(uuids);
    }
    
    public APIListAccountMsg() {
    }
}
