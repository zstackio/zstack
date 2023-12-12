package org.zstack.header.securitymachine.api.securitymachine;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by LiangHanYu on 2022/3/30 11:31
 */
@RestResponse
public class APISecurityMachineDetectSyncEvent extends APIEvent {
    public APISecurityMachineDetectSyncEvent() {
    }

    public APISecurityMachineDetectSyncEvent(String apiId) {
        super(apiId);
    }

    public static APISecurityMachineDetectSyncEvent __example__() {
        APISecurityMachineDetectSyncEvent event = new APISecurityMachineDetectSyncEvent();
        event.setSuccess(true);
        return event;
    }
}