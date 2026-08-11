package org.zstack.test.integration.kvm

import org.junit.Test
import org.zstack.core.CoreGlobalProperty
import org.zstack.core.Platform
import org.zstack.core.aspect.AsyncSafeAspect
import org.zstack.core.componentloader.ComponentLoader
import org.zstack.core.errorcode.ErrorFacade
import org.zstack.core.rest.RESTFacadeImpl
import org.zstack.header.rest.RESTFacade
import org.zstack.kvm.KVMHost
import org.zstack.utils.network.IPv6NetworkUtils
import org.zstack.utils.network.NetworkUtils

import java.lang.reflect.Field
import java.net.InetAddress
import java.net.URI
import java.util.concurrent.atomic.AtomicInteger

class KvmManagedComponentEndpointTest {
    @Test
    void zstac87471SelectsSameFamilySendCommandUrlForKvmHost() {
        withManagementServerIpProperties([
                "management.server.ip" : "192.168.1.10",
                "management.server.ip4": "192.168.1.10",
                "management.server.ip6": "2001:db8::10",
        ]) {
            RESTFacade restf = [
                    buildSendCommandUrl: { String host ->
                        RESTFacadeImpl.buildSendCommandUrl(host, 8080, "zstack")
                    }
            ] as RESTFacade

            def ipv6Endpoint = KVMHost.resolveTargetAwareAgentEndpoint(
                    "2001:db8::20", "http://[2001:db8::20]:7070/kvm/connect", restf)
            def ipv4Endpoint = KVMHost.resolveTargetAwareAgentEndpoint(
                    "192.168.1.20", "http://192.168.1.20:7070/kvm/connect", restf)

            assert ipv6Endpoint.success
            assert ipv6Endpoint.result.requestUrl == "http://[2001:db8::20]:7070/kvm/connect"
            assert ipv6Endpoint.result.sendCommandUrl == "http://[2001:db8::10]:8080/zstack/asyncrest/sendcommand"
            assert ipv4Endpoint.success
            assert ipv4Endpoint.result.requestUrl == "http://192.168.1.20:7070/kvm/connect"
            assert ipv4Endpoint.result.sendCommandUrl == "http://192.168.1.10:8080/zstack/asyncrest/sendcommand"

            def dnsEndpoint = KVMHost.resolveTargetAwareAgentEndpoint(
                    "2001:db8::20", "http://kvm.example.com:7070/kvm/connect", restf)

            assert dnsEndpoint.success
            assert dnsEndpoint.result.requestUrl == "http://[2001:db8::20]:7070/kvm/connect"
            assert dnsEndpoint.result.sendCommandUrl == "http://[2001:db8::10]:8080/zstack/asyncrest/sendcommand"
        }
    }

    @Test
    void zstac87471PreservesAutoDetectedManagementIpSendCommandUrl() {
        boolean oldUnitTestOn = CoreGlobalProperty.UNIT_TEST_ON
        try {
            CoreGlobalProperty.UNIT_TEST_ON = false
            withManagementServerIpProperties([:]) {
                RESTFacade restf = [
                        getHostName      : { "2001:db8::10" },
                        getSendCommandUrl: { "http://[2001:db8::10]:8080/zstack/asyncrest/sendcommand" }
                ] as RESTFacade

                withErrorFacade {
                    def endpoint = KVMHost.resolveTargetAwareAgentEndpoint(
                            "2001:db8::20", "http://[2001:db8::20]:7070/kvm/connect", restf)

                    assert endpoint.success
                    assert endpoint.result.requestUrl == "http://[2001:db8::20]:7070/kvm/connect"
                    assert endpoint.result.sendCommandUrl == "http://[2001:db8::10]:8080/zstack/asyncrest/sendcommand"
                }
            }
        } finally {
            CoreGlobalProperty.UNIT_TEST_ON = oldUnitTestOn
        }
    }

    @Test
    void zstac87471PinsHostnameHostToAutoDetectedManagementIpFamily() {
        boolean oldUnitTestOn = CoreGlobalProperty.UNIT_TEST_ON
        try {
            CoreGlobalProperty.UNIT_TEST_ON = false
            withManagementServerIpProperties([:]) {
                String managementNodeAddress = InetAddress.getAllByName("localhost")[0].hostAddress
                String sendCommandUrl = RESTFacadeImpl.buildSendCommandUrl(
                        managementNodeAddress, 8080, "zstack")
                RESTFacade restf = [
                        getHostName      : { managementNodeAddress },
                        getSendCommandUrl: { sendCommandUrl }
                ] as RESTFacade

                withErrorFacade {
                    def endpoint = KVMHost.resolveTargetAwareAgentEndpoint(
                            "localhost", "http://localhost:7070/kvm/connect", restf)

                    assert endpoint.success
                    URI requestUri = new URI(endpoint.result.requestUrl)
                    String requestHost = IPv6NetworkUtils.stripHostUrlBrackets(requestUri.host)
                    assert requestHost != "localhost"
                    assert NetworkUtils.isIpv4Address(managementNodeAddress) == NetworkUtils.isIpv4Address(requestHost)
                    assert IPv6NetworkUtils.isIpv6Address(managementNodeAddress) ==
                            IPv6NetworkUtils.isIpv6Address(requestHost)
                    assert requestUri.port == 7070
                    assert requestUri.path == "/kvm/connect"
                    assert endpoint.result.sendCommandUrl == sendCommandUrl
                }
            }
        } finally {
            CoreGlobalProperty.UNIT_TEST_ON = oldUnitTestOn
        }
    }

    @Test
    void zstac87471PreservesCustomRestFacadeHostnameFallback() {
        boolean oldUnitTestOn = CoreGlobalProperty.UNIT_TEST_ON
        try {
            CoreGlobalProperty.UNIT_TEST_ON = false
            withManagementServerIpProperties([:]) {
                String requestUrl = "http://[2001:db8::20]:7070/kvm/connect"
                String sendCommandUrl = "http://mn.example.com:8080/zstack/asyncrest/sendcommand"
                RESTFacade restf = [
                        getHostName      : { "mn.example.com" },
                        getSendCommandUrl: { sendCommandUrl }
                ] as RESTFacade

                withErrorFacade {
                    def endpoint = KVMHost.resolveTargetAwareAgentEndpoint(
                            "2001:db8::20", requestUrl, restf)

                    assert endpoint.success
                    assert endpoint.result.requestUrl == requestUrl
                    assert endpoint.result.sendCommandUrl == sendCommandUrl
                }
            }
        } finally {
            CoreGlobalProperty.UNIT_TEST_ON = oldUnitTestOn
        }
    }

    @Test
    void zstac87471PreservesHttpsHostNameForTlsIdentity() {
        withManagementServerIpProperties([
                "management.server.ip" : "192.168.1.10",
                "management.server.ip4": "192.168.1.10",
                "management.server.ip6": "2001:db8::10",
        ]) {
            RESTFacade restf = [
                    buildSendCommandUrl: { String host ->
                        RESTFacadeImpl.buildSendCommandUrl(host, 8080, "zstack")
                    }
            ] as RESTFacade
            String requestUrl = "https://kvm.example.com:7070/kvm/connect"

            def endpoint = KVMHost.resolveTargetAwareAgentEndpoint(
                    "2001:db8::20", requestUrl, restf)

            assert endpoint.success
            assert endpoint.result.requestUrl == requestUrl
            assert endpoint.result.sendCommandUrl == "http://[2001:db8::10]:8080/zstack/asyncrest/sendcommand"
        }
    }

    @Test
    void missingSameFamilyAddressFailsBeforeBuildingTheCallbackCommand() {
        withManagementServerIpProperties([
                "management.server.ip" : "192.168.1.10",
                "management.server.ip4": "192.168.1.10",
        ]) {
            AtomicInteger callbackCalls = new AtomicInteger()
            RESTFacade restf = [
                    buildCallbackUrl: { String ignored ->
                        callbackCalls.incrementAndGet()
                        "http://unexpected"
                    }
            ] as RESTFacade

            withErrorFacade {
                def command = KVMHost.buildManagementNodeCallbackCheckCommand(
                        "2001:db8::20", restf)

                assert !command.success
                assert command.error.globalErrorCode == "ORG_ZSTACK_CORE_PLATFORM_10001"
                assert callbackCalls.get() == 0
            }
        }
    }

    private static void withManagementServerIpProperties(Map<String, String> properties,
                                                          Closure closure) {
        List<String> managedKeys = [
                "management.server.ip",
                "management.server.ip4",
                "management.server.ip6",
        ]
        Map<String, String> oldValues = [:]
        managedKeys.each { key -> oldValues[key] = System.getProperty(key) }

        try {
            managedKeys.each { key -> System.clearProperty(key) }
            properties.each { key, value -> System.setProperty(key, value) }
            closure.call()
        } finally {
            managedKeys.each { key ->
                if (oldValues[key] == null) {
                    System.clearProperty(key)
                } else {
                    System.setProperty(key, oldValues[key])
                }
            }
        }
    }

    private static void withErrorFacade(Closure closure) {
        ErrorFacade errorFacade = [
                instantiateErrorCode : { Enum code, String details,
                                         org.zstack.header.errorcode.ErrorCode cause ->
                    new org.zstack.header.errorcode.ErrorCode(code.name(), "test", details)
                },
                stringToInternalError: { String details ->
                    new org.zstack.header.errorcode.ErrorCode("TEST", "test", details)
                }
        ] as ErrorFacade

        Field loaderField = Platform.class.getDeclaredField("loader")
        loaderField.setAccessible(true)
        Object oldLoader = loaderField.get(null)
        AsyncSafeAspect aspect = AsyncSafeAspect.aspectOf()
        Field errorFacadeField = AsyncSafeAspect.class.getDeclaredField("errf")
        errorFacadeField.setAccessible(true)
        ErrorFacade oldErrorFacade = errorFacadeField.get(aspect) as ErrorFacade

        try {
            loaderField.set(null, [
                    getComponent: { Class componentClass ->
                        componentClass == ErrorFacade.class ? errorFacade : null
                    }
            ] as ComponentLoader)
            errorFacadeField.set(aspect, errorFacade)
            closure.call()
        } finally {
            errorFacadeField.set(aspect, oldErrorFacade)
            loaderField.set(null, oldLoader)
        }
    }
}
