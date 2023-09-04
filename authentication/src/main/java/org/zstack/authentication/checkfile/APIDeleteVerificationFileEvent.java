package org.zstack.authentication.checkfile;


import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteVerificationFileEvent extends APIEvent {
    public APIDeleteVerificationFileEvent(String apiId) {super(apiId);}
    public APIDeleteVerificationFileEvent(){super(null);}

    public static APIDeleteVerificationFileEvent __example__(){
        APIDeleteVerificationFileEvent event = new APIDeleteVerificationFileEvent();
        event.setSuccess(true);
        return event;
    }
}
