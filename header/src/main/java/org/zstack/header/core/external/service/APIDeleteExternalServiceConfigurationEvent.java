package org.zstack.header.core.external.service;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @Author: ya.wang
 * @Date: 1/15/26 1:47 AM
 */
@RestResponse
public class APIDeleteExternalServiceConfigurationEvent extends APIEvent {
    public APIDeleteExternalServiceConfigurationEvent() {}

    public APIDeleteExternalServiceConfigurationEvent(String apiId) { super(apiId); }

    public static APIDeleteExternalServiceConfigurationEvent __example__() {
        APIDeleteExternalServiceConfigurationEvent event = new APIDeleteExternalServiceConfigurationEvent();
        event.setSuccess(true);
        return event;
    }
}
