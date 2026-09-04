package org.zstack.physicalserver;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIRefreshPhysicalServerResourceAssignmentsFromProfileEvent extends APIEvent {
    public APIRefreshPhysicalServerResourceAssignmentsFromProfileEvent() {
    }

    public APIRefreshPhysicalServerResourceAssignmentsFromProfileEvent(String apiId) {
        super(apiId);
    }
}
