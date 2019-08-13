package org.zstack.authentication.checkfile;


import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIRecoverVerificationFileEvent extends APIEvent {
    public APIRecoverVerificationFileEvent(String apiId) {super(apiId);}
    public APIRecoverVerificationFileEvent(){super(null);}

    public static APIRecoverVerificationFileEvent __example__(){
        APIRecoverVerificationFileEvent event = new APIRecoverVerificationFileEvent();
        event.setSuccess(true);
        return event;
    }
}
