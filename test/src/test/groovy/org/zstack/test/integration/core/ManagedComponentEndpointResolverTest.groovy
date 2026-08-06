package org.zstack.test.integration.core

import org.junit.Test
import org.zstack.core.ManagedComponentEndpointResolver
import org.zstack.core.ManagementNodeAddressInventory
import org.zstack.core.Platform
import org.zstack.core.aspect.AsyncSafeAspect
import org.zstack.core.componentloader.ComponentLoader
import org.zstack.core.errorcode.ErrorFacade

import java.lang.reflect.Field
import java.net.UnknownHostException
import java.util.concurrent.atomic.AtomicInteger

class ManagedComponentEndpointResolverTest {
    @Test
    void testLiteralAndResolvedAddressesUseTheSameFamilyCurrentNode() {
        ManagementNodeAddressInventory inventory = new ManagementNodeAddressInventory([
                "192.168.1.10", "2001:db8::10"])
        ManagedComponentEndpointResolver resolver = new ManagedComponentEndpointResolver(inventory)

        def literal = resolver.resolve("2001:db8::20")
        assert literal.success
        assert literal.result*.remoteAddress == ["2001:db8::20"]
        assert literal.result*.currentManagementNodeAddress == ["2001:db8::10"]

        def hostname = resolver.resolve("controller.example.com", [
                "2001:db8::20", "192.168.1.20", "2001:db8::20"])
        assert hostname.success
        assert hostname.result*.remoteAddress == ["2001:db8::20", "192.168.1.20"]
        assert hostname.result*.currentManagementNodeAddress == ["2001:db8::10", "192.168.1.10"]
    }

    @Test
    void testHostnameIsResolvedOnlyOncePerOperation() {
        AtomicInteger calls = new AtomicInteger()
        ManagementNodeAddressInventory inventory = new ManagementNodeAddressInventory([
                "192.168.1.10", "2001:db8::10"])
        ManagedComponentEndpointResolver resolver = new ManagedComponentEndpointResolver(inventory,
                { String ignored ->
                    calls.incrementAndGet()
                    ["192.168.1.20", "2001:db8::20"]
                } as ManagedComponentEndpointResolver.HostAddressResolver)

        def endpoints = resolver.resolve("controller.example.com")

        assert endpoints.success
        assert calls.get() == 1
        assert endpoints.result*.remoteAddress == ["192.168.1.20", "2001:db8::20"]
    }

    @Test
    void testMissingSameFamilyAddressReturnsTypedError() {
        withErrorFacade {
            ManagementNodeAddressInventory inventory = new ManagementNodeAddressInventory(["192.168.1.10"])
            ManagedComponentEndpointResolver resolver = new ManagedComponentEndpointResolver(inventory)

            def endpoints = resolver.resolve("controller.example.com", ["2001:db8::20"])

            assert !endpoints.success
            assert endpoints.error.globalErrorCode == "ORG_ZSTACK_CORE_PLATFORM_10001"
            assert endpoints.error.formatArgs == ["controller.example.com", "IPv6", "false"]
        }
    }

    @Test
    void testUnknownHostnameReturnsTypedError() {
        withErrorFacade {
            ManagementNodeAddressInventory inventory = new ManagementNodeAddressInventory(["192.168.1.10"])
            ManagedComponentEndpointResolver resolver = new ManagedComponentEndpointResolver(inventory,
                    { String host -> throw new UnknownHostException(host) }
                            as ManagedComponentEndpointResolver.HostAddressResolver)

            def endpoints = resolver.resolve("missing.example.com")

            assert !endpoints.success
            assert endpoints.error.globalErrorCode == "ORG_ZSTACK_CORE_PLATFORM_10004"
        }
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
