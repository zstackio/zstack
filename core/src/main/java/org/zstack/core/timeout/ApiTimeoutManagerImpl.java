package org.zstack.core.timeout;

import org.reflections.Reflections;
import org.zstack.core.Platform;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.*;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by frank on 2/17/2016.
 */
public class ApiTimeoutManagerImpl implements ApiTimeoutManager {
    private static final CLogger logger = Utils.getLogger(ApiTimeoutManagerImpl.class);

    private Map<Class, ApiTimeout> apiTimeouts = new HashMap<Class, ApiTimeout>();
    private Map<Class, Long> timeouts = new HashMap<Class, Long>();

    class Value {
        private String valueString;
        private String key;
        private Map<String, String> values = new HashMap<String, String>();

        public Value(String key, String valueString) {
            this.valueString = valueString;
            this.key = key;
            String[] vals = valueString.split(",");
            for (String val : vals) {
                val = val.trim();
                String[] keyval = val.split("::", 2);
                if (keyval.length != 2) {
                    throw new IllegalArgumentException(String.format("the key/value pair must be in format of key::value;" +
                            "invalid configuration[%s=%s] found, check your zstack.properties", key, valueString));
                }

                values.put(keyval[0], keyval[1]);
            }
        }

        public String getValue(String key) {
            String val = values.get(key);
            if (val == null) {
                throw new IllegalArgumentException(String.format("not key[%s] found key/value pair[%s=%s], check your zstack.properties",
                        key, this.key, valueString));
            }

            return val;
        }
    }

    private final String VALUE_TIMEOUT = "timeout";

    void init() {
        try {
            collectTimeout();
            collectTimeoutForDerivedApi();
            flatTimeout();
        } catch (RuntimeException e) {
            new BootErrorLog().write(e.getMessage());
            throw e;
        }
    }

    private void flatTimeout() {
        for (Map.Entry<Class, ApiTimeout> e :apiTimeouts.entrySet()) {
            ApiTimeout v = e.getValue();
            for (Class clz : v.getRelatives()) {
                Long currentTimeout = timeouts.get(clz);
                Long timeout = currentTimeout == null ? v.getTimeout() : Math.max(currentTimeout, v.getTimeout());
                timeouts.put(clz, timeout);
            }
        }
    }

    private void collectTimeoutForDerivedApi() {
        Reflections reflections = Platform.getReflections();

        Map<Class, ApiTimeout> children = new HashMap<Class, ApiTimeout>();
        for (Map.Entry<Class, ApiTimeout> e : apiTimeouts.entrySet()) {
            Class clz = e.getKey();
            ApiTimeout at = e.getValue();

            Set<Class> subClasses = reflections.getSubTypesOf(clz);
            for (Class child : subClasses) {
                children.put(child, at);

                if (logger.isTraceEnabled()) {
                    logger.trace(String.format("configure timeout for API[%s]:" +
                            "\nrelatives: %s" +
                            "\ntimeout: %sms", child, at.relatives, at.timeout));
                }
            }
        }

        apiTimeouts.putAll(children);
    }

    private void collectTimeout() {
        Map<Class, Set<Class>> m = new HashMap<Class, Set<Class>>();
        List<Class> subs = BeanUtils.scanClass("org.zstack", org.zstack.header.core.ApiTimeout.class);
        for (Class sub : subs) {
            org.zstack.header.core.ApiTimeout at = (org.zstack.header.core.ApiTimeout) sub.getAnnotation(org.zstack.header.core.ApiTimeout.class);
            for (Class apiClz : at.apiClasses()) {
                Set<Class> relatives = m.get(apiClz);
                if (relatives == null) {
                    relatives = new HashSet<Class>();
                    m.put(apiClz, relatives);
                }

                relatives.add(sub);
            }
        }

        for (final Map.Entry<String, String> e : Platform.getGlobalProperties().entrySet()) {
            String key = e.getKey();
            if (!key.startsWith("ApiTimeout.")) {
                continue;
            }

            String apiName = StringDSL.stripStart(key, "ApiTimeout.").trim();
            Class apiClz;
            String ERROR_INFO = "The configuration must be in format of \n" +
                    "   ApiTimeout.full_api_class_name = timeout::the_value_of_timeout(e.g. 1h, 30m)";
            try {
                apiClz = Class.forName(apiName);
            } catch (ClassNotFoundException ex) {
                throw new CloudRuntimeException(String.format("Invalid API timeout configuration[invalid key: %s], %s. %s",
                        key, ex.getMessage(), ERROR_INFO));
            }

            String value = e.getValue();
            Value val = new Value(key, value);
            long timeout = parseTimeout(val.getValue(VALUE_TIMEOUT));
            ApiTimeout apiTimeout = new ApiTimeout();
            apiTimeout.setTimeout(timeout);
            apiTimeout.setRelatives(m.get(apiClz));
            apiTimeouts.put(apiClz, apiTimeout);
            if (logger.isTraceEnabled()) {
                logger.trace(String.format("configure timeout for API[%s]:" +
                        "\nrelatives: %s" +
                        "\ntimeout: %sms", apiClz, apiTimeout.relatives, apiTimeout.timeout));
            }
        }
    }

    private long parseTimeout(String timeout) {
        if ("5m".equals(timeout)) {
            // optimization as the most of default timeout are 5 minutes
            return 300000;
        }

        return TimeUtils.parseTimeInMillis(timeout);
    }

    @Override
    public Long getTimeout(Class clz) {
        return timeouts.get(clz);
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
        Long t = getTimeout(clz);
        if (t != null) {
            return tu.convert(t, TimeUnit.MILLISECONDS);
        } else {
            return null;
        }
    }

    @Override
    public Map<Class, ApiTimeout> getAllTimeout() {
        return apiTimeouts;
    }
}
