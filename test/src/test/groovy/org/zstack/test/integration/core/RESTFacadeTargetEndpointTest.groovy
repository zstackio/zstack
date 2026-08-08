package org.zstack.test.integration.core

import org.junit.Test
import org.zstack.core.CoreGlobalProperty
import org.zstack.core.Platform
import org.zstack.core.rest.RESTFacadeImpl

import java.lang.reflect.Method

class RESTFacadeTargetEndpointTest {
    @Test
    void testRequestAndCallbackUseOneResolvedEndpointCandidate() {
        boolean oldUnitTestOn = CoreGlobalProperty.UNIT_TEST_ON
        String oldIpv4 = System.getProperty("management.server.ip4")
        String oldIpv6 = System.getProperty("management.server.ip6")
        try {
            CoreGlobalProperty.UNIT_TEST_ON = false
            System.setProperty("management.server.ip4", "192.168.1.10")
            System.setProperty("management.server.ip6", "2001:db8::10")

            def resolved = Platform.resolveManagedComponentEndpoints(
                    "controller.example.com", ["2001:db8::20", "192.168.1.20"])
            def selected = resolved.result.first()

            Method replaceRequestHost = RESTFacadeImpl.getDeclaredMethod(
                    "replaceRequestHost", String.class, String.class)
            replaceRequestHost.accessible = true
            String requestUrl = replaceRequestHost.invoke(null,
                    "http://controller.example.com:7070/host/ping", selected.remoteAddress)
            String callbackUrl = RESTFacadeImpl.buildCallbackUrl(
                    selected.currentManagementNodeAddress, 8080, "zstack")

            assert requestUrl == "http://[2001:db8::20]:7070/host/ping"
            assert callbackUrl == "http://[2001:db8::10]:8080/zstack/asyncrest/callback"
        } finally {
            CoreGlobalProperty.UNIT_TEST_ON = oldUnitTestOn
            restoreProperty("management.server.ip4", oldIpv4)
            restoreProperty("management.server.ip6", oldIpv6)
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
