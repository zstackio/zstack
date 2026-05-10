package org.zstack.header.server;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIDetachProvisionNetworkFromClusterEvent extends APIEvent {
    public APIDetachProvisionNetworkFromClusterEvent() {}
    public APIDetachProvisionNetworkFromClusterEvent(String apiId) { super(apiId); }

    public static APIDetachProvisionNetworkFromClusterEvent __example__() {
        return new APIDetachProvisionNetworkFromClusterEvent();
    }
}
