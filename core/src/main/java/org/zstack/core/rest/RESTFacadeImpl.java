package org.zstack.core.rest;

import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.MessageCommandRecorder;
import org.zstack.core.Platform;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.retry.Retry;
import org.zstack.core.retry.RetryCondition;
import org.zstack.core.thread.AsyncThread;
import org.zstack.core.thread.CancelablePeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.thread.ThreadFacadeImpl.TimeoutTaskReceipt;
import org.zstack.core.timeout.ApiTimeoutManager;
import org.zstack.core.validation.ValidationFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.rest.*;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.ExceptionDSL;
import org.zstack.utils.IptablesUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class RESTFacadeImpl implements RESTFacade {
    private static final CLogger logger = Utils.getLogger(RESTFacadeImpl.class);
    
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private ApiTimeoutManager timeoutMgr;
    @Autowired
    private ValidationFacade vf;

    private String hostname;
    private int port = 8080;
    private String path;
    private String callbackUrl;
    private RestTemplate template;
    private String baseUrl;
    private String sendCommandUrl;

    private Map<String, HttpCallStatistic> statistics = new ConcurrentHashMap<String, HttpCallStatistic>();
    private Map<String, HttpCallHandlerWrapper> httpCallhandlers = new ConcurrentHashMap<String, HttpCallHandlerWrapper>();
    private List<BeforeAsyncJsonPostInterceptor> interceptors = new ArrayList<BeforeAsyncJsonPostInterceptor>();

    private interface AsyncHttpWrapper {
        void fail(ErrorCode err);

        void success(HttpEntity<String> responseEntity);
    }

    private interface HttpCallHandlerWrapper {
        String handle(HttpEntity<String> entity);

        HttpCallHandler getHandler();
    }

    private Map<String, AsyncHttpWrapper> wrappers = new ConcurrentHashMap<String, AsyncHttpWrapper>();

    void init() {
        IptablesUtils.insertRuleToFilterTable(String.format("-A INPUT -p tcp -m state --state NEW -m tcp --dport %s -j ACCEPT", port));

        String hname = null;
        if ("AUTO".equals(hostname)) {
            hname = Platform.getManagementServerIp();
        } else {
            hname = hostname;
        }

        String url;
        if ("".equals(path) || path == null) {
            url = String.format("http://%s:%s", hname, port);
        } else {
            url = String.format("http://%s:%s/%s", hname, port, path);
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
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setReadTimeout(CoreGlobalProperty.REST_FACADE_READ_TIMEOUT);
        factory.setConnectTimeout(CoreGlobalProperty.REST_FACADE_CONNECT_TIMEOUT);
        template = new RestTemplate(factory);
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
    public void asyncJsonPost(String url, Object body, AsyncRESTCallback callback, TimeUnit unit, long timeout) {
        for (BeforeAsyncJsonPostInterceptor ic : interceptors) {
            ic.beforeAsyncJsonPost(url, body, unit, timeout);
        }

        // for unit test finding invocation chain
        MessageCommandRecorder.record(body.getClass());
        String bodyStr = JSONObjectUtil.toJsonString(body);
        asyncJsonPost(url, bodyStr, callback, unit, timeout);
    }

    @Override
    public void asyncJsonPost(final String url, final String body, final AsyncRESTCallback callback, final TimeUnit unit, final long timeout) {
        for (BeforeAsyncJsonPostInterceptor ic : interceptors) {
            ic.beforeAsyncJsonPost(url, body, unit, timeout);
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
        AsyncHttpWrapper wrapper = new AsyncHttpWrapper() {
            AtomicBoolean called = new AtomicBoolean(false);

            final AsyncHttpWrapper self = this;
            TimeoutTaskReceipt timeoutTaskReceipt = thdf.submitTimeoutTask(new Runnable() {
                @Override
                public void run() {
                    self.fail(errf.stringToTimeoutError(
                            String.format("[Async Http Timeout] url: %s, timeout after %s[%s], command: %s",
                                    url, timeout, unit.toString(), body)
                    ));
                }
            }, unit, timeout);

            private void cancelTimeout() {
                timeoutTaskReceipt.cancel();
            }

            public void fail(ErrorCode err) {
                if (!called.compareAndSet(false, true)) {
                    return;
                }

                wrappers.remove(taskUuid);
                if (!SysErrors.TIMEOUT.toString().equals(err.getCode())) {
                    cancelTimeout();
                }

                callback.fail(err);
            }

            @Override
            @AsyncThread
            public void success(HttpEntity<String> responseEntity) {
                if (!called.compareAndSet(false, true)) {
                    return;
                }

                if (CoreGlobalProperty.PROFILER_HTTP_CALL) {
                    HttpCallStatistic stat = statistics.get(url);
                    stat.addStatistic(System.currentTimeMillis() - finalStime);
                }

                wrappers.remove(taskUuid);
                cancelTimeout();

                if (logger.isTraceEnabled()) {
                    logger.trace(String.format("[http response(url: %s)] %s", url, responseEntity.getBody()));
                }

                if (callback instanceof JsonAsyncRESTCallback) {
                    JsonAsyncRESTCallback jcallback = (JsonAsyncRESTCallback)callback;
                    Object obj = JSONObjectUtil.toObject(responseEntity.getBody(), jcallback.getReturnClass());
                    try {
                        ErrorCode err = vf.validateErrorByErrorCode(obj);
                        if (err != null) {
                            logger.warn(String.format("error response that causes validation failure: %s", responseEntity.getBody()));
                            jcallback.fail(err);
                        } else {
                            jcallback.success(obj);
                        }
                    } catch (Throwable t) {
                        logger.warn(t.getMessage(), t);
                        callback.fail(errf.throwableToInternalError(t));
                    }
                } else {
                    callback.success(responseEntity);
                }
            }
        };

        try {
            wrappers.put(taskUuid, wrapper);
            HttpHeaders requestHeaders = new HttpHeaders();
            requestHeaders.setContentType(MediaType.APPLICATION_JSON);
            requestHeaders.setContentLength(body.length());
            requestHeaders.set(RESTConstant.TASK_UUID, taskUuid);
            requestHeaders.set(RESTConstant.CALLBACK_URL, callbackUrl);
            HttpEntity<String> req = new HttpEntity<String>(body, requestHeaders);
            if (logger.isTraceEnabled()) {
                logger.trace(String.format("json post[%s], %s", url, req.toString()));
            }

            ResponseEntity<String> rsp = new Retry<ResponseEntity<String>>() {
                @Override
                @RetryCondition(onExceptions = {IOException.class, RestClientException.class})
                protected ResponseEntity<String> call() {
                    return template.exchange(url, HttpMethod.POST, req, String.class);
                }
            }.run();

            if (rsp.getStatusCode() != org.springframework.http.HttpStatus.OK) {
                String err = String.format("http status: %s, response body:%s", rsp.getStatusCode().toString(), rsp.getBody());
                logger.warn(err);
                wrapper.fail(errf.instantiateErrorCode(SysErrors.HTTP_ERROR, err));
            }
        } catch (Throwable e) {
            logger.warn(String.format("Unable to post to %s", url), e);
            wrapper.fail(ExceptionDSL.isCausedBy(e, IOException.class) ? errf.instantiateErrorCode(SysErrors.IO_ERROR, e.getMessage()) : errf.throwableToInternalError(e));
        }
    }

    @Override
    public void asyncJsonPost(String url, Object body, AsyncRESTCallback callback) {
        Long timeout = timeoutMgr.getTimeout(body.getClass());
        asyncJsonPost(url, body, callback, TimeUnit.MILLISECONDS, timeout == null ? 300000 : timeout);
    }

    @Override
    public void asyncJsonPost(String url, String body, AsyncRESTCallback callback) {
        asyncJsonPost(url, body, callback, TimeUnit.SECONDS, 300);
    }

    @Override
    public HttpEntity<String> httpServletRequestToHttpEntity(HttpServletRequest req) {
        try {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = req.getReader().readLine()) != null) {
                sb.append(line);
            }
            req.getReader().close();
            
            HttpHeaders header = new HttpHeaders();
            for (Enumeration e = req.getHeaderNames() ; e.hasMoreElements() ;) {
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
        if (body != null) {
            MessageCommandRecorder.record(body.getClass());
        }

        return syncJsonPost(url, body == null ? null : JSONObjectUtil.toJsonString(body), returnClass);
    }

    @Override
    public <T> T syncJsonPost(String url, String body, Class<T> returnClass) {
        return syncJsonPost(url, body, null, returnClass);
    }

    @Override
    public <T> T syncJsonPost(String url, String body, Map<String, String> headers, Class<T> returnClass) {
        body = body == null ? "" : body;

        HttpHeaders requestHeaders = new HttpHeaders();
        if (headers != null) {
            requestHeaders.setAll(headers);
        }
        requestHeaders.setContentType(MediaType.APPLICATION_JSON);
        requestHeaders.setContentLength(body.length());
        HttpEntity<String> req = new HttpEntity<String>(body, requestHeaders);
        if (logger.isTraceEnabled()) {
            logger.trace(String.format("json post[%s], %s", url, req.toString()));
        }


        ResponseEntity<String> rsp = new Retry<ResponseEntity<String>>() {
            @Override
            @RetryCondition(onExceptions = {IOException.class, RestClientException.class})
            protected ResponseEntity<String> call() {
                return template.exchange(url, HttpMethod.POST, req, String.class);
            }
        }.run();

        if (rsp.getStatusCode() != org.springframework.http.HttpStatus.OK) {
            throw new OperationFailureException(errf.stringToOperationError(
                    String.format("failed to post to %s, status code: %s, response body: %s", url, rsp.getStatusCode(), rsp.getBody())
            ));
        }
        
        if (rsp.getBody() != null && returnClass != Void.class) {

            if (logger.isTraceEnabled()) {
                logger.trace(String.format("[http response(url: %s)] %s", url, rsp.getBody()));
            }

            return JSONObjectUtil.toObject(rsp.getBody(), returnClass);
        } else {
            return null;
        }
    }

    @Override
    public void echo(String url, Completion callback) {
        echo(url, callback, TimeUnit.SECONDS.toMillis(1), TimeUnit.SECONDS.toMillis(30));
    }
    
    @Override
    public void echo(final String url, final Completion completion, final long interval, final long timeout) {
        class Echo implements CancelablePeriodicTask {
            private long count;

            Echo() {
                this.count = timeout / interval;
                DebugUtils.Assert(count!=0, String.format("invalid timeout[%s], interval[%s]", timeout, interval));
            }

            @Override
            public boolean run() {
                try {
                    syncJsonPost(url, "", Void.class);
                    logger.debug(String.format("successfully echo %s", url));
                    completion.success();
                    return true;
                } catch (Exception e) {
                    String info = String.format("still unable to echo %s, will try %s times. %s", url, count, e.getMessage());
                    logger.debug(info);
                    if (--count <= 0) {
                        String err = String.format("unable to echo %s in %sms", url, timeout);
                        completion.fail(errf.stringToOperationError(err));
                        return true;
                    } else {
                        return false;
                    }
                }
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
        }

        thdf.submitCancelablePeriodicTask(new Echo());
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
    public String makeUrl(String path) {
        UriComponentsBuilder ub = UriComponentsBuilder.fromHttpUrl(baseUrl);
        ub.path(path);
        return ub.build().toUriString();
    }

    @Override
    public void installBeforeAsyncJsonPostInterceptor(BeforeAsyncJsonPostInterceptor interceptor) {
        interceptors.add(interceptor);
    }
}
