package org.zstack.header.image;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIExpungeImageGroupEvent extends APIEvent {
    public APIExpungeImageGroupEvent() {
    }

    public APIExpungeImageGroupEvent(String apiId) {
        super(apiId);
    }

    public static APIExpungeImageGroupEvent __example__() {
        APIExpungeImageGroupEvent event = new APIExpungeImageGroupEvent();


        return event;
    }
}
