package org.zstack.kvm.memory;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.errorcode.ErrorFacadeImpl;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.kvm.KVMHostAsyncHttpCallMsg;
import org.zstack.kvm.KVMHostAsyncHttpCallReply;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

/** Exercises the actual read-only Manager handler and its captured CloudBus callback. */
public class MemoryWritebackManagerCallbackTest {
    private static final String HOST = "093d46206e694835b4a218449eb1bc7c";
    private static Object previousLoader;
    private static boolean loaderReplaced;

    @BeforeClass
    public static void initializeErrorCodeFixture() throws Exception {
        Path root = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        while (root != null && !Files.isRegularFile(root.resolve("conf/errorCodes/memory.xml"))) {
            root = root.getParent();
        }
        assertNotNull("product root with memory error metadata not found", root);
        ErrorFacadeImpl errorFacade = new ErrorFacadeImpl();
        javax.xml.bind.JAXBContext context = javax.xml.bind.JAXBContext.newInstance(
                "org.zstack.core.errorcode.schema");
        Object metadata = context.createUnmarshaller().unmarshal(root.resolve("conf/errorCodes/memory.xml").toFile());
        Method register = ErrorFacadeImpl.class.getDeclaredMethod("createErrorCode",
                org.zstack.core.errorcode.schema.Error.class, String.class);
        register.setAccessible(true);
        register.invoke(errorFacade, metadata, root.resolve("conf/errorCodes/memory.xml").toString());
        assertNotNull(errorFacade.instantiateErrorCode("MEMORY_ERROR.11009", "probe"));
        Field loaderField = Platform.class.getDeclaredField("loader");
        loaderField.setAccessible(true);
        previousLoader = loaderField.get(null);
        ComponentLoader loader = (ComponentLoader) Proxy.newProxyInstance(
                ComponentLoader.class.getClassLoader(), new Class<?>[]{ComponentLoader.class},
                (proxy, method, args) -> "getComponent".equals(method.getName()) && args != null
                        && args.length == 1 && args[0] == ErrorFacade.class ? errorFacade : null);
        loaderField.set(null, loader);
        loaderReplaced = true;
    }

    @AfterClass
    public static void restoreErrorCodeFixture() throws Exception {
        if (loaderReplaced) {
            Field loaderField = Platform.class.getDeclaredField("loader");
            loaderField.setAccessible(true);
            loaderField.set(null, previousLoader);
        }
    }

    @Test
    public void managerCallbackProjectsValidInventoryWithoutDispatchingAWrite() throws Exception {
        Capture capture = new Capture();
        invokeWriteback(manager(capture));
        assertEquals(1, capture.sends.get());
        assertEquals("/memory/optimization/writeback-backends", capture.request.get().getPath());
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("hostUuid", HOST);
        state.put("status", "AVAILABLE");
        state.put("candidates", new java.util.ArrayList<>());
        capture.callback.get().run(agentReply(HOST, state));

        assertEquals(0, capture.errors.get());
        assertEquals(1, capture.replies.get());
        assertNotNull(capture.reply.get());
        assertEquals(HOST, capture.reply.get().getInventory().getHostUuid());
        assertNotNull(capture.reply.get().getInventory().getCandidates());
        assertTrue(capture.reply.get().getInventory().getCandidates().isEmpty());
        assertEquals("read callback must not dispatch another operation", 1, capture.sends.get());
    }

    @Test
    public void managerCallbackReturnsErrorForMissingForeignAndMalformedAgentInventory() throws Exception {
        for (int mode = 0; mode < 3; mode++) {
            Capture capture = new Capture();
            invokeWriteback(manager(capture));
            Map<String, Object> state = new LinkedHashMap<>();
            state.put("hostUuid", mode == 1 ? "foreign-host" : HOST);
            state.put("status", "AVAILABLE");
            if (mode == 2) {
                state.put("zramPoolPreparationObservedAt", Double.NaN);
            }
            capture.callback.get().run(agentReply(mode == 0 ? null : HOST, state));
            assertEquals("invalid/missing inventory must return one API error", 1, capture.errors.get());
            assertNotNull(capture.error.get());
            assertEquals("MEMORY_BACKEND_QUERY_UNAVAILABLE", capture.error.get().getCode());
            assertEquals(0, capture.replies.get());
            assertNull(capture.reply.get());
            assertEquals(1, capture.sends.get());
            assertEquals("/memory/optimization/writeback-backends", capture.request.get().getPath());
        }
    }

    private static class Capture {
        final AtomicReference<CloudBusCallBack> callback = new AtomicReference<>();
        final AtomicReference<KVMHostAsyncHttpCallMsg> request = new AtomicReference<>();
        final AtomicReference<APIGetHostMemoryWritebackBackendsReply> reply = new AtomicReference<>();
        final AtomicReference<ErrorCode> error = new AtomicReference<>();
        final AtomicInteger sends = new AtomicInteger();
        final AtomicInteger replies = new AtomicInteger();
        final AtomicInteger errors = new AtomicInteger();
    }

    private static MemoryOptimizationManager manager(Capture capture) throws Exception {
        MemoryOptimizationManager manager = new MemoryOptimizationManager();
        MemoryRepository repository = new MemoryRepository() {
            @Override public java.util.List<String> targets(String scope, String resource) {
                return java.util.Collections.singletonList(resource);
            }
        };
        set(manager, "repository", repository);
        CloudBus bus = (CloudBus) Proxy.newProxyInstance(CloudBus.class.getClassLoader(),
                new Class<?>[]{CloudBus.class}, (proxy, method, args) -> {
                    if ("send".equals(method.getName()) && args != null && args.length == 2
                            && args[0] instanceof KVMHostAsyncHttpCallMsg
                            && args[1] instanceof CloudBusCallBack) {
                        capture.sends.incrementAndGet();
                        capture.request.set((KVMHostAsyncHttpCallMsg) args[0]);
                        capture.callback.set((CloudBusCallBack) args[1]);
                    } else if ("reply".equals(method.getName()) && args != null && args.length == 2
                            && args[1] instanceof APIGetHostMemoryWritebackBackendsReply) {
                        capture.replies.incrementAndGet();
                        capture.reply.set((APIGetHostMemoryWritebackBackendsReply) args[1]);
                    } else if ("replyErrorByMessageType".equals(method.getName())) {
                        capture.errors.incrementAndGet();
                        capture.error.set((ErrorCode) args[1]);
                    }
                    if (method.getReturnType() == boolean.class) { return false; }
                    if (method.getReturnType() == int.class) { return 0; }
                    if (method.getReturnType() == long.class) { return 0L; }
                    return null;
                });
        set(manager, "bus", bus);
        return manager;
    }

    private static KVMHostAsyncHttpCallReply agentReply(String outerHost, Map<String, Object> state) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("hostUuid", outerHost);
        body.put("state", state);
        KVMHostAsyncHttpCallReply reply = new KVMHostAsyncHttpCallReply();
        reply.setSuccess(true);
        reply.setResponse(new LinkedHashMap(body));
        return reply;
    }

    private static void invokeWriteback(MemoryOptimizationManager manager) throws Exception {
        APIGetHostMemoryWritebackBackendsMsg msg = new APIGetHostMemoryWritebackBackendsMsg();
        msg.setHostUuid(HOST);
        Method method = MemoryOptimizationManager.class.getDeclaredMethod("writebackBackends",
                APIGetHostMemoryWritebackBackendsMsg.class);
        method.setAccessible(true);
        method.invoke(manager, msg);
    }

    private static void set(Object target, String name, Object value) throws Exception {
        Field field = MemoryOptimizationManager.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
