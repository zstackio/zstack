package org.zstack.header.configuration;

import org.zstack.header.message.APIListMessage;

import java.util.List;

public class APIListInstanceOfferingMsg extends APIListMessage {

    public APIListInstanceOfferingMsg() {
    }

    public APIListInstanceOfferingMsg(List<String> uuids) {
        super(uuids);
    }
 
    public static APIListInstanceOfferingMsg __example__() {
        APIListInstanceOfferingMsg msg = new APIListInstanceOfferingMsg();


        return msg;
    }

}
