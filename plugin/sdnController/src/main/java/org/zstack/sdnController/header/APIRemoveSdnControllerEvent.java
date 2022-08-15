package org.zstack.sdnController.header;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;


@RestResponse
public class APIRemoveSdnControllerEvent extends APIEvent {

    public APIRemoveSdnControllerEvent() {
        super(null);
    }

    public APIRemoveSdnControllerEvent(String apiId) {
        super(apiId);
    }

    public static APIRemoveSdnControllerEvent __example__() {
        APIRemoveSdnControllerEvent event = new APIRemoveSdnControllerEvent();
        return event;
    }

}
