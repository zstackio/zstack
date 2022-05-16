package org.zstack.core.timeout;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.*;
import org.zstack.core.db.Q;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.Component;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.*;
import org.zstack.utils.*;
import org.zstack.utils.logging.CLogger;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;

/**
 * Created by frank on 2/17/2016.
 */
public class ApiTimeoutManagerImpl implements ApiTimeoutManager, Component,
        BeforeDeliveryMessageInterceptor, GlobalConfigInitExtensionPoint {
    private static final CLogger logger = Utils.getLogger(ApiTimeoutManagerImpl.class);

    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private CloudBus bus;
    @Autowired
    private GlobalConfigFacade gcf;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private Timer timer;

    // Legacy timeout which is configured in zstack.properties is prior 3.2.0
    // which has been replaced by ThreadContext based timeout
    // keep this for back-compatibility
    private Map<Class, Long> legacyTimeouts = new HashMap<>();
    private List<ApiTimeoutExtensionPoint> apiTimeoutExts;

    public static final String APITIMEOUT_GLOBAL_CONFIG_TYPE = "apiTimeout";
    public static final String CONFIGURABLE_TIMEOUT_GLOBAL_CONFIG_TYPE = "configurableTimeout";
    private long SYNCALL_TIMEOUT = -1;
    public static final String TASK_CONTEXT_MESSAGE_TIMEOUT = "__messagetimeout__";
    public static final String TASK_CONTEXT_MESSAGE_DEADLINE = "__messagedeadline__";

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

    public Map<Class, Long> getLegacyTimeouts() {
        return legacyTimeouts;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public int orderOfBeforeDeliveryMessageInterceptor() {
        return 100;
    }

    @Override
    public long getMessageTimeout(ConfigurableTimeoutMessage msg) {
        if (msg instanceof APIMessage) {
            return getAPIMessageTimeout((APIMessage) msg);
        } else {
            return getNotApiMessageTimeout(msg);
        }
    }

    private long getNotApiMessageTimeout(ConfigurableTimeoutMessage msg) {
        String s = gcf.getConfigValue(CONFIGURABLE_TIMEOUT_GLOBAL_CONFIG_TYPE, msg.getClass().getName(), String.class);
        return parseTimeout(s);
    }

    private long getAPIMessageTimeout(APIMessage msg) {
        if (msg.getTimeout() != -1) {
            // the timeout is set somewhere, use it
            return msg.getTimeout();
        }

        if (msg instanceof APISyncCallMessage) {
            return SYNCALL_TIMEOUT;
        } else {
            String s = gcf.getConfigValue(APITIMEOUT_GLOBAL_CONFIG_TYPE, msg.getClass().getName(), String.class);
            return parseTimeout(s);
        }
    }

    @Override
    public void beforeDeliveryMessage(Message msg) {
        if (!TaskContext.containsTaskContext(TASK_CONTEXT_MESSAGE_DEADLINE)) {
            long currentTime = timer.getCurrentTimeMillis();

            if (msg instanceof ConfigurableTimeoutMessage) {
                TaskContext.putTaskContextItem(TASK_CONTEXT_MESSAGE_TIMEOUT, String.valueOf(getMessageTimeout((ConfigurableTimeoutMessage) msg)));
                TaskContext.putTaskContextItem(TASK_CONTEXT_MESSAGE_DEADLINE, String.valueOf(currentTime + getMessageTimeout((ConfigurableTimeoutMessage) msg)));
            } else if (msg instanceof NeedReplyMessage) {
                TaskContext.putTaskContextItem(TASK_CONTEXT_MESSAGE_TIMEOUT, String.valueOf(getMessageTimeout()));
                TaskContext.putTaskContextItem(TASK_CONTEXT_MESSAGE_DEADLINE, String.valueOf(currentTime + getMessageTimeout()));
            }
        }
    }

    private long getMessageTimeout() {
        for (ApiTimeoutExtensionPoint apiTimeoutExt : apiTimeoutExts) {
            Long timeout = apiTimeoutExt.getApiTimeout();
            if (timeout != null) {
                return timeout;
            }
        }

        if (TaskContext.containsTaskContext(TASK_CONTEXT_MESSAGE_TIMEOUT)) {
            return Long.parseLong((String) TaskContext.getTaskContext().get(TASK_CONTEXT_MESSAGE_TIMEOUT));
        }

        // this is an internal message
        return parseTimeout(ApiTimeoutGlobalProperty.INTERNAL_MESSAGE_TIMEOUT);
    }

    private Long getLegacyTimeout(Class clz) {
        return legacyTimeouts.get(clz);
    }

    @Override
    public List<GlobalConfig> getGenerationGlobalConfig() {
        return prepareTimeoutGlobalConfig();
    }


    private List<GlobalConfig> prepareTimeoutGlobalConfig() {
        Set<Class<? extends ConfigurableTimeoutMessage>> allConfigurableMessageClasses = BeanUtils.reflections
                .getSubTypesOf(ConfigurableTimeoutMessage.class).stream()
                .filter(clz -> !APISyncCallMessage.class.isAssignableFrom(clz))
                .collect(Collectors.toSet());

        List<GlobalConfig> results = new ArrayList<>();
        allConfigurableMessageClasses.forEach(clz -> {
            GlobalConfigVO vo = new GlobalConfigVO();

            if (APIMessage.class.isAssignableFrom(clz)) {
                vo.setCategory(APITIMEOUT_GLOBAL_CONFIG_TYPE);
            } else {
                vo.setCategory(CONFIGURABLE_TIMEOUT_GLOBAL_CONFIG_TYPE);
            }

            vo.setName(clz.getName());
            vo.setDescription(String.format("timeout for message %s", clz));

            DefaultTimeout at = clz.getAnnotation(DefaultTimeout.class);
            if (at == null) {
                vo.setDefaultValue("30m");
            } else {
                vo.setDefaultValue(String.valueOf(at.timeunit().toMillis(at.value())));
            }

            vo.setValue(getTimeoutGlobalConfigValue(clz, vo));

            results.add(GlobalConfig.valueOf(vo));
        });

        return results;
    }

    private String getTimeoutGlobalConfigValue(Class clz, GlobalConfigVO vo) {
        // once global config already exists, use its default value as
        // auto generated value and do not use legacy timeout
        if (Q.New(GlobalConfigVO.class)
                .eq(GlobalConfigVO_.category, vo.getCategory())
                .eq(GlobalConfigVO_.name, vo.getName()).isExists()) {
            return vo.getDefaultValue();
        }

        Long timeout = getLegacyTimeout(clz);
        if (timeout != null) {
            return String.valueOf(timeout);
        }

        return vo.getDefaultValue();
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
            try {
                apiClz = Class.forName(apiName);
            } catch (ClassNotFoundException ex) {
                String errInfo = "The configuration must be in format of \n" +
                        "   ApiTimeout.full_api_class_name = timeout::the_value_of_timeout(e.g. 1h, 30m)";
                throw new CloudRuntimeException(String.format("Invalid API timeout configuration[invalid key: %s], %s. %s",
                        key, ex.getMessage(), errInfo));
            }

            String value = e.getValue();
            Value val = new Value(key, value);
            long timeout = parseTimeout(val.getValue("timeout"));
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
        if (TaskContext.containsTaskContext(TASK_CONTEXT_MESSAGE_DEADLINE)) {
            long deadline = Long.parseLong((String) TaskContext.getTaskContext().get(TASK_CONTEXT_MESSAGE_DEADLINE));
            if (deadline < timer.getCurrentTimeMillis()) {
                return 1L;
            }
        }

        return getMessageTimeout();
    }

    @Override
    public Long getTimeoutSeconds() {
        return TimeUnit.MILLISECONDS.toSeconds(getTimeout());
    }

    /**
     * This method is aimed to set up message timeout, there are
     * two situations need to resolve
     * 1. when send configurable message which do not have message
     * timeout in thread context, need to set up its timeout from
     * message timeout global config
     * assume apiMessage default timeout 72h will send a configurableTimeoutMessage,
     * the configurableTimeoutMessage set a default timeout 24h.
     * So check the TASK_CONTEXT_MESSAGE_TIMEOUT to confirm the message will use a
     * timeout from its api message but not from global config
     * 2. when send need reply message(this kind of message always
     * have timeout thread context) use getTimeout() to set its own
     * timeout
     * @param msg the message need to set up message timeout
     */
    @Override
    public void setMessageTimeout(Message msg) {
        if (msg instanceof OverlayMessage) {
            NeedReplyMessage imsg = ((OverlayMessage) msg).getMessage();
            setMessageTimeout(imsg);
            ((OverlayMessage) msg).setTimeout(imsg.getTimeout());
        } else if (msg instanceof MulitpleOverlayMsg) {
            List<NeedReplyMessage> imsgs = ((MulitpleOverlayMsg) msg).getMessages();
            imsgs.forEach(this::setMessageTimeout);
            long timeout = imsgs.stream().mapToLong(NeedReplyMessage::getTimeout).max()
                    .orElse(parseTimeout(ApiTimeoutGlobalProperty.INTERNAL_MESSAGE_TIMEOUT));
            ((MulitpleOverlayMsg) msg).setTimeout(timeout);
        } else if (msg instanceof ConfigurableTimeoutMessage) {
            ((ConfigurableTimeoutMessage) msg).setTimeout(evalTimeout(getMessageTimeout((ConfigurableTimeoutMessage) msg)));
        } else if (msg instanceof NeedReplyMessage) {
            NeedReplyMessage nmsg = (NeedReplyMessage) msg;
            if (nmsg.getTimeout() == -1) {
                nmsg.setTimeout(evalTimeout(getMessageTimeout()));
            }
        }
    }

    private long evalTimeout(long messageTimeout) {
        if (!TaskContext.containsTaskContext(TASK_CONTEXT_MESSAGE_DEADLINE)) {
            return messageTimeout;
        }

        if (TaskContext.containsTaskContext(TASK_CONTEXT_MESSAGE_TIMEOUT)) {
            long originTimeout = Long.parseLong((String) TaskContext.getTaskContext().get(TASK_CONTEXT_MESSAGE_TIMEOUT));

            // deadline should be updated
            if (messageTimeout != originTimeout) {
                long deadline = Long.parseLong((String) TaskContext.getTaskContext().get(TASK_CONTEXT_MESSAGE_DEADLINE));
                long remainingTime = deadline - timer.getCurrentTimeMillis();

                TaskContext.getTaskContext().put(TASK_CONTEXT_MESSAGE_DEADLINE, String.valueOf(timer.getCurrentTimeMillis() + messageTimeout - (originTimeout - remainingTime)));
                TaskContext.getTaskContext().put(TASK_CONTEXT_MESSAGE_TIMEOUT, String.valueOf(messageTimeout));
            }
        }

        return getTimeout();
    }
}
