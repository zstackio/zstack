package org.zstack.header.physicalserver;

public class PhysicalServerIdentitySpec {
    private String serialNumber;
    private String zoneUuid;

    public PhysicalServerIdentitySpec() {
    }

    public PhysicalServerIdentitySpec(String serialNumber, String zoneUuid) {
        this.serialNumber = serialNumber;
        this.zoneUuid = zoneUuid;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getZoneUuid() {
        return zoneUuid;
    }

    public void setZoneUuid(String zoneUuid) {
        this.zoneUuid = zoneUuid;
    }
}
