package org.zstack.kvm.xmlhook;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIUpdateVmUserDefinedXmlHookScriptEvent extends APIEvent {
    private XmlHookInventory inventory;

    public void setInventory(XmlHookInventory inventory) {
        this.inventory = inventory;
    }

    public APIUpdateVmUserDefinedXmlHookScriptEvent() {
    }

    public APIUpdateVmUserDefinedXmlHookScriptEvent(String apiId) {
        super(apiId);
    }

    public XmlHookInventory getInventory() {
        return inventory;
    }

    public static APIUpdateVmUserDefinedXmlHookScriptEvent __example__() {
        APIUpdateVmUserDefinedXmlHookScriptEvent event = new APIUpdateVmUserDefinedXmlHookScriptEvent();
        return event;
    }
}
