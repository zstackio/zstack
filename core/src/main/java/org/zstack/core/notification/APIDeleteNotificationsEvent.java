package org.zstack.core.notification;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by xing5 on 2017/3/18.
 */
@RestResponse
public class APIDeleteNotificationsEvent extends APIEvent {
    public APIDeleteNotificationsEvent() {
    }

    public APIDeleteNotificationsEvent(String apiId) {
        super(apiId);
    }

    public static APIDeleteNotificationsEvent __example__() {
        APIDeleteNotificationsEvent msg = new APIDeleteNotificationsEvent();
        return msg;
    }
    
}