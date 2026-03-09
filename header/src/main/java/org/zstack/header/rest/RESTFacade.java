package org.zstack.header.rest;

import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.util.Timeout;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.zstack.header.core.Completion;

import javax.net.ssl.SSLContext;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.List;
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

    void asyncJson(final String url, final String body, Map<String, String> headers, HttpMethod method, final AsyncRESTCallback callback, final TimeUnit unit, final long timeout);

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

    <T> T syncJsonPut(String url, String body, Map<String, String> headers, Class<T> returnClass);

    <T> T syncJsonPut(String url, String body, Map<String, String> headers, Class<T> returnClass, TimeUnit unit, long timeout);

    <T> RestHttp<T> http(Class<T> returnClass);

    ResponseEntity<String> syncRawJson(String url, HttpEntity<String> req, HttpMethod method, TimeUnit unit, long timeout);

    HttpHeaders syncHead(String url);

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

    int getPort();

    String makeUrl(String path);

    Runnable installBeforeAsyncJsonPostInterceptor(BeforeAsyncJsonPostInterceptor interceptor);

    static void setMessageConverter(List<HttpMessageConverter<?>> converters) {
        StringHttpMessageConverter stringHttpMessageConverter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
        stringHttpMessageConverter.setWriteAcceptCharset(true);

        for (int i = 0; i < converters.size(); i++) {
            if (converters.get(i) instanceof StringHttpMessageConverter) {
                converters.remove(i);
                converters.add(i, stringHttpMessageConverter);
                break;
            }
        }
    }

    // timeout are in milliseconds
    static TimeoutRestTemplate createRestTemplate(int readTimeout, int connectTimeout) {
        RequestConfig requestConfig = RequestConfig.custom()
                .setResponseTimeout(Timeout.ofMilliseconds(readTimeout))
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(connectTimeout * 2))
                .build();

        SSLContext sslContext = DefaultSSLVerifier.getSSLContext(DefaultSSLVerifier.trustAllCerts);

        var httpClientBuilder = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig);

        if (sslContext != null) {
            HttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
                    .setSSLSocketFactory(SSLConnectionSocketFactoryBuilder.create()
                            .setSslContext(sslContext)
                            .setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                            .build())
                    .build();
            httpClientBuilder.setConnectionManager(cm);
        }

        HttpComponentsClientHttpRequestFactory factory = new TimeoutHttpComponentsClientHttpRequestFactory();
        factory.setHttpClient(httpClientBuilder.build());
        factory.setConnectTimeout(connectTimeout);

        TimeoutRestTemplate template = new TimeoutRestTemplate(factory);
        setMessageConverter(template.getMessageConverters());

        return template;
    }
}
