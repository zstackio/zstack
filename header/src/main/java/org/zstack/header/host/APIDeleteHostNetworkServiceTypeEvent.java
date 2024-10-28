package org.zstack.header.host;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by boce.wang on 10/25/2024.
 */
@RestResponse
public class APIDeleteHostNetworkServiceTypeEvent extends APIEvent {

    public APIDeleteHostNetworkServiceTypeEvent() {
    }

    public APIDeleteHostNetworkServiceTypeEvent(String apiId) {
        super(apiId);
    }
    public static APIDeleteHostNetworkServiceTypeEvent __example__() {
        APIDeleteHostNetworkServiceTypeEvent event = new APIDeleteHostNetworkServiceTypeEvent();
        event.setSuccess(true);
        return event;
    }
}
