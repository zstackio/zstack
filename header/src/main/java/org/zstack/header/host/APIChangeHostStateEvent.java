package org.zstack.header.host;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

/**
 * @apiResult api event for 'APIChangeHostStateMsg' message
 * @example {
 * "org.zstack.header.host.APIChangeHostStateEvent": {
 * "inventory": {
 * "zoneUuid": "69b3f4825d044f8ab4dae082f2e136fc",
 * "name": "Host-0",
 * "uuid": "2ce70b43f55f43e88e12ec5b75e08978",
 * "clusterUuid": "c595fc1da4c84990a916497a98803b9b",
 * "description": "Test Host",
 * "managementIp": "10.0.0.0",
 * "hypervisorType": "Simulator",
 * "state": "Enabled",
 * "status": "Connected",
 * "createDate": "Apr 29, 2014 7:56:29 PM",
 * "lastOpDate": "Apr 29, 2014 7:56:29 PM"
 * }
 * }
 * }
 * @since 0.1.0
 */
@RestResponse(allTo = "inventory")
public class APIChangeHostStateEvent extends APIEvent {
    /**
     * @desc see :ref:`HostInventory`
     */
    private HostInventory inventory;

    public APIChangeHostStateEvent() {
        super(null);
    }

    public APIChangeHostStateEvent(String apiId) {
        super(apiId);
    }

    public HostInventory getInventory() {
        return inventory;
    }

    public void setInventory(HostInventory inventory) {
        this.inventory = inventory;
    }
}
