package org.zstack.testlib.vops

import org.springframework.beans.factory.annotation.Autowired
import org.zstack.core.errorcode.ErrorFacade
import org.zstack.header.errorcode.ErrorableValue
import org.zstack.header.rest.RestHttp
import org.zstack.externalservice.vops.VOpsClient

import java.util.function.Function

class VOpsClientForTest extends VOpsClient {
    public final VOpsVirtualEndpointSpec parent

    VOpsClientForTest(VOpsVirtualEndpointSpec parent) {
        this.parent = parent
    }

    @Autowired
    ErrorFacade errorFacade

    /**
     * key example:
     *   "GET:/open/ping"
     *   "POST:/open/ping"
     */
    public final Map<String, Handler> defaultHandlers = [:]
    /**
     * key example:
     *   api_id
     */
    public final Map<String, Handler> apiHandlers = [:]

    static class Handler {
        String method
        String path
        Function<HttpForTest, Object> function

        String key() {
            return "${method}:${this.path}"
        }

        boolean match(HttpForTest http) {
            return method == http.method.toString() && this.path == http.getPathWithoutIpAndPort()
        }

        static Handler ofGet(String path, Function<HttpForTest, Object> function) {
            Handler handler = new Handler()
            handler.method = "GET"
            handler.path = path
            handler.function = function
            return handler
        }

        static Handler ofPut(String path, Function<HttpForTest, Object> function) {
            Handler handler = new Handler()
            handler.method = "PUT"
            handler.path = path
            handler.function = function
            return handler
        }
    }

    void addDefaultHandler(Handler handler) {
        defaultHandlers.put(handler.key(), handler)
    }

    @Override
    protected <T> RestHttp<T> http(Class<T> returnClass) {
        return new HttpForTest<T>(returnClass, this)
    }

    static class HttpForTest<T> extends RestHttp<T> {
        final VOpsClientForTest client

        HttpForTest(Class<T> returnClass, VOpsClientForTest client) {
            super(returnClass)
            this.client = client
            this.errorCodeBuilder = { Exception e, http2 -> client.errorFacade.throwableToOperationError(e) }
        }

        /**
         * "http://localhost:8080/a/b/c" -> "/a/b/c"
         * "http://localhost:8080/a/b/c?id=123" -> "/a/b/c"
         */
        String getPathWithoutIpAndPort() {
            assert path != null : "path cannot be null"
            int slashIndex = path.indexOf("/", 8)
            if (slashIndex == -1) {
                throw new RuntimeException("invalid path: ${path}")
            }

            int queryIndex = path.indexOf("?")
            return queryIndex == -1 ? path.substring(slashIndex) : path.substring(slashIndex, queryIndex)
        }

        @Override
        ErrorableValue<T> handleWithErrorCode() {
            def result = findValueFromHandle()

            for (def handler : new ArrayList<>(client.parent.postHandlers)) {
                if (handler.condition.test(this)) {
                    try {
                        def next = handler.runIfMatch.apply(this, result)
                        result = next == null ? result : next
                    } catch (Exception e) {
                        result = ErrorableValue.ofErrorCode(errorCodeBuilder.apply(e, this))
                    }
                }
            }
            return result
        }

        private ErrorableValue<T> findValueFromHandle() {
            def path = getPathWithoutIpAndPort()

            Handler currentHandler
            if (path.startsWith("/api/")) {
                def apiId = path.substring(5)
                currentHandler = client.apiHandlers.get(apiId)
            } else {
                currentHandler = client.defaultHandlers.get(method.toString() + ":" + path)
            }

            if (currentHandler == null) {
                throw new RuntimeException("no handler found for path: ${getPath()}")
            }
            return ErrorableValue.of((T) currentHandler.function.apply(this))
        }
    }
}
