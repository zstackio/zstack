package org.zstack.test.integration.core

import org.junit.Test
import org.zstack.core.ManagementEndpointData
import org.zstack.core.Platform
import org.zstack.header.exception.CloudRuntimeException
import org.zstack.utils.zsha2.ZSha2Info

import java.lang.reflect.Field

class PlatformManagementEndpointSelectionCase {
    private static final String IPV4 = "192.168.1.10"
    private static final String IPV6 = "2001:db8::1"

    @Test
    void testHaDefaultUsesTheConfiguredNodeFamily() {
        ZSha2Info info = new ZSha2Info()
        info.setIpv6(new ZSha2Info.HaAddressFamily(
                nodeIp: "2001:db8::11", peerIp: "2001:db8::12", virtualIp: "2001:db8::100", enabled: true))

        ManagementEndpointData endpoints = new ManagementEndpointData([IPV4, IPV6], info)

        assert endpoints.getDefaultEndpoint(ManagementEndpointData.EndpointType.VIP) == null
        assert endpoints.getDefaultEndpoint(ManagementEndpointData.EndpointType.CANONICAL_NODE) == null
    }

    @Test
    void testInvalidConfiguredDefaultDoesNotFallBackToRoute() {
        Platform.getManagementServerIp()
        String oldManagementIp = System.getProperty("management.server.ip")
        try {
            System.setProperty("management.server.ip", "not-an-ip")
            resetCachedManagementServerIp()

            try {
                Platform.getManagementServerIp()
                assert false
            } catch (CloudRuntimeException ignored) {
            }
        } finally {
            if (oldManagementIp == null) {
                System.clearProperty("management.server.ip")
            } else {
                System.setProperty("management.server.ip", oldManagementIp)
            }
            resetCachedManagementServerIp()
        }
    }

    private static void resetCachedManagementServerIp() {
        Field field = Platform.class.getDeclaredField("managementServerIp")
        field.setAccessible(true)
        field.set(null, null)
    }
}
