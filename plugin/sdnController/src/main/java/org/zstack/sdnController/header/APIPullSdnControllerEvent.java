package org.zstack.sdnController.header;

import org.zstack.header.message.APIEvent;

public class APIPullSdnControllerEvent extends APIEvent {
    public APIPullSdnControllerEvent() { }
    public APIPullSdnControllerEvent(String apiId) { super(apiId); }
}
