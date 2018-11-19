package org.zstack.header.rest;

import org.springframework.http.HttpEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.zstack.header.core.Completion;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public interface RESTFacade {
    void asyncJsonPost(String url, Object body, Map<String, String> headers, AsyncRESTCallback callback, TimeUnit unit, long timeout);

    void asyncJsonPost(String url, Object body, AsyncRESTCallback callback, TimeUnit unit, long timeout);

    void asyncJsonPost(String url, String body, AsyncRESTCallback callback, TimeUnit unit, long timeout);

    void asyncJsonPost(String url, String body, Map<String, String> headers, AsyncRESTCallback callback, TimeUnit unit, long timeout);

    void asyncJsonPost(String url, Object body, Map<String, String> headers, AsyncRESTCallback callback);

    void asyncJsonPost(String url, Object body, AsyncRESTCallback callback);

    void asyncJsonPost(String url, String body, AsyncRESTCallback callback);
    void asyncJsonDelete(String url, String body, Map<String, String> headers, AsyncRESTCallback callback, TimeUnit unit, long timeout);
    void asyncJsonGet(String url, String body, Map<String, String> headers, AsyncRESTCallback callback, TimeUnit unit, long timeout);

    <T> T syncJsonPost(String url, Object body, Class<T> returnClass);

    <T> T syncJsonPost(String url, Object body, Class<T> returnClass, TimeUnit unit, long timeout);

    <T> T syncJsonPost(String url, String body, Class<T> returnClass);

    <T> T syncJsonPost(String url, String body, Map<String, String> headers, Class<T> returnClass);

    <T> T syncJsonPost(String url, String body, Map<String, String> headers, Class<T> returnClass, TimeUnit unit, long timeout);

    /**
     * ZStack's agents only use sync/async post method
     * delete and get methods used for outsides plugins
     */
    <T> T syncJsonDelete(String url, String body, Map<String, String> headers, Class<T> returnClass);

    <T> T syncJsonDelete(String url, String body, Map<String, String> headers, Class<T> returnClass, TimeUnit unit, long timeout);

    <T> T syncJsonGet(String url, String body, Map<String, String> headers, Class<T> returnClass);

    <T> T syncJsonGet(String url, String body, Map<String, String> headers, Class<T> returnClass, TimeUnit unit, long timeout);

    HttpEntity<String> httpServletRequestToHttpEntity(HttpServletRequest req);

    RestTemplate getRESTTemplate();

    void echo(String url, Completion callback);

    void echo(String url, Completion callback, long inverval, long timeout);

    Map<String, HttpCallStatistic> getStatistics();

    <T> void registerSyncHttpCallHandler(String path, Class<T> objectType, SyncHttpCallHandler<T> handler);

    String getBaseUrl();

    String getSendCommandUrl();

    String getCallbackUrl();

    String getHostName();

    String makeUrl(String path);

    Runnable installBeforeAsyncJsonPostInterceptor(BeforeAsyncJsonPostInterceptor interceptor);

    // timeout are in milliseconds
    static TimeoutRestTemplate createRestTemplate(int readTimeout, int connectTimeout) {
        HttpComponentsClientHttpRequestFactory factory = new TimeoutHttpComponentsClientHttpRequestFactory();
        factory.setReadTimeout(readTimeout);
        factory.setConnectTimeout(connectTimeout);
        TimeoutRestTemplate template = new TimeoutRestTemplate(factory);

        StringHttpMessageConverter stringHttpMessageConverter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
        stringHttpMessageConverter.setWriteAcceptCharset(true);
        for (int i = 0; i < template.getMessageConverters().size(); i++) {
            if (template.getMessageConverters().get(i) instanceof StringHttpMessageConverter) {
                template.getMessageConverters().remove(i);
                template.getMessageConverters().add(i, stringHttpMessageConverter);
                break;
            }
        }

        return template;
    }
}
