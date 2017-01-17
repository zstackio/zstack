package org.zstack.header.network.service;

import org.zstack.header.message.APIListMessage;

import java.util.List;

public class APIListNetworkServiceProviderMsg extends APIListMessage {
    public APIListNetworkServiceProviderMsg(List<String> uuids) {
        super(uuids);
    }

    public APIListNetworkServiceProviderMsg() {
        super(null);
    }
 
    public static APIListNetworkServiceProviderMsg __example__() {
        APIListNetworkServiceProviderMsg msg = new APIListNetworkServiceProviderMsg();


        return msg;
    }

}
