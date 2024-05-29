package org.zstack.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DomainUtils {
    private static final String regex = "(https?://)?([^:/]+)(:\\d+)?";

    public static String getDomain(String url) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(2);
        }
        return "";
    }

    public static Boolean curl(String url) {
        ShellResult rst = ShellUtils.runAndReturn(String.format("curl %s", url), true);
        return rst.getRetCode() == 0;
    }

    public static Boolean ping(String domain) {
        ShellResult rst = ShellUtils.runAndReturn(String.format("ping %s -c 3 -W 2", domain), true);
        return rst.getRetCode() == 0;
    }
}
