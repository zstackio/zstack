package org.zstack.physicalserver;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIRestartPhysicalServerManagedServicesEvent extends APIEvent {
    public APIRestartPhysicalServerManagedServicesEvent() {
    }

    public APIRestartPhysicalServerManagedServicesEvent(String apiId) {
        super(apiId);
    }
}
