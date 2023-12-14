package org.zstack.header.securitymachine.api.secretresourcepool;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * Created by LiangHanYu on 2021/11/3 17:37
 */

@RestResponse
public class APIDeleteSecretResourcePoolEvent extends APIEvent {
    public APIDeleteSecretResourcePoolEvent() {
    }

    public APIDeleteSecretResourcePoolEvent(String apiId) {
        super(apiId);
    }

    public static APIDeleteSecretResourcePoolEvent __example__() {
        APIDeleteSecretResourcePoolEvent event = new APIDeleteSecretResourcePoolEvent();
        event.setSuccess(true);
        return event;
    }
}