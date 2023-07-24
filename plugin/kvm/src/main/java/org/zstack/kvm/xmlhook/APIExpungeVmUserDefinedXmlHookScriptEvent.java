package org.zstack.kvm.xmlhook;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse
public class APIExpungeVmUserDefinedXmlHookScriptEvent extends APIEvent {
    public APIExpungeVmUserDefinedXmlHookScriptEvent() {
    }

    public APIExpungeVmUserDefinedXmlHookScriptEvent(String apiId) {
        super(apiId);
    }

    public static APIExpungeVmUserDefinedXmlHookScriptEvent __example__() {
        APIExpungeVmUserDefinedXmlHookScriptEvent event = new APIExpungeVmUserDefinedXmlHookScriptEvent();
        return event;
    }
}
