package org.zstack.physicalNetworkInterface.header;

import org.checkerframework.checker.units.qual.A;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIResponse;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIRecoverPhysicalNicEvent extends APIEvent {
    public APIRecoverPhysicalNicEvent() {
    }

    public APIRecoverPhysicalNicEvent(String apiId) {
        super(apiId);
    }

    public static APIRecoverPhysicalNicEvent __example__() {
        return new APIRecoverPhysicalNicEvent();
    }
}
