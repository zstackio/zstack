package org.zstack.header.zone;

public interface ManagementNetworkIpVersionResourceExtensionPoint {
    void validateExistingResourcesInZone(String zoneUuid, String ipVersion);
}
