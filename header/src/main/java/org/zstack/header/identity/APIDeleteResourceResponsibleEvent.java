package org.zstack.header.identity;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by frank on 7/9/2015.
 */
@RestResponse
public class APIDeleteResourceResponsibleEvent extends APIEvent {
    
    public APIDeleteResourceResponsibleEvent() {
        super();
    }
    
    public APIDeleteResourceResponsibleEvent(String apiId) {
        super(apiId);
    }
    
    public static APIDeleteResourceResponsibleEvent __example__() {
        APIDeleteResourceResponsibleEvent event = new APIDeleteResourceResponsibleEvent();
        return event;
    }
}
