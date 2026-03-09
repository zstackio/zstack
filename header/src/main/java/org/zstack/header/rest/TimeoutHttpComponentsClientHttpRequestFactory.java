package org.zstack.header.rest;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.util.Timeout;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;

/**
 * Created by lining on 2017/6/12.
 */
public class TimeoutHttpComponentsClientHttpRequestFactory extends HttpComponentsClientHttpRequestFactory {
    private static final CLogger logger = Utils.getLogger(TimeoutHttpComponentsClientHttpRequestFactory.class);

    private static final ThreadLocal<TimeoutConfig> timeoutConfig = new ThreadLocal<>();

    @Override
    public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {

        ClientHttpRequest request = super.createRequest(uri, httpMethod);

        TimeoutConfig config = timeoutConfig.get();
        if(config == null){
            return request;
        }
        timeoutConfig.remove();

        try {
            // HC5: set per-request config via reflection on the underlying HttpUriRequestBase
            Field httpRequestField = request.getClass().getDeclaredField("httpRequest");
            httpRequestField.setAccessible(true);
            Object httpRequest = httpRequestField.get(request);

            RequestConfig requestConfig = RequestConfig.custom()
                    .setResponseTimeout(Timeout.ofMilliseconds(config.readTimeout))
                    .setConnectionRequestTimeout(Timeout.ofMilliseconds(config.connectTimeout))
                    .build();

            Method setConfigMethod = httpRequest.getClass().getMethod("setConfig", RequestConfig.class);
            setConfigMethod.invoke(httpRequest, requestConfig);
        }catch (Throwable t){
            logger.warn(String.format("failed to set per-request timeout config: %s", t.getMessage()));
        }

        return request;
    }

    public void setRequestTimeoutConfig(long connectTimeout, long readTimeout){
        if(connectTimeout > Integer.MAX_VALUE){
            logger.warn(String.format("MyRestTemplate connectTimeout[%s] bigger than Integer.MAX_VALUE", connectTimeout));
        }
        if(readTimeout > Integer.MAX_VALUE){
            logger.warn(String.format("MyRestTemplate readTimeout[%s] bigger than Integer.MAX_VALUE", readTimeout));
        }

        timeoutConfig.set(new TimeoutConfig(connectTimeout, readTimeout));
    }

    static class TimeoutConfig {
        public int connectTimeout;
        public int readTimeout;

        public TimeoutConfig(long connectTimeout, long readTimeout) {
            if(connectTimeout > Integer.MAX_VALUE){
                this.connectTimeout = Integer.MAX_VALUE;
            }else{
                this.connectTimeout = (int) connectTimeout;
            }

            if(readTimeout > Integer.MAX_VALUE){
                this.readTimeout = Integer.MAX_VALUE;
            }else {
                this.readTimeout = (int) readTimeout;
            }
        }
    }
}
