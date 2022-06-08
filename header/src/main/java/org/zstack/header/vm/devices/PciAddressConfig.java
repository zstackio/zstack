package org.zstack.header.vm.devices;

import org.zstack.utils.gson.JSONObjectUtil;

/**
 * pci address obeys the format 0000:65:00.0
 * and the format matches domain:bus:device.function
 * refer to kvm usage this format matches domain:bus:slot.function
 */
public class PciAddressConfig {
    public String type;
    public String domain;
    public String bus;
    public String slot;
    public String function;

    public static PciAddressConfig fromString(String address) {
        return JSONObjectUtil.toObject(address, PciAddressConfig.class);
    }

    public String toString() {
        return JSONObjectUtil.toJsonString(this);
    }


}
