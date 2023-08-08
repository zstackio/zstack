package org.zstack.kvm.xmlhook;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APICreateVmUserDefinedXmlHookScriptEvent extends APIEvent {
    private XmlHookInventory inventory;

    public APICreateVmUserDefinedXmlHookScriptEvent() {
    }

    public APICreateVmUserDefinedXmlHookScriptEvent(String apiId) {
        super(apiId);
    }

    public XmlHookInventory getInventory() {
        return inventory;
    }

    public void setInventory(XmlHookInventory inventory) {
        this.inventory = inventory;
    }

    public static APICreateVmUserDefinedXmlHookScriptEvent __example__() {
        APICreateVmUserDefinedXmlHookScriptEvent event = new APICreateVmUserDefinedXmlHookScriptEvent();
        XmlHookInventory inventory = new XmlHookInventory();
        event.setInventory(inventory);
        return event;
    }
}
