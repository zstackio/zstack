package org.zstack.utils;

import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.function.Predicate;

public class HttpServletRequestUtils {
    public static String getClientIP(HttpServletRequest req) {
        Predicate<String> isEmptyOrUnknown = s -> s == null || s.isEmpty() || "unknown".equalsIgnoreCase(s);

        String ipAddress = req.getHeader("X-Forwarded-For");
        if (!isEmptyOrUnknown.test(ipAddress)) {
            ipAddress = ipAddress.split(",")[0].trim();
        }
        if (isEmptyOrUnknown.test(ipAddress)) {
            ipAddress = req.getHeader("X-Real-IP");
        }
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
            ipAddress = req.getRemoteAddr();
        }
        return ipAddress;
    }

    public static String getClientBrowser(HttpServletRequest req) {
        String userAgent = req.getHeader("User-Agent");
        if (StringUtils.isBlank(userAgent)) return "";

        userAgent = userAgent.toLowerCase();
        if (userAgent.contains("chrome")) {
            return "Chrome";
        } else if (userAgent.contains("firefox")) {
            return "Firefox";
        } else if (userAgent.contains("edge")) {
            return "Edge";
        } else if (userAgent.contains("trident") || userAgent.contains("msie")) {
            return "IE";
        } else if (userAgent.contains("360")) {
            return "360";
        } else if (userAgent.contains("tencenttraveler")) {
            return "Tencent Traveler";
        } else if (userAgent.contains("metas")) {
            return "Sogou Explorer";
        } else if (userAgent.contains("safari")) {
            return "Safari";
        } else if (userAgent.contains("cli")) {
            return "cli";
        }else {
            return "Other";
        }
    }
}
