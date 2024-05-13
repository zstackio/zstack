package org.zstack.header.network.l3;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.utils.network.IPv6Constants;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RestResponse(allTo = "inventories")
public class APIReserveIpAddreessEvent extends APIEvent {
    /**
     * @desc see :ref:`IpRangeInventory`
     */
    private List<UsedIpInventory> inventories;

    public APIReserveIpAddreessEvent(String apiId) {
        super(apiId);
    }

    public APIReserveIpAddreessEvent() {
        super(null);
    }

    public List<UsedIpInventory> getInventories() {
        return inventories;
    }

    public void setInventories(List<UsedIpInventory> inventories) {
        this.inventories = inventories;
    }

    public static APIReserveIpAddreessEvent __example__() {
        APIReserveIpAddreessEvent event = new APIReserveIpAddreessEvent();
        UsedIpInventory ip = new UsedIpInventory();

        ip.setVmNicUuid(uuid());
        ip.setUuid(uuid());
        ip.setL3NetworkUuid(uuid());
        ip.setGateway("192.168.1.1");
        ip.setIpRangeUuid(uuid());
        ip.setIpVersion(IPv6Constants.IPv4);
        ip.setIp("192.168.1.100");
        ip.setNetmask("255.255.255.0");

        event.setInventories(Collections.singletonList(ip));
        return event;
    }

}
