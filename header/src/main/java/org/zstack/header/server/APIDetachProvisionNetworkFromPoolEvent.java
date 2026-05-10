package org.zstack.header.server;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDetachProvisionNetworkFromPoolEvent extends APIEvent {
    public APIDetachProvisionNetworkFromPoolEvent() {}
    public APIDetachProvisionNetworkFromPoolEvent(String apiId) { super(apiId); }

    public static APIDetachProvisionNetworkFromPoolEvent __example__() {
        return new APIDetachProvisionNetworkFromPoolEvent();
    }
}
