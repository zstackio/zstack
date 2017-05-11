package org.zstack.core.notification;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by xing5 on 2017/3/18.
 */
@RestResponse
public class APIUpdateNotificationsStatusEvent extends APIEvent {
    public APIUpdateNotificationsStatusEvent() {
    }

    public APIUpdateNotificationsStatusEvent(String apiId) {
        super(apiId);
    }

    public static APIUpdateNotificationsStatusEvent __example__() {
        APIUpdateNotificationsStatusEvent msg = new APIUpdateNotificationsStatusEvent();
        return msg;
    }
    
}