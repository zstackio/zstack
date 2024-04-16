package org.zstack.utils;

import javax.servlet.http.HttpServletRequest;
import java.util.function.Predicate;

public class HttpServletRequestUtils {
    public static String getClientIP(HttpServletRequest req) {
        Predicate<String> isEmptyOrUnknown = s -> s == null || s.isEmpty() || "unknown".equalsIgnoreCase(s);

        String ipAddress = req.getHeader("X-Forwarded-For");
        if (isEmptyOrUnknown.test(ipAddress)) {
            ipAddress = req.getHeader("Proxy-Client-IP");
        }
        if (isEmptyOrUnknown.test(ipAddress)) {
            ipAddress = req.getHeader("WL-Proxy-Client-IP");
        }
        if (isEmptyOrUnknown.test(ipAddress)) {
            ipAddress = req.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (isEmptyOrUnknown.test(ipAddress)) {
            ipAddress = req.getHeader("HTTP_X_FORWARDED");
        }
        if (isEmptyOrUnknown.test(ipAddress)) {
            ipAddress = req.getHeader("HTTP_X_CLUSTER_CLIENT_IP");
        }
        if (isEmptyOrUnknown.test(ipAddress)) {
            ipAddress = req.getHeader("HTTP_CLIENT_IP");
        }
        if (isEmptyOrUnknown.test(ipAddress)) {
            ipAddress = req.getHeader("HTTP_FORWARDED_FOR");
        }
        if (isEmptyOrUnknown.test(ipAddress)) {
            ipAddress = req.getHeader("HTTP_FORWARDED");
        }
        if (isEmptyOrUnknown.test(ipAddress)) {
            ipAddress = req.getHeader("X-Real-IP");
        }
        if (isEmptyOrUnknown.test(ipAddress)) {
            ipAddress = req.getRemoteAddr();
        }
        return ipAddress;
    }

    public static String getClientBrowser(HttpServletRequest req) {
        return req.getHeader("User-Agent");
    }
}
