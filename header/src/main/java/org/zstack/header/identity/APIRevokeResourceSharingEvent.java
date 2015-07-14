package org.zstack.header.identity;

import org.zstack.header.message.APIEvent;

/**
 * Created by frank on 7/13/2015.
 */
public class APIRevokeResourceSharingEvent extends APIEvent {
    public APIRevokeResourceSharingEvent() {
    }

    public APIRevokeResourceSharingEvent(String apiId) {
        super(apiId);
    }
}
