package org.zstack.rest;

import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.host.APIQueryHostReply;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

public class RestServerQueryParameterTest {
    @RestRequest(path = "/test-query-parameter", method = HttpMethod.GET,
            responseClass = APIQueryHostReply.class, strictQueryParameters = true)
    public static class QueryParameterFixture extends APIMessage {
        @APIParam(required = false)
        private Integer start;
        @APIParam(required = false)
        private Float ratio;
        @APIParam(required = false)
        private List<Integer> numbers;
    }

    private Object api(Class<?> apiClass) throws Exception {
        Class<?> apiType = Class.forName("org.zstack.rest.RestServer$Api");
        Constructor<?> constructor = apiType.getDeclaredConstructor(RestServer.class, Class.class, RestRequest.class);
        constructor.setAccessible(true);
        return constructor.newInstance(new RestServer(), apiClass, apiClass.getAnnotation(RestRequest.class));
    }

    private Object convert(Object api, String name, String value) throws Exception {
        Method method = api.getClass().getDeclaredMethod("queryParameterToApiFieldValue", String.class, String[].class);
        method.setAccessible(true);
        return method.invoke(api, name, new String[]{value});
    }

    @Test
    public void malformedIntegerUsesBadRequestRestExceptionInsteadOfInternalError() throws Exception {
        try {
            convert(api(QueryParameterFixture.class), "start", "abc");
            fail("malformed integer should be rejected");
        } catch (java.lang.reflect.InvocationTargetException e) {
            assertEquals(org.zstack.header.rest.RestException.class, e.getCause().getClass());
            org.zstack.header.rest.RestException error = (org.zstack.header.rest.RestException) e.getCause();
            assertEquals(400, error.statusCode);
            assertEquals("Invalid query parameter. 'start' must be an integer", error.error);
        }
    }

    @Test
    public void validIntegerStillUsesTheExistingConversionPath() throws Exception {
        assertEquals(12, convert(api(QueryParameterFixture.class), "start", "12"));
    }

    @Test
    public void validCollectionStillUsesTheExistingConversionPath() throws Exception {
        assertEquals(Collections.singletonList(12), convert(api(QueryParameterFixture.class), "numbers", "12"));
    }

    @Test
    public void malformedNumbersUseBadRequestForTheirElementType() throws Exception {
        for (String field : new String[]{"ratio", "numbers"}) {
            try {
                convert(api(QueryParameterFixture.class), field, "abc");
                fail("malformed number should be rejected: " + field);
            } catch (java.lang.reflect.InvocationTargetException e) {
                assertEquals(org.zstack.header.rest.RestException.class, e.getCause().getClass());
                org.zstack.header.rest.RestException error = (org.zstack.header.rest.RestException) e.getCause();
                assertEquals(400, error.statusCode);
                assertEquals("numbers".equals(field), error.error.contains("must be an integer"));
            }
        }
        assertEquals(Float.valueOf("1.5"), convert(api(QueryParameterFixture.class), "ratio", "1.5"));
    }

    @Test
    public void unknownFieldRemainsIgnoredByTheConversionHelper() throws Exception {
        assertEquals(null, convert(api(QueryParameterFixture.class), "unknown", "abc"));
    }
}
