package org.zstack.utils;

import org.springframework.web.util.UriComponentsBuilder;

public class URLBuilder {
    public static String buildUrl(String scheme, String host, int port, String...paths) {
        UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
        builder.scheme(scheme).host(host).port(port);
        for (String p : paths) {
            builder.path(p);
        }
        return builder.build().toUriString();
    }
    
    public static String buildHttpUrl(String host, int port, String...paths) {
        return buildUrl("http", host, port, paths);
    }
    
    public static String buildSslHttpUrl(String host, int port, String...paths) {
        return buildUrl("https", host, port, paths);
    }
    
    public static String buildUrlFromBase(String baseUrl, String...paths) {
        UriComponentsBuilder ub = UriComponentsBuilder.fromHttpUrl(baseUrl);
        for (String p : paths) {
            ub.path(p);
        }
        return ub.build().toString();
    }
}
