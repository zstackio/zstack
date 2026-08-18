package org.zstack.test.integration.core.rest

import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpStatusCodeException
import org.zstack.core.rest.RESTFacadeImpl
import org.zstack.header.console.ConsoleConstants
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.errorcode.SysErrors
import org.zstack.header.rest.AsyncRESTCallback
import org.zstack.header.rest.RESTConstant
import org.zstack.header.rest.SyncHttpResponse
import org.zstack.test.core.rest.RESTBeanForTest
import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.WebBeanConstructor
import org.zstack.utils.URLBuilder

import java.util.concurrent.TimeUnit

class RestFacadeCase extends SubCase {
    EnvSpec env
    String BASE_URL = "/test-rest-facade"

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {
        env = env {

        }
    }

    @Override
    void test() {
        env.create {
            testRestFacadeFailureWillBeRecorded()
            testSyncHandlerWritesResponseBody()
            testSyncHandlerPreservesStatusAndBody()
        }
    }

    void testSyncHandlerPreservesStatusAndBody() {
        RESTFacadeImpl restf = bean(RESTFacadeImpl.class)
        String commandPath = "/test-rest-facade-status-body-${UUID.randomUUID()}"
        String body = '{"condition":"receiver-overloaded"}'
        restf.registerSyncHttpStatusBodyCallHandler(commandPath, Map.class) {
            Map ignored -> new SyncHttpResponse(429, body)
        }

        HttpHeaders headers = new HttpHeaders()
        headers.set(RESTConstant.COMMAND_PATH, commandPath)
        try {
            restf.getRESTTemplate().exchange(restf.getSendCommandUrl(), HttpMethod.POST,
                    new HttpEntity<String>("{}", headers), String.class)
            assert false: "expected the HTTP client to observe status 429"
        } catch (HttpStatusCodeException e) {
            assert e.rawStatusCode == 429
            assert e.responseBodyAsString == body
            assert e.responseHeaders.getFirst(HttpHeaders.CONTENT_TYPE).startsWith("application/json")
        }
    }

    void testSyncHandlerWritesResponseBody() {
        RESTFacadeImpl restf = bean(RESTFacadeImpl.class)
        String commandPath = "/test-rest-facade-response-body-${UUID.randomUUID()}"
        String body = '{"result":"ok"}'
        restf.registerSyncHttpCallHandler(commandPath, Map.class) { Map ignored -> body }

        HttpHeaders headers = new HttpHeaders()
        headers.set(RESTConstant.COMMAND_PATH, commandPath)
        ResponseEntity<String> response = restf.getRESTTemplate().exchange(restf.getSendCommandUrl(),
                HttpMethod.POST, new HttpEntity<String>("{}", headers), String.class)

        assert response.statusCodeValue == 200
        assert response.body == body
        assert response.headers.getFirst(HttpHeaders.CONTENT_TYPE).startsWith("application/json")
    }

    void testRestFacadeFailureWillBeRecorded() {
        RESTFacadeImpl restf = bean(RESTFacadeImpl.class)

        String requestContent = "test"
        boolean hangUntilTimeout = true
        env.simulator(BASE_URL) { HttpEntity<String> e ->
            while (hangUntilTimeout) {
                sleep(100)
            }

            assert e.body == requestContent
            return e.toString()
        }

        boolean timeout = false
        boolean success = false
        String url = URLBuilder.buildHttpUrl("127.0.0.1", WebBeanConstructor.port, BASE_URL)
        restf.asyncJsonPost(url, requestContent, new AsyncRESTCallback(null) {
            @Override
            void fail(ErrorCode err) {
                timeout = err.isError(SysErrors.TIMEOUT)
            }

            @Override
            void success(HttpEntity<String> responseEntity) {
                success = true
            }
        }, TimeUnit.MILLISECONDS, 200)

        retryInSecs {
            assert timeout
            assert !success
        }

        hangUntilTimeout = false
        retryInSecs {
            assert restf.notifiedFailureHttpTasks.size() == 1
        }

        restf.notifiedFailureHttpTasks.clear()
    }
}
