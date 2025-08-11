package org.zstack.header.identity;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APISetResourceResponsibleEvent extends APIEvent {
    public APISetResourceResponsibleEvent() {
    }

    public APISetResourceResponsibleEvent(String apiId) {
        super(apiId);
    }
 
    public static APISetResourceResponsibleEvent __example__() {
        APISetResourceResponsibleEvent event = new APISetResourceResponsibleEvent();


        return event;
    }

}
