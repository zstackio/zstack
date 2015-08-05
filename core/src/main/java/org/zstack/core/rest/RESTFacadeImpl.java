package org.zstack.core.rest;

import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.ThreadFacadeImpl.TimeoutTaskReceipt;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.core.thread.AsyncThread;
import org.zstack.core.thread.CancelablePeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.validation.ValidationFacade;
import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.rest.*;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.IptablesUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
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
    private ValidationFacade vf;

    private String hostname;
    private int port = 8080;
    private String path;
    private String callbackUrl;
    private RestTemplate template;

    private Map<String, HttpCallStatistic> statistics = new ConcurrentHashMap<String, HttpCallStatistic>();

    private interface AsyncHttpWrapper {
        void fail(ErrorCode err);

        void success(HttpEntity<String> responseEntity);
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
        ub.path(RESTConstant.BASE_PATH);
        ub.path(RESTConstant.CALLBACK_PATH);
        callbackUrl = ub.build().toUriString();
        logger.debug(String.format("RESTFacade built callback url: %s", callbackUrl));
        template = new RestTemplate();
    }

    void notifyCallback(HttpServletRequest req, HttpServletResponse rsp) {
        String taskUuid = req.getHeader(RESTConstant.TASK_UUID);
        try {
            HttpEntity<String> entity = this.httpServletRequestToHttpEntity(req);
            if (taskUuid == null) {
                rsp.sendError(HttpStatus.SC_NOT_FOUND, "No 'taskUuid' found in header");
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
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
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
        String bodyStr = JSONObjectUtil.toJsonString(body);
        asyncJsonPost(url, bodyStr, callback, unit, timeout);
    }

    @Override
    public void asyncJsonPost(final String url, final String body, final AsyncRESTCallback callback, final TimeUnit unit, final long timeout) {
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
            ResponseEntity<String> rsp = template.exchange(url, HttpMethod.POST, req, String.class);
            if (rsp.getStatusCode() != org.springframework.http.HttpStatus.OK) {
                String err = String.format("http status: %s, response body:%s", rsp.getStatusCode().toString(), rsp.getBody());
                logger.warn(err);
                wrapper.fail(errf.stringToOperationError(err));
            }
        } catch (Throwable e) {
            logger.warn(String.format("Unable to post to %s", url), e);
            wrapper.fail(errf.throwableToInternalError(e));
        }
    }

    @Override
    public void asyncJsonPost(String url, Object body, AsyncRESTCallback callback) {
        asyncJsonPost(url, body, callback, TimeUnit.SECONDS, 300);
    }

    @Override
    public void asyncJsonPost(String url, String body, AsyncRESTCallback callback) {
        asyncJsonPost(url, body, callback, TimeUnit.SECONDS, 300);

    }

    @Override
    public HttpEntity<String> httpServletRequestToHttpEntity(HttpServletRequest req) {
        try {
            StringBuilder sb = new StringBuilder();
            String line = "";
            while ((line = req.getReader().readLine()) != null) {
                sb.append(line);
            }
            req.getReader().close();
            
            HttpHeaders header = new HttpHeaders();
            for (Enumeration e = req.getHeaderNames() ; e.hasMoreElements() ;) {
                String name = e.nextElement().toString();
                header.add(name, req.getHeader(name));
            }
            
            HttpEntity<String> entity = new HttpEntity<String>(sb.toString(), header);
            return entity;
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
        String jstr = JSONObjectUtil.toJsonString(body);
        return syncJsonPost(url, jstr, returnClass);
    }

    @Override
    public <T> T syncJsonPost(String url, String body, Class<T> returnClass) {
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.APPLICATION_JSON);
        requestHeaders.setContentLength(body.length());
        HttpEntity<String> req = new HttpEntity<String>(body, requestHeaders);
        logger.trace(String.format("json post[%s], %s", url, req.toString()));
        ResponseEntity<String> rsp = template.exchange(url, HttpMethod.POST, req, String.class);
        if (rsp.getStatusCode() != org.springframework.http.HttpStatus.OK) {
            String err = String.format("http status: %s, response body:%s", rsp.getStatusCode().toString(), rsp.getBody());
            throw new RestClientException(err);
        }
        
        if (rsp.getBody() != null && returnClass != Void.class) {
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
}
