package org.zstack.sdnController.h3cVcfc;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.web.util.UriComponentsBuilder;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.MessageCommandRecorder;
import org.zstack.header.rest.RESTFacade;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class H3cVcfcHttpClient<T> {
    @Autowired
    private RESTFacade restf;
    private TimeUnit unit;
    private Long timeout;
    private Class<T> responseClass;

    public H3cVcfcHttpClient(Class<T> rspClz) {
        this.responseClass = rspClz;
        this.unit = TimeUnit.MILLISECONDS;
        this.timeout = H3cVcfcSdnControllerGlobalProperty.H3C_CONTROLLER_TIMEOUT;
    }

    private String buildUrl(String ip, String path) {
        UriComponentsBuilder ub = UriComponentsBuilder.newInstance();
        ub.scheme(H3cVcfcSdnControllerGlobalProperty.H3C_CONTROLLER_SCHEME);
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            ub.host("localhost");
            ub.port(8989);
        } else {
            ub.host(ip);
            ub.port(H3cVcfcSdnControllerGlobalProperty.H3C_CONTROLLER_PORT);
        }

        ub.path(path);
        return ub.build().toUriString();
    }

    public T syncCall(String action, String ip, String url, Object body, Map<String, String>  headers) {
        String httpBody = null;
        // for unit test finding invocation chain
        if (body != null) {
            MessageCommandRecorder.record(body.getClass());
            httpBody = JSONObjectUtil.toJsonString(body);
        } else {
            return null;
        }

        switch (action) {
            case "GET": {
                return restf.syncJsonGet(buildUrl(ip, url), httpBody, headers, responseClass);
            }
            case "DELETE": {
                return restf.syncJsonDelete(buildUrl(ip, url), httpBody, headers, responseClass);
            }
            default: {
                if (url.equals(H3cVcfcCommands.H3C_VCFC_L2_NETWORKS)) {
                    httpBody = httpBody.replace("\"network_type\"", "\"provider:network_type\"")
                            .replace("\"original_network_type\"", "\"provider:original_network_type\"")
                            .replace("\"domain\"", "\"provider:domain\"")
                            .replace("\"segmentation_id\"", "\"provider:segmentation_id\"");
                }
                return restf.syncJsonPost(buildUrl(ip, url), httpBody, headers, responseClass);
            }
        }

    }
}
