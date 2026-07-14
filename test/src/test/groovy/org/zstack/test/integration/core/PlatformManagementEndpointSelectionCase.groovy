package org.zstack.test.integration.core

import org.junit.Test
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.zstack.core.ManagementEndpointData
import org.zstack.core.Platform
import org.zstack.core.aspect.AsyncSafeAspect
import org.zstack.core.componentloader.ComponentLoader
import org.zstack.core.errorcode.ErrorFacade
import org.zstack.core.rest.RESTFacadeImpl
import org.zstack.header.rest.AsyncRESTCallback
import org.zstack.header.exception.CloudRuntimeException
import org.zstack.utils.zsha2.ZSha2Info

import java.lang.reflect.Field
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

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

    @Test
    void testPrimaryAddressWinsOverSameFamilySecondaryProperty() {
        withManagementServerIpProperties([
                "management.server.ip" : IPV4,
                "management.server.ip4": "192.168.1.11",
                "management.server.ip6": IPV6,
        ]) {
            assert Platform.getManagementServerIp("192.168.1.20").result == IPV4
        }

        withManagementServerIpProperties([
                "management.server.ip" : IPV6,
                "management.server.ip4": IPV4,
                "management.server.ip6": "2001:db8::11",
        ]) {
            assert Platform.getManagementServerIp("2001:db8::20").result == IPV6
        }
    }

    @Test
    void testTargetAwareGettersPreserveEndpointKindAndReturnTypedErrors() {
        withErrorFacade {
            withManagementServerIpProperties([
                    "management.server.ip" : IPV4,
                    "management.server.ip6": IPV6,
            ]) {
                assert Platform.getManagementServerIp("2001:db8::20").result == IPV6
                assert Platform.getCanonicalServerIp("2001:db8::20").result == IPV6
                assert Platform.getManagementServerVip("2001:db8::20").result == IPV6

                assert Platform.getManagementServerIp("management.example.com").error.globalErrorCode == "ORG_ZSTACK_CORE_PLATFORM_10000"
                assert Platform.getCanonicalServerIp("management.example.com").error.globalErrorCode == "ORG_ZSTACK_CORE_PLATFORM_10000"
                assert Platform.getManagementServerVip("management.example.com").error.globalErrorCode == "ORG_ZSTACK_CORE_PLATFORM_10000"
            }

            withManagementServerIpProperties([
                    "management.server.ip": IPV4,
            ]) {
                assert Platform.getManagementServerIp("2001:db8::20").error.globalErrorCode == "ORG_ZSTACK_CORE_PLATFORM_10001"
                assert Platform.getCanonicalServerIp("2001:db8::20").error.globalErrorCode == "ORG_ZSTACK_CORE_PLATFORM_10001"
                assert Platform.getManagementServerVip("2001:db8::20").error.globalErrorCode == "ORG_ZSTACK_CORE_PLATFORM_10001"
            }
        }
    }

    @Test
    void testAsyncRestCallbackSelectionFailsInsteadOfUsingAnotherFamily() {
        withErrorFacade {
            withManagementServerIpProperties([
                    "management.server.ip": IPV4,
            ]) {
                def callback = RESTFacadeImpl.selectCallbackUrl(
                        "http://[2001:db8::20]:7070/host/ping", [:], "http://${IPV4}:8080/zstack/asyncrest/callback", 8080, "zstack")

                assert !callback.success
                assert callback.error.globalErrorCode == "ORG_ZSTACK_CORE_PLATFORM_10001"
            }
        }
    }

    @Test
    void testAsyncRestCallbackSelectionFailsBeforeInterceptors() {
        withErrorFacade {
            withManagementServerIpProperties([
                    "management.server.ip": IPV4,
            ]) {
                AtomicInteger interceptorCalls = new AtomicInteger()
                AtomicReference error = new AtomicReference()
                RESTFacadeImpl restf = new RESTFacadeImpl()
                restf.installBeforeAsyncJsonPostInterceptor([
                        beforeAsyncJsonPost: { Object... ignored -> interceptorCalls.incrementAndGet() },
                ] as org.zstack.header.rest.BeforeAsyncJsonPostInterceptor)

                restf.asyncJson("http://[2001:db8::20]:7070/host/ping", "{}", [:], HttpMethod.POST,
                        new AsyncRESTCallback(null) {
                            @Override
                            void fail(org.zstack.header.errorcode.ErrorCode errorCode) {
                                error.set(errorCode)
                            }

                            @Override
                            void success(HttpEntity<String> responseEntity) {
                            }
                        }, TimeUnit.SECONDS, 10)

                assert interceptorCalls.get() == 0
                assert error.get().globalErrorCode == "ORG_ZSTACK_CORE_PLATFORM_10001"
            }
        }
    }

    private static void withManagementServerIpProperties(Map<String, String> properties, Closure closure) {
        List<String> keys = [
                "management.server.ip",
                "management.server.ip4",
                "management.server.ip6",
        ]
        Map<String, String> oldValues = [:]
        keys.each { key -> oldValues[key] = System.getProperty(key) }

        try {
            keys.each { key -> System.clearProperty(key) }
            properties.each { key, value -> System.setProperty(key, value) }
            closure.call()
        } finally {
            keys.each { key ->
                if (oldValues[key] == null) {
                    System.clearProperty(key)
                } else {
                    System.setProperty(key, oldValues[key])
                }
            }
        }
    }

    private static void resetCachedManagementServerIp() {
        Field field = Platform.class.getDeclaredField("managementServerIp")
        field.setAccessible(true)
        field.set(null, null)
    }

    private static void withErrorFacade(Closure closure) {
        ErrorFacade errorFacade = [
                instantiateErrorCode : { Enum code, String details, org.zstack.header.errorcode.ErrorCode cause ->
                    new org.zstack.header.errorcode.ErrorCode(code.name(), "test", details)
                },
                stringToInternalError: { String details -> new org.zstack.header.errorcode.ErrorCode("TEST", "test", details) }
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
                    getComponent: { Class componentClass -> componentClass == ErrorFacade.class ? errorFacade : null }
            ] as ComponentLoader)
            errorFacadeField.set(aspect, errorFacade)
            closure.call()
        } finally {
            errorFacadeField.set(aspect, oldErrorFacade)
            loaderField.set(null, oldLoader)
        }
    }
}
