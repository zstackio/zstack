package org.zstack.test.integration.core

import org.junit.Test
import org.zstack.core.CoreGlobalProperty
import org.zstack.core.Platform
import org.zstack.core.rest.RESTFacadeImpl
import org.zstack.core.thread.Task
import org.zstack.core.thread.ThreadFacade
import org.springframework.http.HttpMethod

import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class RESTFacadeTargetEndpointTest {
    @Test
    void testAsyncJsonDefersEndpointResolutionToWorkerThread() {
        boolean oldUnitTestOn = CoreGlobalProperty.UNIT_TEST_ON
        try {
            CoreGlobalProperty.UNIT_TEST_ON = false
            AtomicReference<Task<Void>> submittedTask = new AtomicReference<>()
            ThreadFacade threadFacade = [
                    submit: { Task<Void> task ->
                        submittedTask.set(task)
                        return CompletableFuture.completedFuture(null)
                    }
            ] as ThreadFacade
            RESTFacadeImpl restFacade = new RESTFacadeImpl()
            setField(restFacade, "thdf", threadFacade)

            restFacade.asyncJson("http://localhost:7070/host/ping", "{}", null,
                    HttpMethod.POST, null, TimeUnit.SECONDS, 30)

            assert submittedTask.get() != null
            assert submittedTask.get().name.contains("localhost")
        } finally {
            CoreGlobalProperty.UNIT_TEST_ON = oldUnitTestOn
        }
    }

    @Test
    void testRequestAndCallbackUseOneResolvedEndpointCandidate() {
        boolean oldUnitTestOn = CoreGlobalProperty.UNIT_TEST_ON
        String oldIpv4 = System.getProperty("management.server.ip4")
        String oldIpv6 = System.getProperty("management.server.ip6")
        try {
            CoreGlobalProperty.UNIT_TEST_ON = false
            System.setProperty("management.server.ip4", "192.168.1.10")
            System.setProperty("management.server.ip6", "2001:db8::10")
            resetCachedManagementServerIp()

            def resolved = Platform.resolveManagedComponentEndpointCandidates(
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

            String encodedUrl = replaceRequestHost.invoke(null,
                    "http://controller.example.com:7070/a%2Fb?q=x%26y#part%2Fone",
                    selected.remoteAddress)
            assert encodedUrl == "http://[2001:db8::20]:7070/a%2Fb?q=x%26y#part%2Fone"

            Method buildRequestHostHeader = RESTFacadeImpl.getDeclaredMethod(
                    "buildRequestHostHeader", String.class)
            buildRequestHostHeader.accessible = true
            assert buildRequestHostHeader.invoke(null,
                    "http://controller.example.com:7070/host/ping") ==
                    "controller.example.com:7070"
        } finally {
            CoreGlobalProperty.UNIT_TEST_ON = oldUnitTestOn
            restoreProperty("management.server.ip4", oldIpv4)
            restoreProperty("management.server.ip6", oldIpv6)
            resetCachedManagementServerIp()
        }
    }

    private static void resetCachedManagementServerIp() {
        Field field = Platform.class.getDeclaredField("managementServerIp")
        field.setAccessible(true)
        field.set(null, null)
    }

    private static void setField(Object target, String name, Object value) {
        Field field = target.class.getDeclaredField(name)
        field.setAccessible(true)
        field.set(target, value)
    }

    private static void restoreProperty(String name, String value) {
        if (value == null) {
            System.clearProperty(name)
        } else {
            System.setProperty(name, value)
        }
    }
}
