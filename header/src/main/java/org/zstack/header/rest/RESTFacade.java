package org.zstack.header.rest;

import org.springframework.http.HttpEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.zstack.header.core.Completion;

import javax.servlet.http.HttpServletRequest;
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

    <T> T syncJsonPost(String url, Object body, Class<T> returnClass);

    <T> T syncJsonPost(String url, String body, Class<T> returnClass);

    <T> T syncJsonPost(String url, String body, Map<String, String> headers, Class<T> returnClass);

    HttpEntity<String> httpServletRequestToHttpEntity(HttpServletRequest req);

    RestTemplate getRESTTemplate();

    void echo(String url, Completion callback);

    void echo(String url, Completion callback, long inverval, long timeout);

    Map<String, HttpCallStatistic> getStatistics();

    <T> void registerSyncHttpCallHandler(String path, Class<T> objectType, SyncHttpCallHandler<T> handler);

    String getBaseUrl();

    String getSendCommandUrl();

    String getCallbackUrl();

    String makeUrl(String path);

    void installBeforeAsyncJsonPostInterceptor(BeforeAsyncJsonPostInterceptor interceptor);

    // timeout are in milliseconds
    static RestTemplate createRestTemplate(int readTimeout, int connectTimeout) {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setReadTimeout(readTimeout);
        factory.setConnectTimeout(connectTimeout);
        return new RestTemplate(factory);
    }
}
