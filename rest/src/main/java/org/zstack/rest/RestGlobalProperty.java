package org.zstack.rest;

import org.zstack.core.GlobalProperty;
import org.zstack.core.GlobalPropertyDefinition;

/**
 * Created by xing5 on 2016/12/9.
 */
@GlobalPropertyDefinition
public class RestGlobalProperty {
    @GlobalProperty(name="RestServer.maxCachedApiResults", defaultValue = "2000")
    public static int MAX_CACHED_API_RESULTS;

    /**
     * When set RestServer.maskSensitiveInfo to true, sensitive info will be
     * masked see @NoLogging.
     *
     * Set default value as false to keep back-compatible to avoid breaking users who
     * depend on plaintext API result
     */
    @GlobalProperty(name="RestServer.maskSensitiveInfo", defaultValue = "false")
    public static boolean MASK_SENSITIVE_INFO;
}
