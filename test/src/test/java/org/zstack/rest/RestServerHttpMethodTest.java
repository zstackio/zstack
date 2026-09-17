package org.zstack.rest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.zstack.core.config.GlobalConfig;
import org.zstack.header.rest.RestException;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.zone.APIDeleteZoneMsg;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class RestServerHttpMethodTest {
    private GlobalConfig original;
    private GlobalConfig config;

    @Before
    public void setUp() {
        original = RestGlobalConfig.CHECK_HTTP_METHOD;
        config = mock(GlobalConfig.class);
        RestGlobalConfig.CHECK_HTTP_METHOD = config;
    }

    @After
    public void tearDown() {
        RestGlobalConfig.CHECK_HTTP_METHOD = original;
    }

    @Test
    public void getMustNotEnterDeleteHandlingWhenCheckIsEnabled() throws Exception {
        when(config.value(Boolean.class)).thenReturn(true);
        RestException error = invokeDeleteRoute("GET");
        assertEquals(405, error.statusCode);
        assertTrue(error.error.contains("expected method[DELETE]"));
    }

    @Test
    public void declaredMethodStillReachesAuthentication() throws Exception {
        when(config.value(Boolean.class)).thenReturn(true);
        RestException error = invokeDeleteRoute("DELETE");
        assertEquals(400, error.statusCode);
        assertEquals("missing header 'Authorization'", error.error);
    }

    @Test
    public void disablingCheckRestoresLegacyDispatchAndCanBeReenabled() throws Exception {
        when(config.value(Boolean.class)).thenReturn(false);
        RestException error = invokeDeleteRoute("GET");
        assertEquals(400, error.statusCode);
        assertEquals("missing header 'Authorization'", error.error);
        when(config.value(Boolean.class)).thenReturn(true);
        assertEquals(405, invokeDeleteRoute("GET").statusCode);
    }

    private RestException invokeDeleteRoute(String method) throws Exception {
        RestServer server = new RestServer();
        RestServer.Api api = server.new Api(APIDeleteZoneMsg.class,
                APIDeleteZoneMsg.class.getAnnotation(RestRequest.class));
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn(method);
        when(request.getRequestURI()).thenReturn("/zstack/v1/zones/test-zone");
        when(request.getContextPath()).thenReturn("/zstack");
        Method handler = RestServer.class.getDeclaredMethod("handleUniqueApi", RestServer.Api.class,
                HttpEntity.class, HttpServletRequest.class, HttpServletResponse.class);
        handler.setAccessible(true);
        try {
            handler.invoke(server, api, new HttpEntity<String>("{}"), request,
                    mock(HttpServletResponse.class));
            fail("Expected rejection before any business operation");
            return null;
        } catch (InvocationTargetException e) {
            assertTrue(e.getCause().toString(), e.getCause() instanceof RestException);
            return (RestException) e.getCause();
        }
    }
}
