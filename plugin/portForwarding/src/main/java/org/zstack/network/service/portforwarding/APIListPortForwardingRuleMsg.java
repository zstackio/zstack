package org.zstack.network.service.portforwarding;

import org.zstack.header.message.APIListMessage;

import java.util.List;

public class APIListPortForwardingRuleMsg extends APIListMessage {

    public APIListPortForwardingRuleMsg(List<String> uuids) {
    }
    
    public APIListPortForwardingRuleMsg() {
    }

 
    public static APIListPortForwardingRuleMsg __example__() {
        APIListPortForwardingRuleMsg msg = new APIListPortForwardingRuleMsg();
        //deprecated


        return msg;
    }

}
