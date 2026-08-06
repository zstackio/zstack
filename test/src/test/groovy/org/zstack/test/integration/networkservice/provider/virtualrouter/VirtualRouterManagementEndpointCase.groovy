package org.zstack.test.integration.networkservice.provider.virtualrouter

import org.junit.Test
import org.zstack.core.Platform
import org.zstack.appliancevm.ApplianceVmFacadeImpl
import org.zstack.header.rest.RESTConstant
import org.zstack.header.rest.RESTFacade
import org.zstack.network.service.virtualrouter.VirtualRouterManagerImpl

import java.lang.reflect.Field

class VirtualRouterManagementEndpointCase {
    @Test
    void testBootstrapFallbackKeepsTheVirtualRouterAddressFamily() {
        assert ApplianceVmFacadeImpl.selectManagementNodeIpForBootstrap(
                ["192.168.1.10", "2001:db8::10"],
                ["2001:db9::/64"],
                "192.168.1.10") == "2001:db8::10"

        assert ApplianceVmFacadeImpl.selectManagementNodeIpForBootstrap(
                ["192.168.1.10"],
                ["2001:db9::/64"],
                "192.168.1.10") == null
    }

    @Test
    void testCallbackUsesConfiguredCurrentNodeAddressInsteadOfRouteSource() {
        String oldPrimary = System.getProperty("management.server.ip")
        String oldIpv4 = System.getProperty("management.server.ip4")
        try {
            System.setProperty("management.server.ip", "192.168.1.10")
            System.clearProperty("management.server.ip4")

            VirtualRouterManagerImpl manager = new VirtualRouterManagerImpl()
            Field restf = VirtualRouterManagerImpl.getDeclaredField("restf")
            restf.accessible = true
            restf.set(manager, [
                    buildCallbackUrl: { String address -> String.format("http://%s:8080/callback", address) },
            ] as RESTFacade)

            Map<String, String> headers = manager.buildAgentCallbackUrlHeaders("127.0.0.1")

            assert headers[RESTConstant.CALLBACK_URL] == "http://192.168.1.10:8080/callback"
        } finally {
            restoreProperty("management.server.ip", oldPrimary)
            restoreProperty("management.server.ip4", oldIpv4)
        }
    }

    private static void restoreProperty(String name, String value) {
        if (value == null) {
            System.clearProperty(name)
        } else {
            System.setProperty(name, value)
        }
    }
}
