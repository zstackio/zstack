package org.zstack.header.zone;

import org.zstack.header.message.APIListMessage;

import java.util.List;

public class APIListZonesMsg extends APIListMessage {
    public APIListZonesMsg() {
    }

    public APIListZonesMsg(List<String> uuids) {
        super(uuids);
    }
 
    public static APIListZonesMsg __example__() {
        APIListZonesMsg msg = new APIListZonesMsg();
        //deprecated


        return msg;
    }

}
