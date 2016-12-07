package org.zstack.network.securitygroup;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 *@apiResult
 * api event for :ref:`APIChangeSecurityGroupStateMsg`
 *
 *@category security group
 *
 *@since 0.1.0
 *
 *@example
 * {
"org.zstack.network.securitygroup.APIChangeSecurityGroupStateEvent": {
"inventory": {
"uuid": "6a6eb010bdcb4b6296ea1972c437c459",
"name": "test",
"state": "Disabled",
"createDate": "May 14, 2014 10:10:11 PM",
"lastOpDate": "May 14, 2014 10:10:11 PM",
"internalId": 1,
"rules": [],
"attachedL3NetworkUuids": [
"d08c75080c3f45538c088e963e464f7b"
]
},
"apiId": "598b09e6a3b447c69fef64e02822682d",
"success": true,
"id": "39e18484de7947a5a0eeb1cb7993db13"
}
}
 */
@RestResponse(allTo = "inventory")
public class APIChangeSecurityGroupStateEvent extends APIEvent {
    /**
     * @desc :ref:`SecurityGroupInventory`
     */
    private SecurityGroupInventory inventory;

    public APIChangeSecurityGroupStateEvent() {
        super(null);
    }

    public APIChangeSecurityGroupStateEvent(String apiId) {
        super(apiId);
    }

    public SecurityGroupInventory getInventory() {
        return inventory;
    }

    public void setInventory(SecurityGroupInventory inventory) {
        this.inventory = inventory;
    }
}
