package org.zstack.header.image;

import org.zstack.header.message.APIEvent;

/**
 * Created by frank on 11/15/2015.
 */
public class APIExpungeImageEvent extends APIEvent {
    public APIExpungeImageEvent() {
    }

    public APIExpungeImageEvent(String apiId) {
        super(apiId);
    }
}
