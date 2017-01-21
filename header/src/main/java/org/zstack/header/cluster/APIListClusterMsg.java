package org.zstack.header.cluster;

import org.zstack.header.message.APIListMessage;

import java.util.List;

public class APIListClusterMsg extends APIListMessage {
    public APIListClusterMsg() {
    }

    public APIListClusterMsg(List<String> uuids) {
        super(uuids);
    }
 
    public static APIListClusterMsg __example__() {
        APIListClusterMsg msg = new APIListClusterMsg();
        //deprecated
        return msg;
    }

}
