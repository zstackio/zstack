package org.zstack.header.host;

import org.zstack.header.message.APIListMessage;

import java.util.List;

import static org.zstack.utils.CollectionDSL.list;

public class APIListHostMsg extends APIListMessage {

    public APIListHostMsg() {
    }

    public APIListHostMsg(List<String> uuids) {
        super(uuids);
    }
 
    public static APIListHostMsg __example__() {
        APIListHostMsg msg = new APIListHostMsg();
        msg.setUuids(list(uuid(),uuid()));

        return msg;
    }

}
