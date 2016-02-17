package org.zstack.core.timeout;

import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.APIMessage;
import org.zstack.utils.BeanUtils;
import org.zstack.utils.BootErrorLog;
import org.zstack.utils.StringDSL;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by frank on 2/17/2016.
 */
public class TimeoutManagerImpl implements TimeoutManager {
    private static final CLogger logger = Utils.getLogger(TimeoutManagerImpl.class);

    private class ApiTimeout {
        List<Class> relatives;
        long timeout;
    }

    private Map<Class, ApiTimeout> apiTimeouts = new HashMap<Class, ApiTimeout>();

    void init() {
        try {
            collectTimeout();
            collectTimeoutForDerivedApi();
        } catch (RuntimeException e) {
            new BootErrorLog().write(e.getMessage());
            throw e;
        }
    }

    private void collectTimeoutForDerivedApi() {
        List<Class> allApis = BeanUtils.scanClassByType("org.zstack", APIMessage.class);
        Set<Class> origin = new HashSet<Class>();
        origin.addAll(apiTimeouts.keySet());

        for (Class clz : allApis) {
            for (Class ac : origin) {
                if (clz != ac && ac.isAssignableFrom(clz)) {
                    ApiTimeout at = apiTimeouts.get(ac);
                    apiTimeouts.put(clz, at);

                    if (logger.isTraceEnabled()) {
                        logger.trace(String.format("configure timeout for API[%s]:" +
                                "\nrelatives: %s" +
                                "\ntimeout: %sms", clz, at.relatives, at.timeout));
                    }
                }
            }
        }
    }

    private void collectTimeout() {
        for (final String key: System.getProperties().stringPropertyNames()) {
            if (!key.startsWith("ApiTimeout.")) {
                continue;
            }

            String apiName = StringDSL.stripStart(key, "ApiTimeout.").trim();
            Class apiClz;
            String ERROR_INFO = "The configuration must be in format of \n" +
                    "   ApiTimeout.full_api_class_name = full_sub_message_names_or_commands; timeout(e.g. 60s, 1m, 1h)";
            try {
                apiClz = Class.forName(apiName);
            } catch (ClassNotFoundException e) {
                throw new CloudRuntimeException(String.format("Invalid API timeout configuration[invalid key: %s], %s. %s",
                        key, e.getMessage(), ERROR_INFO));
            }

            String value = System.getProperty(key);
            if (!value.contains(";")) {
                throw new CloudRuntimeException(String.format("Invalid API timeout configuration[%s=%s], no ';' found. %s",
                        key, value, ERROR_INFO));
            }

            String[] p = value.split(";");
            if (p.length > 2) {
                throw new CloudRuntimeException(String.format("Invalid API timeout configuration[%s=%s], multiple ';' found. %s",
                        key, value, ERROR_INFO));
            }

            String relativeNames = p[0];
            String timeout = p[1];

            ApiTimeout at = new ApiTimeout();
            try {
                at.relatives = parseRelatives(relativeNames);
            } catch (ClassNotFoundException e) {
                throw new CloudRuntimeException(String.format("Invalid API timeout configuration[%s=%s], class %s not found. %s", key, value,
                        e.getMessage(), ERROR_INFO));
            }
            try {
                at.timeout = parseTimeout(timeout.trim());
            } catch (NumberFormatException e) {
                throw new CloudRuntimeException(String.format("Invalid API timeout configuration[%s=%s], %s. %s", key, value,
                        String.format("%s is not a valid time number", timeout), ERROR_INFO), e);
            }

            apiTimeouts.put(apiClz, at);
            if (logger.isTraceEnabled()) {
                logger.trace(String.format("configure timeout for API[%s]:" +
                        "\nrelatives: %s" +
                        "\ntimeout: %sms", apiClz, at.relatives, at.timeout));
            }
        }
    }

    private long parseTimeout(String timeout) {
        if ("5m".equals(timeout)) {
            // optimization as the most of default timeout are 5 minutes
            return 300000;
        }

        try {
            return Long.valueOf(timeout);
        } catch (NumberFormatException e) {
            if (timeout.endsWith("s")) {
                timeout = StringDSL.stripEnd(timeout, "s");
                return TimeUnit.SECONDS.toMillis(Long.valueOf(timeout));
            } else if (timeout.endsWith("S")) {
                timeout = StringDSL.stripEnd(timeout, "S");
                return TimeUnit.SECONDS.toMillis(Long.valueOf(timeout));
            } else if (timeout.endsWith("m")) {
                timeout = StringDSL.stripEnd(timeout, "m");
                return TimeUnit.MINUTES.toMillis(Long.valueOf(timeout));
            } else if (timeout.endsWith("M")) {
                timeout = StringDSL.stripEnd(timeout, "M");
                return TimeUnit.MINUTES.toMillis(Long.valueOf(timeout));
            } else if (timeout.endsWith("h")) {
                timeout = StringDSL.stripEnd(timeout, "h");
                return TimeUnit.HOURS.toMillis(Long.valueOf(timeout));
            } else if (timeout.endsWith("H")) {
                timeout = StringDSL.stripEnd(timeout, "H");
                return TimeUnit.HOURS.toMillis(Long.valueOf(timeout));
            } else if (timeout.endsWith("d")) {
                timeout = StringDSL.stripEnd(timeout, "d");
                return TimeUnit.DAYS.toMillis(Long.valueOf(timeout));
            } else if (timeout.endsWith("D")) {
                timeout = StringDSL.stripEnd(timeout, "D");
                return TimeUnit.DAYS.toMillis(Long.valueOf(timeout));
            } else {
                throw new NumberFormatException();
            }
        }
    }

    private List<Class> parseRelatives(String relativeNames) throws ClassNotFoundException {
        List<Class> classes = new ArrayList<Class>();
        String[] names = relativeNames.split(",");

        for (String name : names) {
            name = name.trim();
            classes.add(Class.forName(name));
        }

        return classes;
    }

    @Override
    public Long getTimeout(Class clz) {
        ApiTimeout at = apiTimeouts.get(clz);
        return at != null ? at.timeout : null;
    }

    @Override
    public Long getTimeout(Class clz, long defaultTimeout) {
        Long t = getTimeout(clz);
        return t == null ? defaultTimeout : t;
    }

    @Override
    public Long getTimeout(Class clz, String defaultTimeout) {
        Long t = getTimeout(clz);
        if (t == null) {
            return parseTimeout(defaultTimeout);
        } else {
            return t;
        }
    }

    @Override
    public Long getTimeout(Class clz, TimeUnit tu) {
        ApiTimeout at = apiTimeouts.get(clz);
        return at != null ? tu.convert(at.timeout, TimeUnit.MILLISECONDS) : null;
    }
}
