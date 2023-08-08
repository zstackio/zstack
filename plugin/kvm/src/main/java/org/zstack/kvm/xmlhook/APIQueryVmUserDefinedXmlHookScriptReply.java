package org.zstack.kvm.xmlhook;

import org.zstack.header.query.APIQueryReply;
import org.zstack.header.rest.RestResponse;

import java.util.List;

@RestResponse(allTo = "inventories")
public class APIQueryVmUserDefinedXmlHookScriptReply extends APIQueryReply {

    private List<XmlHookInventory> inventories;

    public List<XmlHookInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<XmlHookInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIQueryVmUserDefinedXmlHookScriptReply __example__() {
        APIQueryVmUserDefinedXmlHookScriptReply reply = new APIQueryVmUserDefinedXmlHookScriptReply();
        return reply;
    }
}
