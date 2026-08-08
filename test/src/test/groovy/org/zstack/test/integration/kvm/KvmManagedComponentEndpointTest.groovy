package org.zstack.test.integration.kvm

import org.junit.Test
import org.zstack.core.Platform
import org.zstack.core.aspect.AsyncSafeAspect
import org.zstack.core.componentloader.ComponentLoader
import org.zstack.core.errorcode.ErrorFacade
import org.zstack.header.rest.RESTFacade
import org.zstack.kvm.KVMHost

import java.lang.reflect.Field
import java.util.concurrent.atomic.AtomicInteger

class KvmManagedComponentEndpointTest {
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
