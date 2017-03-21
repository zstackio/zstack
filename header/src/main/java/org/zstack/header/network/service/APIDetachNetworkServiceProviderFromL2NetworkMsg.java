package org.zstack.header.network.service;

import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.notification.ApiNotification;

public class APIDetachNetworkServiceProviderFromL2NetworkMsg extends APIMessage {
    @APIParam
    private String networkServiceProviderUuid;
    @APIParam
    private String l2NetworkUuid;

    public String getNetworkServiceProviderUuid() {
        return networkServiceProviderUuid;
    }

    public String getL2NetworkUuid() {
        return l2NetworkUuid;
    }

    public void setL2NetworkUuid(String l2NetworkUuid) {
        this.l2NetworkUuid = l2NetworkUuid;
    }

    public void setNetworkServiceProviderUuid(String networkServiceProviderUuid) {
        this.networkServiceProviderUuid = networkServiceProviderUuid;
    }
 
    public static APIDetachNetworkServiceProviderFromL2NetworkMsg __example__() {
        APIDetachNetworkServiceProviderFromL2NetworkMsg msg = new APIDetachNetworkServiceProviderFromL2NetworkMsg();
        return msg;
    }

    public ApiNotification __notification__() {
        APIMessage that = this;

        return new ApiNotification() {
            @Override
            public void after(APIEvent evt) {
                if (evt.isSuccess()) {
                    ntfy("Detached network service provider[uuid:%s]",networkServiceProviderUuid).resource(l2NetworkUuid,L2NetworkVO.class.getSimpleName())
                            .messageAndEvent(that, evt).done();
                }
            }
        };
    }
}
