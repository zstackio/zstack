package org.zstack.header.network.service;

import org.zstack.header.identity.Action;
import org.zstack.header.network.l2.L2NetworkConstant;
import org.zstack.header.search.APIGetMessage;

@Action(category = L2NetworkConstant.ACTION_CATEGORY, names = {"read"})
public class APIGetNetworkServiceProviderMsg extends APIGetMessage {

 
    public static APIGetNetworkServiceProviderMsg __example__() {
        APIGetNetworkServiceProviderMsg msg = new APIGetNetworkServiceProviderMsg();


        return msg;
    }

}
