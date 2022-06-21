package org.zstack.kvm;

import org.zstack.header.vm.devices.PciAddressConfig;

public class BaseVirtualPciDeviceTO {
    private String resourceUuid;
    private PciAddressConfig pciAddress;

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public PciAddressConfig getPciAddress() {
        return pciAddress;
    }

    public void setPciAddress(PciAddressConfig pciAddress) {
        this.pciAddress = pciAddress;
    }
}
