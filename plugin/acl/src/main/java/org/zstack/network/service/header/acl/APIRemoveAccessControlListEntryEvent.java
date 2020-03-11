package org.zstack.network.service.header.acl;

import org.zstack.header.message.APIEvent;

/**
 * @author: zhanyong.miao
 * @date: 2020-03-10
 **/
public class APIRemoveAccessControlListEntryEvent extends APIEvent {
    public APIRemoveAccessControlListEntryEvent() {
    }

    public APIRemoveAccessControlListEntryEvent(String apiId) {
        super(apiId);
    }

    public static APIRemoveAccessControlListEntryEvent __example__() {
        APIRemoveAccessControlListEntryEvent event = new APIRemoveAccessControlListEntryEvent();
        event.setSuccess(true);
        return event;
    }

}