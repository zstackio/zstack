package org.zstack.header.zone;

public interface ManagementNetworkIpVersionManager {
    /**
     * Validate an endpoint against the management network IP version configured
     * on a zone.
     *
     * @param zoneUuid zone UUID; validation is skipped when it is null
     * @param endpoint address, URI, or storage URL to validate
     * @param resourceType resource type used in the error message
     * @param resourceIdentity resource identifier used in the error message
     * @param errorCode global error code used when IP versions mismatch
     * @throws org.zstack.header.apimediator.ApiMessageInterceptionException
     *         when the endpoint uses an unsupported IPv6 link-local address or
     *         a different IP version from the zone
     */
    void validateEndpointInZone(String zoneUuid, String endpoint, String resourceType, String resourceIdentity, String errorCode);

    /**
     * Validate an endpoint against a caller-provided IP version.
     *
     * @param zoneUuid zone UUID used in the error message; validation is skipped
     *                 when it is null
     * @param ipVersion expected IP version, {@code ipv4} or {@code ipv6};
     *                  validation is skipped when it is null
     * @param endpoint address, URI, or storage URL to validate
     * @param resourceType resource type used in the error message
     * @param resourceIdentity resource identifier used in the error message
     * @param errorCode global error code used when IP versions mismatch
     * @throws org.zstack.header.apimediator.ApiMessageInterceptionException
     *         when the endpoint uses an unsupported IPv6 link-local address or
     *         a different IP version from {@code ipVersion}
     */
    void validateEndpointMatchesIpVersion(String zoneUuid, String ipVersion, String endpoint,
                                          String resourceType, String resourceIdentity, String errorCode);

    /**
     * Validate that existing zone resources can keep working after the zone
     * management network IP version is changed.
     *
     * @param zoneUuid zone UUID
     * @param ipVersion target IP version, {@code ipv4} or {@code ipv6}
     * @throws org.zstack.header.apimediator.ApiMessageInterceptionException
     *         when an existing host or extension-owned resource has an
     *         incompatible endpoint
     */
    void validateZoneCompatibleWithExistingResources(String zoneUuid, String ipVersion);

    /**
     * Return the zone management network IP version.
     *
     * @param zoneUuid zone UUID
     * @return configured IP version, or {@code ipv4} when no zone tag is set
     */
    String getZoneIpVersion(String zoneUuid);
}
