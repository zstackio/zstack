package org.zstack.test.integration.core.rest

import org.springframework.http.HttpEntity
import org.zstack.core.CoreGlobalProperty
import org.zstack.core.rest.RESTFacadeImpl
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.rest.AsyncRESTCallback
import org.zstack.test.integration.ZStackTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.WebBeanConstructor
import org.zstack.utils.URLBuilder

import java.util.concurrent.TimeUnit

class RestFacadeKeepAliveCase extends SubCase {
    EnvSpec env
    String BASE_URL = "/test-keep-alive"

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
            testKeepAliveDefault()
            testKeepAliveCap()
            testAsyncPostStillWorks()
        }
    }

    // keep-alive cap defaults to 5s, must be < agent socket_timeout
    void testKeepAliveDefault() {
        assert CoreGlobalProperty.REST_FACADE_KEEPALIVE_TIME == 5000
    }

    // client never keeps a connection longer than the cap, even when the agent advertises more or nothing
    void testKeepAliveCap() {
        assert RESTFacadeImpl.cappedKeepAlive(-1, 5000) == 5000
        assert RESTFacadeImpl.cappedKeepAlive(10000, 5000) == 5000
        assert RESTFacadeImpl.cappedKeepAlive(2000, 5000) == 2000
    }

    // the keep-alive-capped async client still completes a normal post
    void testAsyncPostStillWorks() {
        RESTFacadeImpl restf = bean(RESTFacadeImpl.class)

        env.simulator(BASE_URL) { HttpEntity<String> e ->
            return e.toString()
        }

        boolean success = false
        String url = URLBuilder.buildHttpUrl("127.0.0.1", WebBeanConstructor.port, BASE_URL)
        restf.asyncJsonPost(url, "ping", new AsyncRESTCallback(null) {
            @Override
            void fail(ErrorCode err) {
            }

            @Override
            void success(HttpEntity<String> responseEntity) {
                success = true
            }
        }, TimeUnit.MILLISECONDS, 5000)

        retryInSecs {
            assert success
        }
    }
}
