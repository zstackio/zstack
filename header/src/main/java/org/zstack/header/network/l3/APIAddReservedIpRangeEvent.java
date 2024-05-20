package org.zstack.header.network.l3;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

@RestResponse(allTo = "inventory")
public class APIAddReservedIpRangeEvent extends APIEvent {
    /**
     * @desc see :ref:`IpRangeInventory`
     */
    private ReservedIpRangeInventory inventory;

    public APIAddReservedIpRangeEvent(String apiId) {
        super(apiId);
    }

    public APIAddReservedIpRangeEvent() {
        super(null);
    }

    public ReservedIpRangeInventory getInventory() {
        return inventory;
    }

    public void setInventory(ReservedIpRangeInventory inventory) {
        this.inventory = inventory;
    }

    public static APIAddReservedIpRangeEvent __example__() {
        APIAddReservedIpRangeEvent event = new APIAddReservedIpRangeEvent();
        ReservedIpRangeInventory ipRange = new ReservedIpRangeInventory();

        ipRange.setL3NetworkUuid(uuid());
        ipRange.setName("Test-IP-Range");
        ipRange.setStartIp("192.168.100.10");
        ipRange.setEndIp("192.168.100.250");

        event.setInventory(ipRange);
        return event;
    }

}
