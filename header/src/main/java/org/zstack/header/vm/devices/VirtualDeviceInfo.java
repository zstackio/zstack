package org.zstack.header.vm.devices;

public class VirtualDeviceInfo {
    private String resourceUuid;
    private PciAddressConfig pciInfo;

    public String getResourceUuid() {
        return resourceUuid;
    }

    public void setResourceUuid(String resourceUuid) {
        this.resourceUuid = resourceUuid;
    }

    public PciAddressConfig getPciInfo() {
        return pciInfo;
    }

    public void setPciInfo(PciAddressConfig pciInfo) {
        this.pciInfo = pciInfo;
    }
}
