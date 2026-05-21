package org.zstack.core.rest;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import org.apache.http.HttpStatus;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsAsyncClientHttpRequestFactory;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.client.*;
import org.springframework.web.util.UriComponentsBuilder;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.MessageCommandRecorder;
import org.zstack.core.Platform;
import org.zstack.core.debug.DebugManager;
import org.zstack.core.retry.Retry;
import org.zstack.core.retry.RetryCondition;
import org.zstack.core.telemetry.MessageTracingHelper;
import org.zstack.core.telemetry.TelemetryFacade;
import org.zstack.core.telemetry.TelemetryGlobalProperty;
import org.zstack.core.thread.AsyncThread;
import org.zstack.core.thread.CancelablePeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.thread.ThreadFacadeImpl.TimeoutTaskReceipt;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.core.validation.ValidationFacade;
import org.zstack.header.Constants;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.rest.*;
import org.zstack.utils.ExceptionDSL;
import org.zstack.utils.IptablesUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.IPv6NetworkUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.zstack.core.Platform.*;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.*;

public class RESTFacadeImpl implements RESTFacade {
    private static final CLogger logger = Utils.getSafeLogger(RESTFacadeImpl.class);
    
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private ApiTimeoutManager timeoutMgr;
    @Autowired
    private ValidationFacade vf;

    private TelemetryFacade telemetryFacade;

    private String hostname;
    private int port = 8080;
    private String path;
    private String callbackUrl;
    private TimeoutRestTemplate template;
    private AsyncRestTemplate asyncRestTemplate;
    private String baseUrl;
    private String sendCommandUrl;
    private String callbackHostName;

    private final int notifiedFailureHttpTasksSize = 128;

    final private Map<String, HttpCallStatistic> statistics = new ConcurrentHashMap<String, HttpCallStatistic>();
    final private Map<String, HttpCallHandlerWrapper> httpCallhandlers = new ConcurrentHashMap<String, HttpCallHandlerWrapper>();
    private final List<BeforeAsyncJsonPostInterceptor> interceptors = new ArrayList<BeforeAsyncJsonPostInterceptor>();

    private interface AsyncHttpWrapper {
        void fail(ErrorCode err);

        void success(HttpEntity<String> responseEntity);
    }

    private interface HttpCallHandlerWrapper {
        String handle(HttpEntity<String> entity);

        HttpCallHandler getHandler();
    }

    final private Map<String, AsyncHttpWrapper> wrappers = new ConcurrentHashMap<String, AsyncHttpWrapper>();
    final private Map<String, String> notifiedFailureHttpTasks = Collections.synchronizedMap(new LinkedHashMap<String, String>(notifiedFailureHttpTasksSize, 0.9f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry eldest) {
            return this.size() > notifiedFailureHttpTasksSize;
        }
    });

    private TelemetryFacade getTelemetryFacade() {
        if (telemetryFacade == null && TelemetryGlobalProperty.ENABLED) {
            telemetryFacade = Platform.getComponentLoader().getComponentNoExceptionWhenNotExisting(TelemetryFacade.class);
        }
        return telemetryFacade;
    }

    private boolean isTelemetryEnabled() {
        return TelemetryGlobalProperty.ENABLED && getTelemetryFacade() != null && getTelemetryFacade().isEnabled();
    }

    private void injectTraceHeaders(HttpHeaders headers) {
        if (!isTelemetryEnabled()) {
            return;
        }

        String traceparent = ThreadContext.get(Constants.THREAD_CONTEXT_TRACEPARENT);
        if (traceparent != null && !traceparent.isEmpty()) {
            headers.set(Constants.HTTP_HEADER_TRACEPARENT, traceparent);
        }
        String tracestate = ThreadContext.get(Constants.THREAD_CONTEXT_TRACESTATE);
        if (tracestate != null && !tracestate.isEmpty()) {
            headers.set(Constants.HTTP_HEADER_TRACESTATE, tracestate);
        }
    }

    private void endSpanAndScope(Span span, Scope scope) {
        if (span != null) {
            try {
                span.end();
            } catch (Exception e) {
                logger.trace("Failed to end HTTP span", e);
            }
        }
        // Scope has already been closed in the creating thread, only handle span here
        // If scope is not null (theoretically should not happen), still try to close it to avoid resource leaks
        if (scope != null) {
            try {
                scope.close();
            } catch (Exception e) {
                logger.trace("Failed to close HTTP scope", e);
            }
        }
    }

    void init() {
        DebugManager.registerDebugSignalHandler("DumpRestStats", () -> {
            StringBuilder sb = new StringBuilder();
            sb.append("\n================ BEGIN: REST CALL Statistics ===================\n");
            if (!CoreGlobalProperty.PROFILER_HTTP_CALL) {
                sb.append("# rest call profiler is off\n");
            } else {
                List<HttpCallStatistic> hstats = new ArrayList<HttpCallStatistic>(getStatistics().values());
                hstats.sort((o1, o2) -> (int) (o2.getTotalTime() - o1.getTotalTime()));
                for (HttpCallStatistic stat : hstats) {
                    sb.append(stat.toString());
                    sb.append("\n");
                }
            }
            sb.append("================ END: REST CALL Statistics =====================\n");
            logger.debug(sb.toString());
        });

        DebugManager.registerDebugSignalHandler("DumpFailedRestTasks", () -> {
            StringBuilder sb = new StringBuilder();
            sb.append("\n================ BEGIN: FAILED REST CALL TASKS ===================\n");
            synchronized (notifiedFailureHttpTasks) {
                notifiedFailureHttpTasks.forEach((k, v) -> sb.append(String.format("task[%s] failed because no 'callback found for taskUuid'. request body: %s\n", k, v)));
            }
            sb.append("================ END: FAILED REST CALL TASKS =====================\n");
            logger.debug(sb.toString());
        });

        port = Platform.getManagementNodeServicePort();

        IptablesUtils.insertRuleToFilterTable(String.format("-A INPUT -p tcp -m state --state NEW -m tcp --dport %s -j ACCEPT", port));

        if ("AUTO".equals(hostname)) {
            callbackHostName = Platform.getManagementServerIp();
        } else {
            callbackHostName = hostname.trim();
        }

        String url = IPv6NetworkUtils.buildHttpUrl(callbackHostName, port);
        if (path != null && !path.isEmpty()) {
            url = String.format("%s/%s", url, path);
        }
        UriComponentsBuilder ub = UriComponentsBuilder.fromHttpUrl(url);
        ub.path(RESTConstant.CALLBACK_PATH);
        callbackUrl = ub.build().toUriString();

        ub = UriComponentsBuilder.fromHttpUrl(url);
        baseUrl = ub.build().toUriString();

        ub = UriComponentsBuilder.fromHttpUrl(url);
        ub.path(RESTConstant.COMMAND_CHANNEL_PATH);
        sendCommandUrl = ub.build().toUriString();

        logger.debug(String.format("RESTFacade built callback url: %s", callbackUrl));
        template = RESTFacade.createRestTemplate(CoreGlobalProperty.REST_FACADE_READ_TIMEOUT, CoreGlobalProperty.REST_FACADE_CONNECT_TIMEOUT);
        asyncRestTemplate = createAsyncRestTemplate(
                CoreGlobalProperty.REST_FACADE_READ_TIMEOUT,
                CoreGlobalProperty.REST_FACADE_CONNECT_TIMEOUT,
                CoreGlobalProperty.REST_FACADE_MAX_PER_ROUTE,
                CoreGlobalProperty.REST_FACADE_MAX_TOTAL);
    }

    // timeout are in milliseconds
    private static AsyncRestTemplate createAsyncRestTemplate(int readTimeout, int connectTimeout, int maxPerRoute, int maxTotal) {
        PoolingNHttpClientConnectionManager connectionManager;
        try {
            connectionManager = new PoolingNHttpClientConnectionManager(new DefaultConnectingIOReactor(IOReactorConfig.DEFAULT));
        } catch (IOReactorException ex) {
            throw new CloudRuntimeException(ex);
        }

        connectionManager.setDefaultMaxPerRoute(maxPerRoute);
        connectionManager.setMaxTotal(maxTotal);

        CloseableHttpAsyncClient httpAsyncClient = HttpAsyncClients.custom()
                .setConnectionManager(connectionManager)
                .build();

        HttpComponentsAsyncClientHttpRequestFactory cf = new HttpComponentsAsyncClientHttpRequestFactory(httpAsyncClient);
        cf.setConnectTimeout(connectTimeout);
        cf.setReadTimeout(readTimeout);
        cf.setConnectionRequestTimeout(connectTimeout * 2);

        AsyncRestTemplate asyncRestTemplate = new AsyncRestTemplate(cf);
        RESTFacade.setMessageConverter(asyncRestTemplate.getMessageConverters());

        return asyncRestTemplate;
    }

    void notifyCallback(HttpServletRequest req, HttpServletResponse rsp) {
        String taskUuid = req.getHeader(RESTConstant.TASK_UUID);
        try {
            HttpEntity<String> entity = this.httpServletRequestToHttpEntity(req);
            if (taskUuid == null) {
                rsp.sendError(HttpStatus.SC_BAD_REQUEST, "No 'taskUuid' found in the header");
                logger.warn(String.format("Received a callback request, but no 'taskUuid' found in headers. request body: %s", entity.getBody()));
                return;
            }

            AsyncHttpWrapper wrapper = wrappers.get(taskUuid);
            if (wrapper == null) {
                rsp.sendError(HttpStatus.SC_NOT_FOUND, String.format("No callback found for taskUuid[%s]", taskUuid));
                logger.warn(String.format("Received a callback request, but no 'callback found for taskUuid[%s]. request body: %s", taskUuid, entity.getBody()));
                notifiedFailureHttpTasks.put(taskUuid, entity.getBody());
                return;
            }

            rsp.setStatus(HttpStatus.SC_OK);
            wrapper.success(entity);
        } catch (IOException e) {
            logger.warn(e.getMessage(), e);
        } catch (Throwable t) {
            try {
                rsp.sendError(HttpStatus.SC_INTERNAL_SERVER_ERROR, t.getMessage());
            } catch (IOException e) {
                logger.warn(e.getMessage(), e);
            }
        }
    }

    void sendCommand(HttpServletRequest req, HttpServletResponse rsp) {
        String commandPath = req.getHeader(RESTConstant.COMMAND_PATH);
        try {
            HttpEntity<String> entity = this.httpServletRequestToHttpEntity(req);
            if (commandPath == null) {
                rsp.sendError(HttpStatus.SC_BAD_REQUEST, "No 'commandPath' found in the header");
                logger.warn(String.format("Received a command, but no 'taskUuid' found in headers. request body: %s", entity.getBody()));
                return;
            }

            HttpCallHandlerWrapper handler = httpCallhandlers.get(commandPath);
            if (handler == null) {
                rsp.sendError(HttpStatus.SC_NOT_FOUND, String.format("no handler found for the command path[%s]", commandPath));
                logger.warn(String.format("Received a command, but no handler found for the path[%s]. request body: %s", commandPath, entity.getBody()));
                return;
            }

            String ret = handler.handle(entity);
            if (ret == null) {
                rsp.setStatus(HttpStatus.SC_OK);
            } else {
                rsp.setStatus(HttpStatus.SC_OK, ret);
            }
        } catch (IOException e) {
            logger.warn(e.getMessage(), e);
        } catch (Throwable t) {
            logger.warn(t.getMessage(), t);
            try {
                rsp.sendError(HttpStatus.SC_INTERNAL_SERVER_ERROR, t.getMessage());
            } catch (IOException e) {
                logger.warn(e.getMessage(), e);
            }
        }
    }


    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public void asyncJsonPost(String url, Object body, Map<String, String> headers, AsyncRESTCallback callback, TimeUnit unit, long timeout) {
        // for unit test finding invocation chain
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            MessageCommandRecorder.record(body.getClass());
        }
        String bodyStr = JSONObjectUtil.toJsonString(body);
        asyncJsonPost(url, bodyStr, headers, callback, unit, timeout);
    }

    @Override
    public void asyncJsonPost(String url, Object body, AsyncRESTCallback callback, TimeUnit unit, long timeout) {
        asyncJsonPost(url, body, null, callback, unit, timeout);
    }

    @Override
    public void asyncJsonPost(final String url, final String body, final AsyncRESTCallback callback, final TimeUnit unit, final long timeout) {
        asyncJsonPost(url, body, null, callback, unit, timeout);
    }

    @Override
    public void asyncJsonPost(final String url, final String body, Map<String, String> headers, final AsyncRESTCallback callback, final TimeUnit unit, final long timeout) {
        asyncJson(url, body, headers, HttpMethod.POST, callback, unit, timeout);
    }

    @Override
    public void asyncJsonDelete(final String url, final String body, Map<String, String> headers, final AsyncRESTCallback callback, final TimeUnit unit, final long timeout) {
        asyncJson(url, body, headers, HttpMethod.DELETE, callback, unit, timeout);
    }
    @Override
    public void asyncJsonGet(final String url, final String body, Map<String, String> headers, final AsyncRESTCallback callback, final TimeUnit unit, final long timeout) {
        asyncJson(url, body, headers, HttpMethod.GET, callback, unit, timeout);
    }

    @Override
    public void asyncJson(final String url, final String body, Map<String, String> headers, HttpMethod method, final AsyncRESTCallback callback, final TimeUnit unit, final long timeout) {
        synchronized (interceptors) {
            for (BeforeAsyncJsonPostInterceptor ic : interceptors) {
                ic.beforeAsyncJsonPost(url, body, unit, timeout);
            }
        }

        if (unit.toMillis(timeout) <= 1) {
            callback.fail(touterr(ORG_ZSTACK_CORE_REST_10000, "url: %s, current timeout: %s, api message timeout, skip post async call",
                    url, unit.toMillis(timeout)));
            return;
        }

        long stime = 0;
        if (CoreGlobalProperty.PROFILER_HTTP_CALL) {
            stime = System.currentTimeMillis();
            HttpCallStatistic stat = statistics.get(url);
            if (stat == null) {
                stat = new HttpCallStatistic();
                stat.setUrl(url);
                statistics.put(url, stat);
            }
        }

        final String taskUuid = Platform.getUuid();
        final long finalStime = stime;

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentLength(body.length());
        requestHeaders.set(RESTConstant.TASK_UUID, taskUuid);
        requestHeaders.set(RESTConstant.CALLBACK_URL, callbackUrl);
        MediaType JSON = MediaType.parseMediaType("application/json; charset=utf-8");
        requestHeaders.setContentType(JSON);
        if (headers != null) {
            for (Map.Entry<String, String> e : headers.entrySet()) {
                requestHeaders.set(e.getKey(), e.getValue());
            }
        }

        Span httpSpan = null;
        Scope scope = null;
        if (isTelemetryEnabled()) {
            try {
                URI uri = URI.create(url);
                int defaultPort = "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
                httpSpan = getTelemetryFacade().getTracer()
                        .spanBuilder("HTTP " + method.toString())
                        .setSpanKind(SpanKind.CLIENT)
                        .setAttribute("http.method", method.toString())
                        .setAttribute("http.url", url)
                        .setAttribute("http.host", uri.getHost())
                        .setAttribute("http.path", uri.getPath())
                        .setAttribute("net.peer.name", uri.getHost())
                        .setAttribute("net.peer.port", uri.getPort() > 0 ? uri.getPort() : defaultPort)
                        .startSpan();

                scope = httpSpan.makeCurrent();

                Map<String, String> traceCtx = MessageTracingHelper.extractTraceContext();
                if (traceCtx.containsKey(MessageTracingHelper.TRACEPARENT_KEY)) {
                    requestHeaders.set(Constants.HTTP_HEADER_TRACEPARENT, 
                            traceCtx.get(MessageTracingHelper.TRACEPARENT_KEY));
                }
                if (traceCtx.containsKey(MessageTracingHelper.TRACESTATE_KEY)) {
                    requestHeaders.set(Constants.HTTP_HEADER_TRACESTATE, 
                            traceCtx.get(MessageTracingHelper.TRACESTATE_KEY));
                }
                
                // Close scope immediately after header injection in current thread to avoid context pollution from cross-thread closure
                if (scope != null) {
                    try {
                        scope.close();
                    } catch (Exception e) {
                        logger.trace("Failed to close HTTP scope in current thread", e);
                    }
                    scope = null;
                }
            } catch (Exception e) {
                logger.trace("Failed to create HTTP client span", e);
            }
        }

        final Span finalHttpSpan = httpSpan;
        final Scope finalScope = scope;

        HttpEntity<String> req = new HttpEntity<String>(body, requestHeaders);

        AsyncHttpWrapper wrapper = new AsyncHttpWrapper() {
            final AtomicBoolean called = new AtomicBoolean(false);

            final AsyncHttpWrapper self = this;
            final TimeoutTaskReceipt timeoutTaskReceipt = thdf.submitTimeoutTask(new Runnable() {
                @Override
                public void run() {
                    self.fail(touterr(
                    ORG_ZSTACK_CORE_REST_10001,         "[Async Http Timeout] url: %s, timeout after %s[%s], command: %s",
                            url, timeout, unit.toString(), body
                    ));
                }
            }, unit, timeout);

            private void cancelTimeout() {
                timeoutTaskReceipt.cancel();
            }

            final ReturnValueCompletion<HttpEntity<String>> completion = new ReturnValueCompletion<HttpEntity<String>>(callback) {
                @Override
                @AsyncThread
                public void success(HttpEntity<String> responseEntity) {
                    if (!called.compareAndSet(false, true)) {
                        logger.warn(String.format("Success callback many times, taskId=%s, currentTimeMillis=%s", taskUuid, System.currentTimeMillis()));
                        return;
                    }

                    try {
                        if (CoreGlobalProperty.PROFILER_HTTP_CALL) {
                            HttpCallStatistic stat = statistics.get(url);
                            stat.addStatistic(System.currentTimeMillis() - finalStime);
                        }

                        wrappers.remove(taskUuid);
                        cancelTimeout();

                        if (logger.isTraceEnabled()) {
                            List<String> hs = responseEntity.getHeaders().get(RESTConstant.TASK_UUID);
                            String taskUuid = hs == null || hs.isEmpty() ? null : hs.get(0);

                            if (taskUuid == null) {
                                logger.trace(String.format("[http response(url: %s)] %s", url, responseEntity.getBody()));
                            } else {
                                logger.trace(String.format("[http response(url: %s, taskUuid: %s)] %s",
                                        url, taskUuid, responseEntity.getBody()));
                            }
                        }

                        if (callback instanceof JsonAsyncRESTCallback) {
                            JsonAsyncRESTCallback<Object> jcallback = (JsonAsyncRESTCallback)callback;
                            try {
                                Object obj = JSONObjectUtil.toObject(responseEntity.getBody(), jcallback.getReturnClass());
                                ErrorCode err = vf.validateErrorByErrorCode(obj);
                                if (err != null) {
                                    logger.warn(String.format("error response that causes validation failure: %s", responseEntity.getBody()));
                                    if (finalHttpSpan != null) {
                                        finalHttpSpan.setStatus(StatusCode.ERROR, err.getDetails());
                                    }
                                    jcallback.fail(err);
                                } else {
                                    if (finalHttpSpan != null) {
                                        finalHttpSpan.setStatus(StatusCode.OK);
                                    }
                                    jcallback.success(obj);
                                }
                            } catch (Throwable t) {
                                logger.warn(t.getMessage(), t);
                                if (finalHttpSpan != null) {
                                    finalHttpSpan.recordException(t);
                                    finalHttpSpan.setStatus(StatusCode.ERROR, t.getMessage());
                                }
                                callback.fail(inerr(ORG_ZSTACK_CORE_REST_10002, t.getMessage()));
                            }
                        } else {
                            if (finalHttpSpan != null) {
                                finalHttpSpan.setStatus(StatusCode.OK);
                            }
                            callback.success(responseEntity);
                        }
                    } finally {
                        endSpanAndScope(finalHttpSpan, finalScope);
                    }
                }

                @Override
                @AsyncThread
                public void fail(ErrorCode err) {
                    if (!called.compareAndSet(false, true)) {
                        logger.warn(String.format("Failed callback many times, taskId=%s, currentTimeMillis=%s", taskUuid, System.currentTimeMillis()));
                        return;
                    }

                    try {
                        wrappers.remove(taskUuid);
                        if (!SysErrors.TIMEOUT.toString().equals(err.getCode())) {
                            cancelTimeout();
                        }

                        if (finalHttpSpan != null) {
                            finalHttpSpan.setStatus(StatusCode.ERROR, err.getDetails());
                        }

                        logger.warn(String.format("Unable to post to %s: %s", url, err.getDetails()));
                        callback.fail(err);
                    } finally {
                        endSpanAndScope(finalHttpSpan, finalScope);
                    }
                }
            };

            @Override
            public void fail(ErrorCode err) {
                completion.fail(err);
            }

            @Override
            public void success(HttpEntity<String> responseEntity) {
                completion.success(responseEntity);
            }
        };

        try {
            wrappers.put(taskUuid, wrapper);

            if (logger.isTraceEnabled()) {
                logger.trace(String.format("json %s [%s], %s", method.toString(), url, req));
            }

            ListenableFuture<ResponseEntity<String>> f = asyncRestTemplate.exchange(url, method, req, String.class);
            f.addCallback(rsp -> {}, e -> wrapper.fail(err(ORG_ZSTACK_CORE_REST_10003, SysErrors.HTTP_ERROR, e.getLocalizedMessage())));
        } catch (RestClientException e) {
            logger.warn(String.format("Unable to %s to %s: %s", method.toString(), url, e.getMessage()));
            wrapper.fail(ExceptionDSL.isCausedBy(e, ResourceAccessException.class) ? err(ORG_ZSTACK_CORE_REST_10004, SysErrors.IO_ERROR, e.getMessage()) : inerr(ORG_ZSTACK_CORE_REST_10005, e.getMessage()));
        }
    }

    @Override
    public void asyncJsonPost(String url, Object body, Map<String, String> headers, AsyncRESTCallback callback) {
        Long timeout = timeoutMgr.getTimeout();
        asyncJsonPost(url, body, headers, callback, TimeUnit.MILLISECONDS, timeout);
    }

    @Override
    public void asyncJsonPost(String url, Object body, AsyncRESTCallback callback) {
        Long timeout = timeoutMgr.getTimeout();
        asyncJsonPost(url, body, callback, TimeUnit.MILLISECONDS, timeout);
    }

    @Override
    public void asyncJsonPost(String url, String body, AsyncRESTCallback callback) {
        Long timeout = timeoutMgr.getTimeout();
        asyncJsonPost(url, body, callback, TimeUnit.MILLISECONDS, timeout);
    }

    @Override
    public HttpEntity<String> httpServletRequestToHttpEntity(HttpServletRequest req) {
        try {
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = req.getReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }

            HttpHeaders header = new HttpHeaders();
            for (Enumeration<?> e = req.getHeaderNames() ; e.hasMoreElements() ;) {
                String name = e.nextElement().toString();
                header.add(name, req.getHeader(name));
            }

            return new HttpEntity<String>(sb.toString(), header);
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
            throw new CloudRuntimeException(e);
        }
    }

    @Override
    public RestTemplate getRESTTemplate() {
        return template;
    }

    @Override
    public <T> T syncJsonPost(String url, Object body, Class<T> returnClass) {
        // for unit test finding invocation chain
        if (CoreGlobalProperty.UNIT_TEST_ON && body != null) {
            MessageCommandRecorder.record(body.getClass());
        }

        return syncJsonPost(url, body == null ? null : JSONObjectUtil.toJsonString(body), returnClass);
    }

    @Override
    public <T> T syncJsonPost(String url, Object body, Class<T> returnClass, TimeUnit unit, long timeout) {
        // for unit test finding invocation chain
        if (CoreGlobalProperty.UNIT_TEST_ON && body != null) {
            MessageCommandRecorder.record(body.getClass());
        }

        return syncJsonPost(url, body == null ? null : JSONObjectUtil.toJsonString(body),null, returnClass, unit, timeout);
    }

    public <T> RestHttp<T> http(Class<T> returnClass) {
        RestHttp<T> http = new RestHttp<>(returnClass)
                .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE);

        if (CoreGlobalProperty.UNIT_TEST_ON) {
            http.retryIfException(ResourceAccessException.class);
        } else {
            http.retryIfException(ResourceAccessException.class, HttpStatusCodeException.class);
        }

        return http
                .withHandler(this::syncJson)
                .withErrorCodeBuilder((Exception e, RestHttp<T>http2) -> {
                    if (e instanceof HttpStatusCodeException) {
                        final HttpStatusCodeException exception = (HttpStatusCodeException) e;
                        return operr(ORG_ZSTACK_CORE_REST_10006, "failed to %s to %s, status code: %s, response body: %s",
                                http2.getMethod().toString().toLowerCase(),
                                http2.getPath(),
                                exception.getStatusCode(), exception.getResponseBodyAsString());
                    } else if (e instanceof ResourceAccessException) {
                        return operr(ORG_ZSTACK_CORE_REST_10007, "failed to %s to %s, IO Error: %s",
                                http2.getMethod().toString().toLowerCase(),
                                http2.getPath(),
                                e.getMessage());
                    }
                    return null;
                });
    }

    protected <T> ResponseEntity<String> syncJson(RestHttp<T> http) {
        String body = http.getBody() == null ? "" : http.getBody();

        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setAll(http.getHeaders());
        requestHeaders.setContentLength(body.length());
        HttpEntity<String> req = new HttpEntity<>(body, requestHeaders);
        ResponseEntity<String> rsp = syncRawJson(req, http);

        if (logger.isTraceEnabled()) {
            logger.trace(String.format("[http response(url: %s)] %s", http.getPath(), rsp.getBody()));
        }
        return rsp;
    }

    public <T> ResponseEntity<String> syncRawJson(HttpEntity<String> req, RestHttp<T> http) {
        if (logger.isTraceEnabled()) {
            logger.trace(String.format("json %s[%s], %s", http.getMethod().toString().toLowerCase(), http.getPath(), req));
        }

        ResponseEntity<String> rsp;
        final String url = http.getPath();
        final HttpMethod method = http.getMethod();

        try {
            if (http.isRetry()) {
                rsp = new Retry<ResponseEntity<String>>() {
                    {
                        retryConditions.addAll(http.getRetryIfExceptionMatched());
                        times = http.getRetryTimes();
                        interval = http.getRetryIntervalInSeconds();
                    }

                    @Override
                    protected ResponseEntity<String> call() {
                        if (http.isTimeoutEnabled()) {
                            return template.exchange(url, method, req, String.class);
                        } else {
                            final long millis = http.getTimeoutInMillis();
                            return template.exchange(url, method, req, String.class, Platform.getUuid(), millis, millis);
                        }
                    }
                }.run();
            } else {
                rsp = (http.isTimeoutEnabled()) ?
                        template.exchange(url, method, req, String.class,
                                Platform.getUuid(), http.getTimeoutInMillis(), http.getTimeoutInMillis()) :
                        template.exchange(url, method, req, String.class);
            }
        } catch (Exception e) {
            if (http.getErrorCodeBuilder() != null) {
                ErrorCode errorCode = http.getErrorCodeBuilder().apply(e, http);
                if (errorCode != null) {
                    throw new OperationFailureException(errorCode);
                }
            }
            throw e;
        }

        boolean valid = false;
        if (method == HttpMethod.DELETE && rsp.getStatusCode() == org.springframework.http.HttpStatus.NO_CONTENT) {
            valid = true;
        } else if (method == HttpMethod.POST && rsp.getStatusCode() == org.springframework.http.HttpStatus.CREATED) {
            valid = true;
        } else if (rsp.getStatusCode() == org.springframework.http.HttpStatus.OK || rsp.getStatusCode() == org.springframework.http.HttpStatus.ACCEPTED) {
            valid = true;
        }

        if (!valid) {
            throw new OperationFailureException(operr(ORG_ZSTACK_CORE_REST_10008, "failed to %s to %s, status code: %s, response body: %s", method.toString().toLowerCase(), url, rsp.getStatusCode(), rsp.getBody()));
        }

        return rsp;
    }

    @Override
    public <T> T syncJsonPost(String url, String body, Class<T> returnClass) {
        return syncJsonPost(url, body, null, returnClass, null, -1);
    }

    @Override
    public <T> T syncJsonPost(String url, String body, Map<String, String> headers, Class<T> returnClass) {
        return syncJsonPost(url, body, headers, returnClass, null, -1);
    }

    @Override
    public <T> T syncJsonPost(String url, String body, Map<String, String> headers, Class<T> returnClass, TimeUnit unit, long timeout) {
        return syncJson(url, body, headers, HttpMethod.POST, returnClass, unit, timeout);
    }

    @Override
    public <T> T syncJsonDelete(String url, String body, Map<String, String> headers, Class<T> returnClass) {
        return syncJsonDelete(url, body, headers, returnClass, null, -1);
    }
    @Override
    public <T> T syncJsonDelete(String url, String body, Map<String, String> headers, Class<T> returnClass, TimeUnit unit, long timeout) {
        return syncJson(url, body, headers, HttpMethod.DELETE, returnClass, unit, timeout);
    }

    @Override
    public <T> T syncJsonGet(String url, String body, Map<String, String> headers, Class<T> returnClass) {
        return syncJsonGet(url, body, headers, returnClass, null, -1);
    }
    @Override
    public <T> T syncJsonGet(String url, String body, Map<String, String> headers, Class<T> returnClass, TimeUnit unit, long timeout) {
        return syncJson(url, body, headers, HttpMethod.GET, returnClass, unit, timeout);
    }

    @Override
    public <T> T syncJsonPut(String url, String body, Map<String, String> headers, Class<T> returnClass) {
        return syncJsonPut(url, body, headers, returnClass, null, -1);
    }

    @Override
    public <T> T syncJsonPut(String url, String body, Map<String, String> headers, Class<T> returnClass, TimeUnit unit, long timeout) {
        return syncJson(url, body, headers, HttpMethod.PUT, returnClass, unit, timeout);
    }

    @Override
    public HttpHeaders syncHead(String url) {
        return template.headForHeaders(URI.create(url));
    }

    protected  <T> T syncJson(String url, String body, Map<String, String> headers, HttpMethod method, Class<T> returnClass, TimeUnit unit, long timeout) {
        body = body == null ? "" : body;

        HttpHeaders requestHeaders = new HttpHeaders();
        if (headers != null) {
            requestHeaders.setAll(headers);
        }
        requestHeaders.setContentType(MediaType.APPLICATION_JSON);
        requestHeaders.setContentLength(body.length());
        HttpEntity<String> req = new HttpEntity<String>(body, requestHeaders);
        ResponseEntity<String> rsp = syncRawJson(url, req, method, unit, timeout);

        if (rsp.getBody() != null && returnClass != Void.class) {
            if (logger.isTraceEnabled()) {
                logger.trace(String.format("[http response(url: %s)] %s", url, rsp.getBody()));
            }

            return JSONObjectUtil.toObject(rsp.getBody(), returnClass);
        } else {
            if (logger.isTraceEnabled()) {
                logger.trace(String.format("[http response(url: %s)] %s", url, rsp.getBody()));
            }
            return null;
        }
    }

    public ResponseEntity<String> syncRawJson(String url, HttpEntity<String> req, HttpMethod method, TimeUnit unit, long timeout) {
        if (logger.isTraceEnabled()) {
            logger.trace(String.format("json %s[%s], %s", method.toString().toLowerCase(), url, req));
        }

        ResponseEntity<String> rsp;

        try {
            if (CoreGlobalProperty.UNIT_TEST_ON) {
                rsp = new Retry<ResponseEntity<String>>() {
                    @Override
                    @RetryCondition(onExceptions = {ResourceAccessException.class})
                    protected ResponseEntity<String> call() {
                        return template.exchange(url, method, req, String.class);
                    }
                }.run();
            } else {
                rsp = new Retry<ResponseEntity<String>>() {
                    @Override
                    @RetryCondition(onExceptions = {ResourceAccessException.class, HttpStatusCodeException.class})
                    protected ResponseEntity<String> call() {
                        if (unit == null) {
                            return template.exchange(url, method, req, String.class);
                        } else {
                            return template.exchange(url, method, req, String.class, Platform.getUuid(), unit.toMillis(timeout), unit.toMillis(timeout));
                        }
                    }
                }.run();
            }
        } catch (HttpStatusCodeException e) {
            throw new OperationFailureException(operr(ORG_ZSTACK_CORE_REST_10009, "failed to %s to %s, status code: %s, response body: %s", method.toString().toLowerCase(), url, e.getStatusCode(), e.getResponseBodyAsString()));
        } catch (ResourceAccessException e) {
            throw new OperationFailureException(operr(ORG_ZSTACK_CORE_REST_10010, "failed to %s to %s, IO Error: %s", method.toString().toLowerCase(), url, e.getMessage()));
        }

        boolean valid = false;
        if (method == HttpMethod.DELETE && rsp.getStatusCode() == org.springframework.http.HttpStatus.NO_CONTENT) {
            valid = true;
        } else if (method == HttpMethod.POST &&
                (rsp.getStatusCode() == org.springframework.http.HttpStatus.CREATED ||
                        rsp.getStatusCode() == org.springframework.http.HttpStatus.NO_CONTENT)) {
            valid = true;
        } else if (rsp.getStatusCode() == org.springframework.http.HttpStatus.OK || rsp.getStatusCode() == org.springframework.http.HttpStatus.ACCEPTED) {
            valid = true;
        }

        if (!valid) {
            throw new OperationFailureException(operr(ORG_ZSTACK_CORE_REST_10011, "failed to %s to %s, status code: %s, response body: %s", method.toString().toLowerCase(), url, rsp.getStatusCode(), rsp.getBody()));
        }

        return rsp;
    }

    @Override
    public void echo(String url, Completion callback) {
        echo(url, callback, TimeUnit.SECONDS.toMillis(1), TimeUnit.SECONDS.toMillis(CoreGlobalProperty.REST_FACADE_ECHO_TIMEOUT));
    }

    @Override
    public void echo(final String url, final Completion completion, final long interval, long timeout) {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            timeout = 1;
        }

        long expired = System.currentTimeMillis() + timeout;
        long finalTimeout = timeout;
        thdf.submitCancelablePeriodicTask(new CancelablePeriodicTask(completion) {
            @Override
            public boolean run() {
                try {
                    syncJsonPost(url, "", Void.class, TimeUnit.SECONDS, 2);
                    logger.debug(String.format("successfully echo %s", url));
                    completion.success();
                    return true;
                } catch (Throwable t) {
                    long now = System.currentTimeMillis();

                    String info = String.format("still unable to echo %s, remaining %sms to timeout. %s", url, now - expired, t.getMessage());
                    logger.debug(info);

                    if (now > expired) {
                        completion.fail(operr(ORG_ZSTACK_CORE_REST_10012, "unable to echo %s in %sms", url, finalTimeout));
                        return true;
                    }
                }

                return false;
            }

            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.MILLISECONDS;
            }

            @Override
            public long getInterval() {
                return interval;
            }

            @Override
            public String getName() {
                return "RESTFacade echo";
            }
        });
    }

    @Override
    public Map<String, HttpCallStatistic> getStatistics() {
        return statistics;
    }

    @Override
    public <T> void registerSyncHttpCallHandler(String path, final Class<T> objectType, final SyncHttpCallHandler<T> handler) {
        HttpCallHandlerWrapper wrapper = httpCallhandlers.get(path);
        if (wrapper != null) {
            throw new CloudRuntimeException(String.format("duplicate SyncHttpCallHandler[%s, %s] for the command path[%s]", wrapper.getHandler().getClass(),
                    handler.getClass(), path));
        }

        wrapper = new HttpCallHandlerWrapper() {
            @Override
            public String handle(HttpEntity<String> entity) {
                T cmd = JSONObjectUtil.toObject(entity.getBody(), objectType);
                return handler.handleSyncHttpCall(cmd);
            }

            @Override
            public HttpCallHandler getHandler() {
                return handler;
            }
        };

        httpCallhandlers.put(path, wrapper);
    }

    @Override
    public String getBaseUrl() {
        return baseUrl;
    }

    @Override
    public String getSendCommandUrl() {
        return sendCommandUrl;
    }

    @Override
    public String getCallbackUrl() {
        return callbackUrl;
    }

    @Override
    public String getHostName() {
        return callbackHostName;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public String makeUrl(String path) {
        UriComponentsBuilder ub = UriComponentsBuilder.fromHttpUrl(baseUrl);
        ub.path(path);
        return ub.build().toUriString();
    }

    @Override
    public Runnable installBeforeAsyncJsonPostInterceptor(BeforeAsyncJsonPostInterceptor interceptor) {
        synchronized (interceptors) {
            interceptors.add(interceptor);
        }

        return () -> {
            synchronized (interceptors) {
                interceptors.remove(interceptor);
            }
        };
    }
}
