package org.zstack.header.zone;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.sql.Timestamp;

/**
 * Created by frank on 6/14/2015.
 */
@RestResponse(allTo = "inventory")
public class APIUpdateZoneEvent extends APIEvent {
    private ZoneInventory inventory;

    public APIUpdateZoneEvent() {
    }

    public APIUpdateZoneEvent(String apiId) {
        super(apiId);
    }

    public ZoneInventory getInventory() {
        return inventory;
    }

    public void setInventory(ZoneInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APIUpdateZoneEvent __example__() {
        APIUpdateZoneEvent event = new APIUpdateZoneEvent();
        ZoneInventory zone = new ZoneInventory();
        zone.setName("TestZone");
        zone.setUuid(uuid());
        zone.setDescription("Test");
        zone.setState("Enabled");
        zone.setType("zstack");
        zone.setCreateDate(new Timestamp(System.currentTimeMillis()));
        zone.setLastOpDate(new Timestamp(System.currentTimeMillis()));
        event.setInventory(zone);
        return event;
    }

}
