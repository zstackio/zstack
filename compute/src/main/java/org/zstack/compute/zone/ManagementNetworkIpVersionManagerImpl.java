package org.zstack.compute.zone;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.tag.SystemTagInventory;
import org.zstack.header.zone.ManagementNetworkIpVersionManager;
import org.zstack.header.zone.ManagementNetworkIpVersionResourceExtensionPoint;
import org.zstack.header.zone.ZoneVO;
import org.zstack.utils.network.ManagementNetworkIpVersionUtils;

import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.argerr;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_COMPUTE_ZONE_10004;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_COMPUTE_ZONE_10005;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_COMPUTE_ZONE_10006;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.ORG_ZSTACK_COMPUTE_ZONE_10007;

public class ManagementNetworkIpVersionManagerImpl implements ManagementNetworkIpVersionManager {
    private static final String DEFAULT_ZONE_IP_VERSION = ManagementNetworkIpVersionUtils.IPV4;
    private static final String HOST_RESOURCE_TYPE = "host";
    private static final String MISMATCH_ERROR =
            "%s[%s] uses %s endpoint[%s], but zone[uuid:%s] management network ipVersion is %s";

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;

    @Override
    public void validateEndpointInZone(String zoneUuid, String endpoint, String resourceType, String resourceIdentity, String errorCode) {
        if (zoneUuid == null || endpoint == null) {
            return;
        }

        validateEndpointMatchesIpVersion(zoneUuid, getZoneIpVersion(zoneUuid), endpoint, resourceType, resourceIdentity, errorCode);
    }

    @Override
    public void validateEndpointMatchesIpVersion(String zoneUuid, String ipVersion, String endpoint,
                                                 String resourceType, String resourceIdentity, String errorCode) {
        if (zoneUuid == null || ipVersion == null || endpoint == null) {
            return;
        }

        if (ManagementNetworkIpVersionUtils.isIpv6LinkLocalEndpoint(endpoint)) {
            throw new ApiMessageInterceptionException(argerr(
                    ORG_ZSTACK_COMPUTE_ZONE_10007,
                    "%s[%s] uses IPv6 link-local endpoint[%s], which is not supported for zone[uuid:%s] management network",
                    resourceType, resourceIdentity, endpoint, zoneUuid));
        }

        String endpointIpVersion = ManagementNetworkIpVersionUtils.getEndpointIpVersion(endpoint);
        if (endpointIpVersion == null || endpointIpVersion.equals(ipVersion)) {
            return;
        }

        throw new ApiMessageInterceptionException(argerr(errorCode, MISMATCH_ERROR,
                resourceType, resourceIdentity, endpointIpVersion, endpoint, zoneUuid, ipVersion));
    }

    @Override
    public String getZoneIpVersion(String zoneUuid) {
        Map<String, String> tokens = ZoneSystemTags.MANAGEMENT_NETWORK_IP_VERSION.getTokensByResourceUuid(zoneUuid);
        if (tokens == null) {
            return DEFAULT_ZONE_IP_VERSION;
        }

        return ManagementNetworkIpVersionUtils.normalizeIpVersion(
                tokens.get(ZoneSystemTags.MANAGEMENT_NETWORK_IP_VERSION_TOKEN));
    }

    @Override
    public void validateZoneCompatibleWithExistingResources(String zoneUuid, String ipVersion) {
        validateExistingHosts(zoneUuid, ipVersion);
        for (ManagementNetworkIpVersionResourceExtensionPoint ext :
                pluginRgty.getExtensionList(ManagementNetworkIpVersionResourceExtensionPoint.class)) {
            ext.validateExistingResourcesInZone(zoneUuid, ipVersion);
        }
    }

    public void validateZoneIpVersionTag(String resourceUuid, String systemTag) {
        validateZoneIpVersionTag(resourceUuid, systemTag, null);
    }

    public void validateZoneIpVersionTag(String resourceUuid, String systemTag, String excludedTagUuid) {
        validateZoneIpVersionTagValue(resourceUuid, systemTag);
        validateZoneHasNoOtherIpVersionTag(resourceUuid, excludedTagUuid);
    }

    public void validateZoneIpVersionTagValue(String resourceUuid, String systemTag) {
        if (!ZoneSystemTags.MANAGEMENT_NETWORK_IP_VERSION.isMatch(systemTag)) {
            return;
        }

        if (!dbf.isExist(resourceUuid, ZoneVO.class)) {
            return;
        }

        String ipVersion = ZoneSystemTags.MANAGEMENT_NETWORK_IP_VERSION.getTokenByTag(
                systemTag,
                ZoneSystemTags.MANAGEMENT_NETWORK_IP_VERSION_TOKEN);
        String normalizedIpVersion = ManagementNetworkIpVersionUtils.normalizeIpVersion(ipVersion);
        if (normalizedIpVersion == null) {
            throw new ApiMessageInterceptionException(argerr(
                    ORG_ZSTACK_COMPUTE_ZONE_10004,
                    "management network ipVersion[%s] is invalid, allowed values are [%s, %s]",
                    ipVersion,
                    ManagementNetworkIpVersionUtils.IPV4,
                    ManagementNetworkIpVersionUtils.IPV6));
        }

        validateZoneCompatibleWithExistingResources(resourceUuid, normalizedIpVersion);
    }

    private void validateZoneHasNoOtherIpVersionTag(String zoneUuid, String excludedTagUuid) {
        for (SystemTagInventory tag : ZoneSystemTags.MANAGEMENT_NETWORK_IP_VERSION.getTagInventories(zoneUuid)) {
            if (excludedTagUuid != null && excludedTagUuid.equals(tag.getUuid())) {
                continue;
            }

            throw new ApiMessageInterceptionException(argerr(
                    ORG_ZSTACK_COMPUTE_ZONE_10006,
                    "zone[uuid:%s] already has management network ipVersion system tag[%s], update or delete it before creating another one",
                    zoneUuid, tag.getTag()));
        }
    }

    private void validateExistingHosts(String zoneUuid, String ipVersion) {
        List<HostVO> hosts = Q.New(HostVO.class)
                .eq(HostVO_.zoneUuid, zoneUuid)
                .list();

        for (HostVO host : hosts) {
            validateEndpointMatchesIpVersion(zoneUuid, ipVersion, host.getManagementIp(),
                    HOST_RESOURCE_TYPE, host.getUuid(), ORG_ZSTACK_COMPUTE_ZONE_10005);
        }
    }
}
