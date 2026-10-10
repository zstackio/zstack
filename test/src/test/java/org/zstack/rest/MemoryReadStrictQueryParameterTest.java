package org.zstack.rest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.rest.RestAuthenticationBackend;
import org.zstack.header.rest.RestAuthenticationParams;
import org.zstack.header.rest.RestAuthenticationType;
import org.zstack.header.rest.RestException;
import org.zstack.kvm.memory.APIQueryHostMemoryOperationsMsg;
import org.zstack.kvm.memory.APIQueryMemoryStateMsg;
import org.zstack.kvm.memory.APIQueryMemoryTaskMsg;
import org.zstack.kvm.memory.APIGetVmMemoryOptimizationMsg;
import org.zstack.kvm.memory.APIGetMemoryStatesMsg;
import org.zstack.kvm.memory.APIGetMemoryTasksMsg;
import org.zstack.kvm.memory.APIGetHostMemoryOperationsMsg;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.MessageReply;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class MemoryReadStrictQueryParameterTest {
    private Object oldCheckHttpMethod;

    @Before
    public void setup() throws Exception {
        oldCheckHttpMethod = RestGlobalConfig.CHECK_HTTP_METHOD;
        RestGlobalConfig.CHECK_HTTP_METHOD = mock(org.zstack.core.config.GlobalConfig.class);
        when(((org.zstack.core.config.GlobalConfig) RestGlobalConfig.CHECK_HTTP_METHOD).value(Boolean.class)).thenReturn(false);
    }

    @After
    public void cleanup() {
        RestGlobalConfig.CHECK_HTTP_METHOD = (org.zstack.core.config.GlobalConfig) oldCheckHttpMethod;
    }

    @Test
    public void rejectsGenericQuerySyntaxOnMemoryReadRoutesThroughRestDispatch() throws Exception {
        Class<?>[] apis = {APIQueryMemoryStateMsg.class, APIGetMemoryStatesMsg.class,
                APIQueryMemoryTaskMsg.class, APIGetMemoryTasksMsg.class,
                APIQueryHostMemoryOperationsMsg.class, APIGetHostMemoryOperationsMsg.class};
        String[] unsupported = {"q", "fields", "count", "sortBy", "sortDirection", "notAField", "q.conditions", "hostUuids.key"};

        for (Class<?> apiClass : apis) {
            for (String parameter : unsupported) {
                Map<String, String[]> params = new HashMap<>();
                params.put(parameter, new String[]{"anything"});
                assertTrue(apiClass.getSimpleName() + " parameter=" + parameter + " must fail before bus dispatch",
                        dispatch(apiClass, params) == null);
            }
        }
    }

    @Test
    public void strictQueryModeDoesNotChangeOtherGetRoutes() throws Exception {
        Map<String, String[]> params = new HashMap<>();
        params.put("notAField", new String[]{"legacy-ignored"});
        assertNotNull("non-opt-in GET route must keep legacy unknown-parameter handling",
                dispatch(APIGetVmMemoryOptimizationMsg.class, params));
    }

    @Test
    public void getAliasesBindInheritedFiltersAndLegacyRoutesRemainCallable() throws Exception {
        Map<String, String[]> stateParams = new HashMap<>();
        stateParams.put("hostUuids.0", new String[]{"host-a"});
        stateParams.put("start", new String[]{"5"});
        stateParams.put("limit", new String[]{"20"});
        stateParams.put("snapshotId", new String[]{"snap-1"});
        APIMessage state = dispatch(APIGetMemoryStatesMsg.class, stateParams);
        assertTrue(state instanceof APIGetMemoryStatesMsg);
        assertEquals(Collections.singletonList("host-a"), ((APIQueryMemoryStateMsg) state).getHostUuids());
        assertEquals(5, ((APIQueryMemoryStateMsg) state).getStart());
        assertEquals(20, ((APIQueryMemoryStateMsg) state).getLimit());
        assertEquals("snap-1", ((APIQueryMemoryStateMsg) state).getSnapshotId());

        Map<String, String[]> taskParams = new HashMap<>();
        taskParams.put("uuid", new String[]{"task-a"});
        taskParams.put("status", new String[]{"Succeeded"});
        taskParams.put("snapshotId", new String[]{"snap-2"});
        APIMessage task = dispatch(APIGetMemoryTasksMsg.class, taskParams);
        assertTrue(task instanceof APIGetMemoryTasksMsg);
        assertEquals("task-a", ((APIQueryMemoryTaskMsg) task).getUuid());
        assertEquals("Succeeded", ((APIQueryMemoryTaskMsg) task).getStatus());
        assertEquals("snap-2", ((APIQueryMemoryTaskMsg) task).getSnapshotId());

        Map<String, String[]> operationParams = new HashMap<>();
        operationParams.put("operationId", new String[]{"op-a"});
        APIMessage operation = dispatch(APIGetHostMemoryOperationsMsg.class, operationParams);
        assertTrue(operation instanceof APIGetHostMemoryOperationsMsg);
        assertEquals("resource-test", ((APIQueryHostMemoryOperationsMsg) operation).getHostUuid());
        assertEquals("op-a", ((APIQueryHostMemoryOperationsMsg) operation).getOperationId());

        APIMessage legacy = dispatch(APIQueryMemoryStateMsg.class, stateParams);
        assertTrue(legacy instanceof APIQueryMemoryStateMsg);
        assertEquals(((APIQueryMemoryStateMsg) state).getHostUuids(), ((APIQueryMemoryStateMsg) legacy).getHostUuids());
        assertEquals(((APIQueryMemoryStateMsg) state).getSnapshotId(), ((APIQueryMemoryStateMsg) legacy).getSnapshotId());
    }

    private APIMessage dispatch(Class<?> apiClass, Map<String, String[]> parameters) throws Exception {
        RestServer server = new RestServer();
        RestServer.Api api = server.new Api(apiClass, apiClass.getAnnotation(org.zstack.header.rest.RestRequest.class));
        installAuthBackend(server);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getContextPath()).thenReturn("");
        when(request.getHeader("Authorization")).thenReturn("OAuth test-session");
        when(request.getHeaderNames()).thenReturn(Collections.enumeration(Collections.singleton("Authorization")));
        when(request.getRemoteHost()).thenReturn("localhost");
        String path = api.requestAnnotation.path();
        path = path.replaceAll("\\{[^}]+\\}", "resource-test");
        when(request.getRequestURI()).thenReturn("/v1" + path);
        when(request.getParameterMap()).thenReturn(parameters);
        installRequestInfo(request);
        AtomicReference<APIMessage> dispatched = new AtomicReference<>();
        installCloudBus(server, dispatched);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "OAuth test-session");
        Method handler = RestServer.class.getDeclaredMethod("handleUniqueApi", RestServer.Api.class,
                HttpEntity.class, HttpServletRequest.class, HttpServletResponse.class);
        handler.setAccessible(true);
        try {
            handler.invoke(server, api, new HttpEntity<String>("{}", headers), request,
                    mock(HttpServletResponse.class));
            return dispatched.get();
        } catch (InvocationTargetException e) {
            if (e.getCause() instanceof RestException) {
                if (dispatched.get() != null) {
                    return dispatched.get();
                }
                RestException restError = (RestException) e.getCause();
                assertEquals(400, restError.statusCode);
                assertTrue(restError.error.contains(parameters.keySet().iterator().next()));
                return null;
            }
            if (dispatched.get() == null) {
                throw new AssertionError("Unexpected failure before dispatch", e.getCause());
            }
            return dispatched.get();
        }
    }

    private CloudBus installCloudBus(RestServer server, AtomicReference<APIMessage> dispatched) throws Exception {
        CloudBus bus = mock(CloudBus.class);
        MessageReply reply = new MessageReply();
        reply.setSuccess(false);
        org.zstack.header.errorcode.ErrorCode error = mock(org.zstack.header.errorcode.ErrorCode.class);
        reply.setError(error);
        when(bus.call(any(org.zstack.header.message.NeedReplyMessage.class))).thenAnswer(invocation -> {
            dispatched.set((APIMessage) invocation.getArgument(0));
            return reply;
        });
        Field field = RestServer.class.getDeclaredField("bus");
        field.setAccessible(true);
        field.set(server, bus);
        return bus;
    }

    @SuppressWarnings("unchecked")
    private void installRequestInfo(HttpServletRequest request) throws Exception {
        Class<?> infoType = Class.forName("org.zstack.rest.RestServer$RequestInfo");
        java.lang.reflect.Constructor<?> ctor = infoType.getDeclaredConstructor(HttpServletRequest.class);
        ctor.setAccessible(true);
        Object info = ctor.newInstance(request);
        Field field = RestServer.class.getDeclaredField("requestInfo");
        field.setAccessible(true);
        ((ThreadLocal<Object>) field.get(null)).set(info);
    }

    @SuppressWarnings("unchecked")
    private void installAuthBackend(RestServer server) throws Exception {
        Field field = RestServer.class.getDeclaredField("restAuthBackends");
        field.setAccessible(true);
        Map<RestAuthenticationType, RestAuthenticationBackend> backends = (Map<RestAuthenticationType, RestAuthenticationBackend>) field.get(server);
        RestAuthenticationBackend backend = new RestAuthenticationBackend() {
            @Override public RestAuthenticationType getAuthenticationType() { return AccountConstant.ACCOUNT_REST_AUTHENTICATION_TYPE; }
            @Override public SessionInventory doAuth(RestAuthenticationParams params) {
                SessionInventory session = new SessionInventory();
                session.setAccountUuid(AccountConstant.INITIAL_SYSTEM_ADMIN_UUID);
                session.setUuid("test-session");
                return session;
            }
        };
        backends.put(backend.getAuthenticationType(), backend);
    }
}
