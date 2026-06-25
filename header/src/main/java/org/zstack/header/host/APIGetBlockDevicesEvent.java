package org.zstack.header.host;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;

import java.util.Collections;
import java.util.List;

@RestResponse(fieldsTo = "all")
public class APIGetBlockDevicesEvent extends APIEvent {
    private List<HostBlockDeviceStruct> blockDevices;

    public APIGetBlockDevicesEvent() {
    }

    public APIGetBlockDevicesEvent(String apiId) {
        super(apiId);
    }

    public List<HostBlockDeviceStruct> getBlockDevices() {
        return blockDevices;
    }

    public void setBlockDevices(List<HostBlockDeviceStruct> blockDevices) {
        this.blockDevices = blockDevices;
    }

    public static APIGetBlockDevicesEvent __example__() {
        APIGetBlockDevicesEvent event = new APIGetBlockDevicesEvent();
        HostBlockDeviceStruct struct = new HostBlockDeviceStruct();
        struct.setName("sda");
        struct.setWwid("3600508b400105e5a0000800001490000");
        struct.setVendor("VMware");
        struct.setModel("Virtual disk");
        struct.setWwn("0x6000c2990b2c19db");
        struct.setSerial("6000c2990b2c19db");
        struct.setHctl("0:0:0:0");
        struct.setType("disk");
        struct.setSize(107374182400L);
        struct.setPath("/dev/sda");
        struct.setSource("block");
        struct.setTransport("fc");
        event.setBlockDevices(Collections.singletonList(struct));
        return event;
    }
}
