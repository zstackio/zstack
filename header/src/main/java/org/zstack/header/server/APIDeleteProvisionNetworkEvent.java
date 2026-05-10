package org.zstack.header.server;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDeleteProvisionNetworkEvent extends APIEvent {
    public APIDeleteProvisionNetworkEvent() { super(null); }
    public APIDeleteProvisionNetworkEvent(String apiId) { super(apiId); }

    public static APIDeleteProvisionNetworkEvent __example__() {
        return new APIDeleteProvisionNetworkEvent();
    }
}
