package org.zstack.authentication.checkfile;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIAddVerificationFileEvent extends APIEvent {
    public APIAddVerificationFileEvent(String apiId)  {
        super(apiId);
    }
    public APIAddVerificationFileEvent() { super(null); }

    public static APIAddVerificationFileEvent __example__() {
        APIAddVerificationFileEvent event = new APIAddVerificationFileEvent();
        event.setSuccess(true);
        return event;
    }
}
