package org.zstack.network.l2.vxlan.vxlanNetwork;

import org.zstack.header.message.APIEvent;
import org.zstack.header.network.l2.L2VlanNetworkInventory;
import org.zstack.header.rest.RestResponse;

/**
 * @apiResult api event for :ref:`APICreateL2VxlanNetworkMsg`
 * @category l2network
 * @example {
 * "org.zstack.header.network.l2.APICreateL2VxlanNetworkEvent": {
 * "inventory": {
 * "vni": 10,
 * "uuid": "9186dff66cd94485bc6adb0b360cd7e0",
 * "name": "TestL2VxlanNetwork",
 * "description": "Test",
 * "zoneUuid": "d81c3d3d008e46038b8a38fee595fe41",
 * "physicalInterface": "eth0.1100",
 * "vtepCidr": "172.20.0.0/24",
 * "poolUuid": "",
 * "type": "L2VxlanNetwork",
 * "createDate": "May 3, 2014 10:57:12 PM",
 * "lastOpDate": "May 3, 2014 10:57:12 PM",
 * "attachedClusterUuids": []
 * },
 * "success": true
 * }
 * }
 * @since 1.10.0
 */
@RestResponse(allTo = "inventory")
public class APICreateL2VxlanNetworkEvent extends APIEvent {
    /**
     * @desc see :ref:`L2VlanNetworkInventory`
     */
    private L2VxlanNetworkInventory inventory;

    public APICreateL2VxlanNetworkEvent(String apiId) {
        super(apiId);
    }

    public L2VxlanNetworkInventory getInventory() {
        return inventory;
    }

    public APICreateL2VxlanNetworkEvent() {
        super(null);
    }

    public void setInventory(L2VxlanNetworkInventory inventory) {
        this.inventory = inventory;
    }

    public static APICreateL2VxlanNetworkEvent __example__() {
        APICreateL2VxlanNetworkEvent event = new APICreateL2VxlanNetworkEvent();
        L2VxlanNetworkInventory net = new L2VxlanNetworkInventory();

        net.setName("Test-Net");
        net.setVni(10);
        net.setDescription("Test");
        net.setZoneUuid(uuid());
        net.setPhysicalInterface("eth0.1100");
        net.setVtepCidr("172.20.0.0/24");
        net.setPoolUuid("");
        net.setType("L2VxlanNetwork");

        event.setInventory(net);
        return event;
    }
}
