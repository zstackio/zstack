package org.zstack.network.service.portforwarding;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 *@apiResult
 * api event for message :ref:`APIAttachPortForwardingRuleMsg`
 *
 *@category port forwarding
 *
 *@since 0.1.0
 *
 *@example
 * {
"org.zstack.network.service.portforwarding.APIAttachPortForwardingRuleEvent": {
"inventory": {
"uuid": "bc82d5c4f9394c24b7fa19ee611c0857",
"name": "pfRule1",
"vipUuid": "7b5216172fe83c05940e15c629922a79",
"vipPortStart": 22,
"vipPortEnd": 100,
"privatePortStart": 22,
"privatePortEnd": 100,
"vmNicUuid": "5dfef29a376a49de9e1a887ea9bfe683",
"protocolType": "TCP",
"allowedCidr": "77.10.3.1/24",
"createDate": "May 6, 2014 11:07:57 PM",
"lastOpDate": "May 6, 2014 11:07:57 PM"
},
"success": true
}
}
 */
@RestResponse(allTo = "inventory")
public class APIAttachPortForwardingRuleEvent extends APIEvent {
    /**
     * @desc see :ref:`PortForwardingRuleInventory`
     */
    private PortForwardingRuleInventory inventory;

    public APIAttachPortForwardingRuleEvent(String apiId) {
        super(apiId);
    }

    public APIAttachPortForwardingRuleEvent() {
        super(null);
    }

    public PortForwardingRuleInventory getInventory() {
        return inventory;
    }

    public void setInventory(PortForwardingRuleInventory inventory) {
        this.inventory = inventory;
    }
}
