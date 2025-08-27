package org.zstack.compute.vm;


import org.apache.commons.lang.StringUtils;
import org.zstack.header.apimediator.ApiMessageInterceptionException;

import java.util.regex.Pattern;

import static org.zstack.core.Platform.argerr;

public class VmHostnameUtils {
    private static final Pattern HOSTNAME_PATTERN =
            Pattern.compile("^(?=.{1,255}$)([a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)(\\.[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$");

    public static void validateHostname(String hostname) {
        if (StringUtils.isEmpty(hostname)) {
            return;
        }

        boolean isValid = HOSTNAME_PATTERN.matcher(hostname).matches();
        if (!isValid) {
            throw new ApiMessageInterceptionException(argerr("%s is not a valid hostname", hostname));
        }
    }

}
