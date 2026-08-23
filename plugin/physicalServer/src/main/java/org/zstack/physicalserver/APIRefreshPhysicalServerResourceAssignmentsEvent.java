package org.zstack.physicalserver;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIRefreshPhysicalServerResourceAssignmentsEvent extends APIEvent {
    public APIRefreshPhysicalServerResourceAssignmentsEvent() {
    }

    public APIRefreshPhysicalServerResourceAssignmentsEvent(String apiId) {
        super(apiId);
    }
}
