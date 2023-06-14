package org.zstack.physicalNetworkInterface.header;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.pciDevice.virtual.APIGenerateVirtualPciDevicesEvent;

@RestResponse
public class APIConfigurePhysicalNicEvent extends APIEvent {
    public APIConfigurePhysicalNicEvent() {
    }

    public APIConfigurePhysicalNicEvent(String apiId) {
        super(apiId);
    }

    public static APIConfigurePhysicalNicEvent __example__() {
        return new APIConfigurePhysicalNicEvent();
    }
}
