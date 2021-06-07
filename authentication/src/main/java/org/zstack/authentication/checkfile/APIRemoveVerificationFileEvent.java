package org.zstack.authentication.checkfile;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIRemoveVerificationFileEvent extends APIEvent {
    public APIRemoveVerificationFileEvent(String apiId) {super(apiId);}
    public APIRemoveVerificationFileEvent(){super(null);}

    public static APIRemoveVerificationFileEvent __example__(){
        APIRemoveVerificationFileEvent event = new APIRemoveVerificationFileEvent();
        event.setSuccess(true);
        return event;
    }
}
