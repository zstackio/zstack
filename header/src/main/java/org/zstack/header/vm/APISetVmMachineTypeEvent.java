package org.zstack.header.vm;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APISetVmMachineTypeEvent extends APIEvent {
    public APISetVmMachineTypeEvent() {
    }

    public APISetVmMachineTypeEvent(String apiId) {
        super(apiId);
    }

    public static APISetVmMachineTypeEvent __example__() {
        return new APISetVmMachineTypeEvent();
    }
}
