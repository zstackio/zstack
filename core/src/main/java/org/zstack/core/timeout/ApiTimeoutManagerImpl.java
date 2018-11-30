package org.zstack.core.timeout;

import org.apache.logging.log4j.ThreadContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.*;
import org.zstack.core.db.SQLBatch;
import org.zstack.header.Component;
import org.zstack.header.Constants;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.managementnode.PrepareDbInitialValueExtensionPoint;
import org.zstack.header.message.*;
import org.zstack.utils.*;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.argerr;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by frank on 2/17/2016.
 */
public class ApiTimeoutManagerImpl implements ApiTimeoutManager, Component,
        BeforeDeliveryMessageInterceptor, PrepareDbInitialValueExtensionPoint {
    private static final CLogger logger = Utils.getLogger(ApiTimeoutManagerImpl.class);

    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private CloudBus bus;
    @Autowired
    private GlobalConfigFacade gcf;

    // Legacy timeout which is configured in zstack.properties is prior 3.2.0
    // which has been replaced by ThreadContext based timeout
    // keep this for back-compatibility
    private Map<Class, Long> legacyTimeouts = new HashMap<>();
    private List<ApiTimeoutExtensionPoint> apiTimeoutExts;

    public static final String APITIMEOUT_GLOBAL_CONFIG_TYPE = "apiTimeout";
    private long SYNCALL_TIMEOUT = -1;
    public static final String TASK_CONTEXT_API_TIMEOUT = "__apitimeout__";

    void init() {
        SYNCALL_TIMEOUT = parseTimeout(ApiTimeoutGlobalProperty.SYNCCALL_API_TIMEOUT);
        collectLegacyTimeout();
    }

    @Override
    public boolean start() {
        try {
            populateExtensions();
            bus.installBeforeDeliveryMessageInterceptor(this);
            installValidatorToGlobalConfig();
        } catch (RuntimeException e) {
            new BootErrorLog().write(e.getMessage());
            throw e;
        }

        return true;
    }

    private void installValidatorToGlobalConfig() {
        GlobalConfigValidatorExtensionPoint validator = (category, name, oldValue, newValue) -> {
            long minimal = parseTimeout(ApiTimeoutGlobalProperty.MINIMAL_TIMEOUT);
            if (parseTimeout(newValue) < minimal) {
                throw new OperationFailureException(argerr("api timeout cannot be set smaller than %s", ApiTimeoutGlobalProperty.MINIMAL_TIMEOUT));
            }
        };

        gcf.getAllConfig().values().stream().filter(gc -> APITIMEOUT_GLOBAL_CONFIG_TYPE.equals(gc.getCategory()))
                .forEach(gc -> gc.installValidateExtension(validator));
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public int orderOfBeforeDeliveryMessageInterceptor() {
        return 100;
    }

    private long getAPIMessageTimeout(APIMessage msg) {
        if (msg.getTimeout() != -1) {
            // the timeout is set somewhere, use it
            return msg.getTimeout();
        }

        if (msg instanceof APISyncCallMessage) {
            return SYNCALL_TIMEOUT;
        } else  {
            String s = gcf.getConfigValue(APITIMEOUT_GLOBAL_CONFIG_TYPE, msg.getClass().getName(), String.class);
            return parseTimeout(s);
        }
    }

    @Override
    public void beforeDeliveryMessage(Message msg) {
        if (msg instanceof APIMessage) {
            TaskContext.putTaskContextItem(TASK_CONTEXT_API_TIMEOUT, getAPIMessageTimeout((APIMessage) msg));
        }
    }

    private Long getLegacyTimeout(Class clz) {
        return legacyTimeouts.get(clz);
    }

    @Override
    public void prepareDbInitialValue() {
        new SQLBatch() {
            @Override
            protected void scripts() {
                List<String> apiClassNamesInGlobalConfig = q(GlobalConfigVO.class).select(GlobalConfigVO_.name)
                        .eq(GlobalConfigVO_.category, APITIMEOUT_GLOBAL_CONFIG_TYPE).listValues();

                List<Class> apiClasses = BeanUtils.reflections.getSubTypesOf(APIMessage.class)
                        .stream().filter(clz -> !APISyncCallMessage.class.isAssignableFrom(clz))
                        .filter(clz -> !apiClassNamesInGlobalConfig.contains(clz.getName()))
                        .collect(Collectors.toList());


                apiClasses.forEach(clz -> {
                    GlobalConfigVO vo = new GlobalConfigVO();
                    vo.setCategory(APITIMEOUT_GLOBAL_CONFIG_TYPE);
                    vo.setName(clz.getName());
                    vo.setDescription(String.format("timeout for API %s", clz));

                    APIDefaultTimeout at = (APIDefaultTimeout) clz.getAnnotation(APIDefaultTimeout.class);
                    if (at == null) {
                        vo.setDefaultValue("30m");
                    } else {
                        vo.setDefaultValue(String.valueOf(at.timeunit().toMillis(at.value())));
                    }

                    Long timeout = getLegacyTimeout(clz);
                    if (timeout != null) {
                        vo.setValue(String.valueOf(timeout));
                    } else {
                        vo.setValue(vo.getDefaultValue());
                    }

                    gcf.createGlobalConfig(vo);
                });
            }
        }.execute();
    }

    class Value {
        private String valueString;
        private String key;
        private Map<String, String> values = new HashMap<>();

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

    private void populateExtensions() {
        apiTimeoutExts = pluginRgty.getExtensionList(ApiTimeoutExtensionPoint.class);
    }

    private void collectLegacyTimeout() {
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
            String VALUE_TIMEOUT = "timeout";
            long timeout = parseTimeout(val.getValue(VALUE_TIMEOUT));
            legacyTimeouts.put(apiClz, timeout);
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
    public Long getTimeout() {
        for (ApiTimeoutExtensionPoint apiTimeoutExt : apiTimeoutExts) {
            Long timeout = apiTimeoutExt.getApiTimeout();
            if (timeout != null) {
                return timeout;
            }
        }

        Long apiTimeout = parseObjectToLong(TaskContext.getTaskContextItem(TASK_CONTEXT_API_TIMEOUT));
        if (apiTimeout != null) {
            return apiTimeout;
        } else {
            // this is an internal message
            return parseTimeout(ApiTimeoutGlobalProperty.INTERNAL_MESSAGE_TIMEOUT);
        }
    }

    private static Long parseObjectToLong(Object o) {
        if (o == null) {
            return null;
        }
        DecimalFormat df = new DecimalFormat("#");
        df.setMaximumFractionDigits(0);
        return Long.parseLong(df.format(o).split("\\.")[0]);
    }

    @Override
    public void setMessageTimeout(Message msg) {
        if (msg instanceof APIMessage) {
            APIMessage amsg = (APIMessage) msg;
            amsg.setTimeout(getAPIMessageTimeout(amsg));
        } else if (msg instanceof NeedReplyMessage) {
            NeedReplyMessage nmsg = (NeedReplyMessage) msg;
            if (nmsg.getTimeout() == -1) {
                nmsg.setTimeout(getTimeout());
            }
        }
    }
}
