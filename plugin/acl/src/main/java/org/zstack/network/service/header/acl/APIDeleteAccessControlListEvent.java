package org.zstack.network.service.header.acl;

import org.zstack.header.message.APIEvent;

/**
 * @author: zhanyong.miao
 * @date: 2020-03-09
 **/
public class APIDeleteAccessControlListEvent extends APIEvent {
    public APIDeleteAccessControlListEvent() {
    }

    public APIDeleteAccessControlListEvent(String apiId) {
        super(apiId);
    }

    public static APIDeleteAccessControlListEvent __example__() {
        APIDeleteAccessControlListEvent event = new APIDeleteAccessControlListEvent();
        event.setSuccess(true);
        return event;
    }

}