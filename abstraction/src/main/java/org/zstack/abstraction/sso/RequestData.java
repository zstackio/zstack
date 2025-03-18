package org.zstack.abstraction.sso;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.MultiValueMap;

public class RequestData {
    public String requestUrl;
    public HttpMethod method;
    public MultiValueMap<String, String> map;
    public HttpHeaders headers;

    public RequestData(String requestUrl, MultiValueMap<String, String> map, HttpMethod method, HttpHeaders headers) {
        this.requestUrl = requestUrl;
        this.method = method;
        this.map = map;
        this.headers = headers;
    }
}
