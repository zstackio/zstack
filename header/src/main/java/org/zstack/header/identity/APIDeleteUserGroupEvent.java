package org.zstack.header.identity;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by frank on 7/9/2015.
 */
@RestResponse
public class APIDeleteUserGroupEvent extends APIEvent {
    public APIDeleteUserGroupEvent() {
    }

    public APIDeleteUserGroupEvent(String apiId) {
        super(apiId);
    }
 
    public static APIDeleteUserGroupEvent __example__() {
        APIDeleteUserGroupEvent event = new APIDeleteUserGroupEvent();


        return event;
    }

}
