package org.zstack.sdnController.header;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIPullSdnControllerEvent extends APIEvent {
    public APIPullSdnControllerEvent() { }
    public APIPullSdnControllerEvent(String apiId) { super(apiId); }
}
