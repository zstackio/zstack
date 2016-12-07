package org.zstack.network.service.vip;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.rest.RestResponse;

/**
 *@apiResult
 * api event for message :ref:`APICreateVipMsg`
 *
 *@category vip
 *
 *@since 0.1.0
 *
 *@example
 * {
"org.zstack.network.service.vip.APICreateVipEvent": {
"inventory": {
"uuid": "a6e0feb8191538f991672b6a1cb4fa17",
"name": "vip",
"ipRangeUuid": "6c620829bd8d4c948bfad3b64a12e00e",
"l3NetworkUuid": "d0aff3c3e0104b089d90e7efebd84a7c",
"ip": "192.168.1.50",
"gateway": "192.168.1.1",
"netmask": "255.255.255.0",
"createDate": "May 13, 2014 10:25:06 PM",
"lastOpDate": "May 13, 2014 10:25:06 PM"
},
"success": true
}
}
 */
@RestResponse(allTo = "inventory")
public class APICreateVipEvent extends APIEvent {
    /**
     * @desc see :ref:`VipInventory`
     */
    private VipInventory inventory;

    public APICreateVipEvent(String apiId) {
        super(apiId);
    }
    
    public APICreateVipEvent() {
        super(null);
    }
    
    public VipInventory getInventory() {
        return inventory;
    }

    public void setInventory(VipInventory inventory) {
        this.inventory = inventory;
    }
}
