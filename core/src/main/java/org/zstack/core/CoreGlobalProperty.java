package org.zstack.core;

/**
 */
@GlobalPropertyDefinition
public class CoreGlobalProperty {
    @GlobalProperty(name = "unitTestOn", defaultValue = "false")
    public static boolean UNIT_TEST_ON;
    @GlobalProperty(name = "beanRefContextConf", defaultValue = "beanRefContext.xml")
    public static String BEAN_REF_CONTEXT_CONF;
    @GlobalProperty(name = "vmTracerOn", defaultValue = "true")
    public static boolean VM_TRACER_ON;
    @GlobalProperty(name = "profiler.workflow", defaultValue = "false")
    public static boolean PROFILER_WORKFLOW;
    @GlobalProperty(name = "profiler.httpCall", defaultValue = "false")
    public static boolean PROFILER_HTTP_CALL;
    @GlobalProperty(name = "exitJVMOnBootFailure", defaultValue = "true")
    public static boolean EXIT_JVM_ON_BOOT_FAILURE;
    @GlobalProperty(name = "checkBoxTypeInInventory", defaultValue = "false")
    public static boolean CHECK_BOX_TYPE_IN_INVENTORY;
    @GlobalProperty(name = "pidFilePath", defaultValue = "{user.home}/management-server.pid")
    public static String PID_FILE_PATH;
    @GlobalProperty(name = "consoleProxyOverriddenIp", defaultValue = "0.0.0.0")
    public static String CONSOLE_PROXY_OVERRIDDEN_IP;
    @GlobalProperty(name = "exposeSimulatorType", defaultValue = "false")
    public static boolean EXPOSE_SIMULATOR_TYPE;
    @GlobalProperty(name = "exitJVMOnStop", defaultValue = "true")
    public static boolean EXIT_JVM_ON_STOP;
    @GlobalProperty(name = "locale", defaultValue = "zh_CN")
    public static String LOCALE;
    @GlobalProperty(name = "user.home")
    public static String USER_HOME;
    @GlobalProperty(name = "RESTFacade.readTimeout", defaultValue = "300000")
    public static int REST_FACADE_READ_TIMEOUT;
    @GlobalProperty(name = "RESTFacade.connectTimeout", defaultValue = "15000")
    public static int REST_FACADE_CONNECT_TIMEOUT;
    @GlobalProperty(name = "upgradeStartOn", defaultValue = "false")
    public static boolean IS_UPGRADE_START;
    @GlobalProperty(name = "shadowEntityOn", defaultValue = "false")
    public static boolean SHADOW_ENTITY_ON;
    @GlobalProperty(name = "consoleProxyPort", defaultValue = "4900")
    public static int CONSOLE_PROXY_PORT;
}
