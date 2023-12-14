package org.zstack.header.securitymachine.api.securitymachine;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by LiangHanYu on 2021/11/3 18:21
 */
@RestResponse
public class APIDeleteSecurityMachineEvent extends APIEvent {
    public APIDeleteSecurityMachineEvent() {
    }

    public APIDeleteSecurityMachineEvent(String apiId) {
        super(apiId);
    }

    public static APIDeleteSecurityMachineEvent __example__() {
        APIDeleteSecurityMachineEvent event = new APIDeleteSecurityMachineEvent();
        event.setSuccess(true);
        return event;
    }
}