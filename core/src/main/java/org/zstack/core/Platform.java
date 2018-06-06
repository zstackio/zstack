package org.zstack.core;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.LocaleUtils;
import org.apache.commons.lang.StringUtils;
import org.reflections.Reflections;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.web.context.WebApplicationContext;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.componentloader.ComponentLoaderImpl;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.core.db.DatabaseGlobalProperty;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.statemachine.StateMachine;
import org.zstack.core.statemachine.StateMachineImpl;
import org.zstack.header.Component;
import org.zstack.header.core.StaticInit;
import org.zstack.header.core.encrypt.ENCRYPT;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.vo.BaseResource;
import org.zstack.utils.*;
import org.zstack.utils.data.StringTemplate;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.logging.CLoggerImpl;
import org.zstack.utils.path.PathUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;
import static org.zstack.utils.StringDSL.ln;

public class Platform {
    private static final CLogger logger = CLoggerImpl.getLogger(Platform.class);

    private static ComponentLoader loader;
    private static String msId;
    private static String managementServerIp;
    private static MessageSource messageSource;

    public static final String COMPONENT_CLASSPATH_HOME = "componentsHome";
    public static final String FAKE_UUID = "THIS_IS_A_FAKE_UUID";

    private static final Map<String, String> globalProperties = new HashMap<String, String>();

    private static Locale locale;

    public static volatile boolean IS_RUNNING = true;

    private static Reflections reflections = BeanUtils.reflections;

    public static Reflections getReflections() {
        return reflections;
    }

    public static Set<Method> encryptedMethodsMap;

    public static Map<String, String> childResourceToBaseResourceMap = new HashMap<>();

    static Map<Class, DynamicObjectMetadata> dynamicObjectMetadata = new HashMap<>();

    public static Locale getLocale() {
        return locale;
    }

    private static Map<String, String> linkGlobalPropertyMap(String prefix) {
        Map<String, String> ret = new HashMap<String, String>();
        Map<String, String> map = getGlobalPropertiesStartWith(prefix);
        if (map.isEmpty()) {
            return ret;
        }

        for (Map.Entry<String, String> e : map.entrySet()) {
            String key = StringDSL.stripStart(e.getKey(), prefix).trim();
            ret.put(key, e.getValue().trim());
        }

        return ret;
    }

    private static void linkGlobalProperty(Class clz, Map<String, String> propertiesMap) {
        for (Field f : clz.getDeclaredFields()) {
            GlobalProperty at = f.getAnnotation(GlobalProperty.class);
            if (at == null) {
                continue;
            }

            if (!Modifier.isStatic(f.getModifiers())) {
                throw new CloudRuntimeException(String.format("%s.%s is annotated by @GlobalProperty but it's not defined with static modifier", clz.getName(), f.getName()));
            }

            Object valueToSet = null;
            String name = at.name();
            if (Map.class.isAssignableFrom(f.getType())) {
                Map ret = linkGlobalPropertyMap(name);
                if (ret.isEmpty() && at.required()) {
                    throw new IllegalArgumentException(String.format("A required global property[%s] missing in zstack.properties", name));
                }
                valueToSet = ret;
            }  else if (List.class.isAssignableFrom(f.getType())) {
                List ret = linkGlobalPropertyList(name);
                if (ret.isEmpty() && at.required()) {
                    throw new IllegalArgumentException(String.format("A required global property[%s] missing in zstack.properties", name));
                }
                valueToSet = ret;
            } else {
                String value = propertiesMap.get(name);
                if (value == null && at.defaultValue().equals(GlobalProperty.DEFAULT_NULL_STRING) && at.required()) {
                    throw new IllegalArgumentException(String.format("A required global property[%s] missing in zstack.properties", name));
                }

                if (value == null) {
                    value = at.defaultValue();
                }

                if (GlobalProperty.DEFAULT_NULL_STRING.equals(value)) {
                    value = null;
                }

                if (value != null) {
                    value = StringTemplate.substitute(value, propertiesMap);
                }

                if (Integer.class.isAssignableFrom(f.getType()) || Integer.TYPE.isAssignableFrom(f.getType())) {
                    valueToSet =  TypeUtils.stringToValue(value, Integer.class, 0);
                } else if (Long.class.isAssignableFrom(f.getType()) || Long.TYPE.isAssignableFrom(f.getType())) {
                    valueToSet =  TypeUtils.stringToValue(value, Long.class, 0L);
                } else if (Float.class.isAssignableFrom(f.getType()) || Float.TYPE.isAssignableFrom(f.getType())) {
                    valueToSet = TypeUtils.stringToValue(value, Float.class, 0F);
                } else if (Double.class.isAssignableFrom(f.getType()) || Double.TYPE.isAssignableFrom(f.getType())) {
                    valueToSet = TypeUtils.stringToValue(value, Double.class, 0D);
                } else if (String.class.isAssignableFrom(f.getType())) {
                    valueToSet = value;
                } else if (Boolean.class.isAssignableFrom(f.getType()) || Boolean.TYPE.isAssignableFrom(f.getType())) {
                    valueToSet = TypeUtils.stringToValue(value, Boolean.class);
                } else {
                    throw new CloudRuntimeException(String.format("%s.%s of type[%s] is unsupported by global property. try use Platform.getGlobalProperty() and parse by yourself",
                            clz.getName(), f.getName(), f.getType().getName()));
                }
            }

            f.setAccessible(true);
            try {
                f.set(null, valueToSet);
                globalProperties.put(name, valueToSet == null ? "null" : valueToSet.toString());
                if (logger.isTraceEnabled()) {
                    logger.trace(String.format("linked global property[%s.%s], value: %s", clz.getName(), f.getName(), valueToSet));
                }
            } catch (IllegalAccessException e) {
                throw new CloudRuntimeException(String.format("unable to link global property[%s.%s]", clz.getName(), f.getName()), e);
            }
        }
    }

    public static Map<String, String> getGlobalProperties() {
        return globalProperties;
    }

    private static List linkGlobalPropertyList(String name) {
        Map<String, String> map = getGlobalPropertiesStartWith(name);
        List<String> ret = new ArrayList<String>(map.size());
        if (map.isEmpty()) {
            return ret;
        }

        List<String> orderedKeys = new ArrayList<String>();
        orderedKeys.addAll(map.keySet());
        Collections.sort(orderedKeys);

        for (String key : orderedKeys) {
            String index = StringDSL.stripStart(key, name).trim();
            try {
                Long.valueOf(index);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(String.format("[Illegal List Definition] %s is an invalid list key" +
                        " definition, the last character must be a number, for example %s1. %s is not a number", key, key, index));

            }

            ret.add(map.get(key));
        }

        return ret;
    }

    private static void linkGlobalProperty() {
        Set<Class<?>> clzs = reflections.getTypesAnnotatedWith(GlobalPropertyDefinition.class);

        boolean noTrim = System.getProperty("DoNotTrimPropertyFile") != null;

        List<String> lst = new ArrayList<String>();
        Map<String, String> propertiesMap = new HashMap<String, String>();
        for (final String name: System.getProperties().stringPropertyNames()) {
            String value = System.getProperty(name);
            if (!noTrim) {
                value = value.trim();
            }
            propertiesMap.put(name, value);
            lst.add(String.format("%s=%s", name, value));
        }

        logger.debug(String.format("system properties:\n%s", StringUtils.join(lst, ",")));

        for (Class clz : clzs) {
            linkGlobalProperty(clz, propertiesMap);
        }
    }

    private static void writePidFile() throws IOException {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return;
        }

        File pidFile = new File(CoreGlobalProperty.PID_FILE_PATH);
        if (pidFile.exists()) {
            String pidStr = FileUtils.readFileToString(pidFile);
            try {
                long pid = Long.valueOf(pidStr);
                String processProcDir = String.format("/proc/%s", pid);
                File processProcDirFile = new File(processProcDir);
                if (processProcDirFile.exists()) {
                    throw new CloudRuntimeException(String.format("pid file[%s] exists and the process[pid:%s] that the pid file points to is still running", CoreGlobalProperty.PID_FILE_PATH, pidStr));
                }
            } catch (NumberFormatException e) {
                logger.warn(String.format("pid file[%s] includes an invalid pid[%s] that is not a long number, ignore it",
                        CoreGlobalProperty.PID_FILE_PATH, pidStr));
            }

            logger.info(String.format("stale pid file[%s], ignore it", CoreGlobalProperty.PID_FILE_PATH));
        }

        pidFile.deleteOnExit();
        String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
        FileUtils.writeStringToFile(pidFile, pid);
    }

    private static void prepareDefaultDbProperties() {
        if (DatabaseGlobalProperty.DbUrl != null) {
            String dbUrl = DatabaseGlobalProperty.DbUrl;
            if (dbUrl.endsWith("/")) {
                dbUrl = dbUrl.substring(0, dbUrl.length()-1);
            }

            if (getGlobalProperty("DbFacadeDataSource.jdbcUrl") == null) {
                String url;
                if (dbUrl.contains("{database}")) {
                    url = ln(dbUrl).formatByMap(
                            map(e("database", "zstack"))
                    );
                } else {
                    url = String.format("%s/zstack", dbUrl);
                }

                System.setProperty("DbFacadeDataSource.jdbcUrl", url);
                logger.debug(String.format("default DbFacadeDataSource.jdbcUrl to DB.url [%s]", url));
            }
            if (getGlobalProperty("RESTApiDataSource.jdbcUrl") == null) {
                String url;
                if (dbUrl.contains("{database}")) {
                    url = ln(dbUrl).formatByMap(
                            map(e("database", "zstack_rest"))
                    );
                } else {
                    url = String.format("%s/zstack_rest", dbUrl);
                }

                System.setProperty("RESTApiDataSource.jdbcUrl", url);
                logger.debug(String.format("default RESTApiDataSource.jdbcUrl to DB.url [%s]", url));
            }
        }
        if (DatabaseGlobalProperty.DbUser != null) {
            if (getGlobalProperty("DbFacadeDataSource.user") == null) {
                System.setProperty("DbFacadeDataSource.user", DatabaseGlobalProperty.DbUser);
                logger.debug(String.format("default RESTApiDataSource.user to DB.user [%s]", DatabaseGlobalProperty.DbUser));
            }
            if (getGlobalProperty("RESTApiDataSource.user") == null) {
                System.setProperty("RESTApiDataSource.user", DatabaseGlobalProperty.DbUser);
                logger.debug(String.format("default RESTApiDataSource.user to DB.user [%s]", DatabaseGlobalProperty.DbUser));
            }
        }
        if (DatabaseGlobalProperty.DbPassword != null) {
            if (getGlobalProperty("DbFacadeDataSource.password") == null) {
                System.setProperty("DbFacadeDataSource.password", DatabaseGlobalProperty.DbPassword);
                logger.debug(String.format("default DbFacadeDataSource.password to DB.password [%s]", DatabaseGlobalProperty.DbPassword));
            }
            if (getGlobalProperty("RESTApiDataSource.password") == null) {
                System.setProperty("RESTApiDataSource.password", DatabaseGlobalProperty.DbPassword);
                logger.debug(String.format("default RESTApiDataSource.password to DB.password [%s]", DatabaseGlobalProperty.DbPassword));
            }
        }
        if (DatabaseGlobalProperty.DbMaxIdleTime != null) {
            if (getGlobalProperty("DbFacadeDataSource.maxIdleTime") == null) {
                System.setProperty("DbFacadeDataSource.maxIdleTime", DatabaseGlobalProperty.DbMaxIdleTime);
                logger.debug(String.format("default DbFacadeDataSource.maxIdleTime to DB.maxIdleTime [%s]", DatabaseGlobalProperty.DbMaxIdleTime));
            }
            if (getGlobalProperty("ExtraDataSource.maxIdleTime") == null) {
                System.setProperty("ExtraDataSource.maxIdleTime", DatabaseGlobalProperty.DbMaxIdleTime);
                logger.debug(String.format("default ExtraDataSource.maxIdleTime to DB.maxIdleTime [%s]", DatabaseGlobalProperty.DbMaxIdleTime));
            }
            if (getGlobalProperty("RESTApiDataSource.maxIdleTime") == null) {
                System.setProperty("RESTApiDataSource.maxIdleTime", DatabaseGlobalProperty.DbMaxIdleTime);
                logger.debug(String.format("default RESTApiDataSource.maxIdleTime to DB.maxIdleTime [%s]", DatabaseGlobalProperty.DbMaxIdleTime));
            }
        }
        if (DatabaseGlobalProperty.DbIdleConnectionTestPeriod != null) {
            if (getGlobalProperty("DbFacadeDataSource.idleConnectionTestPeriod") == null) {
                System.setProperty("DbFacadeDataSource.idleConnectionTestPeriod", DatabaseGlobalProperty.DbIdleConnectionTestPeriod);
                logger.debug(String.format("default DbFacadeDataSource.idleConnectionTestPeriod to DB.idleConnectionTestPeriod [%s]", DatabaseGlobalProperty.DbIdleConnectionTestPeriod));
            }
            if (getGlobalProperty("ExtraDataSource.idleConnectionTestPeriod") == null) {
                System.setProperty("ExtraDataSource.idleConnectionTestPeriod", DatabaseGlobalProperty.DbIdleConnectionTestPeriod);
                logger.debug(String.format("default ExtraDataSource.idleConnectionTestPeriod to DB.idleConnectionTestPeriod [%s]", DatabaseGlobalProperty.DbIdleConnectionTestPeriod));
            }
            if (getGlobalProperty("RESTApiDataSource.idleConnectionTestPeriod") == null) {
                System.setProperty("RESTApiDataSource.idleConnectionTestPeriod", DatabaseGlobalProperty.DbIdleConnectionTestPeriod);
                logger.debug(String.format("default RESTApiDataSource.idleConnectionTestPeriod to DB.idleConnectionTestPeriod [%s]", DatabaseGlobalProperty.DbIdleConnectionTestPeriod));
            }
        }
    }

    static {
        try {
            msId = getUuid();

            Set<Class> baseResourceClasses = reflections.getTypesAnnotatedWith(BaseResource.class).stream()
                    .filter(clz -> clz.isAnnotationPresent(BaseResource.class)).collect(Collectors.toSet());
            for (Class clz : baseResourceClasses) {
                Set<Class> childResourceClasses = reflections.getSubTypesOf(clz);
                childResourceToBaseResourceMap.put(clz.getSimpleName(), clz.getSimpleName());
                for (Class child : childResourceClasses) {
                    childResourceToBaseResourceMap.put(child.getSimpleName(), clz.getSimpleName());
                }
            }

            File globalPropertiesFile = PathUtil.findFileOnClassPath("zstack.properties", true);
            FileInputStream in = new FileInputStream(globalPropertiesFile);
            System.getProperties().load(in);

            collectDynamicObjectMetadata();
            linkGlobalProperty();
            prepareDefaultDbProperties();
            callStaticInitMethods();
            encryptedMethodsMap = getAllEncryptPassword();
            writePidFile();
        } catch (Throwable e) {
            logger.warn(String.format("unhandled exception when in Platform's static block, %s", e.getMessage()), e);
            new BootErrorLog().write(e.getMessage());
            if (CoreGlobalProperty.EXIT_JVM_ON_BOOT_FAILURE) {
                System.exit(1);
            } else {
                throw new RuntimeException(e);
            }

        }
    }

    private static void collectDynamicObjectMetadata() {
        reflections.getSubTypesOf(DynamicObject.class).forEach(clz -> {
            DynamicObjectMetadata metadata = new DynamicObjectMetadata();
            FieldUtils.getAllFields(clz).forEach(f -> {
                f.setAccessible(true);
                metadata.fields.put(f.getName(), f);
            });

            Class p = clz;
            while (p != Object.class) {
                for (Method m : p.getDeclaredMethods()) {
                    m.setAccessible(true);
                    metadata.methods.put(m.getName(), m);
                }
                p = p.getSuperclass();
            }

            dynamicObjectMetadata.put(clz, metadata);
        });
    }

    public static String getBaseResourceType(String childResourceType) {
        String type = childResourceToBaseResourceMap.get(childResourceType);
        if (type == null) {
            type = childResourceType;
        }
        return type;
    }

    public static List<String> getAllChildrenResourceType(String baseResourceType) {
        return childResourceToBaseResourceMap.entrySet()
                .stream()
                .filter(map -> baseResourceType.equals(map.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private static Set<Method> getAllEncryptPassword() {
        Set<Method> encrypteds = reflections.getMethodsAnnotatedWith(ENCRYPT.class);
        for (Method encrypted: encrypteds) {
            logger.debug(String.format("found encrypted method[%s:%s]", encrypted.getDeclaringClass(), encrypted.getName()));
        }
        return encrypteds;
    }

    private static void callStaticInitMethods() throws InvocationTargetException, IllegalAccessException {
        List<Method> inits = new ArrayList<>(reflections.getMethodsAnnotatedWith(StaticInit.class));
        inits.sort((o1, o2) -> {
            StaticInit a1 = o1.getAnnotation(StaticInit.class);
            StaticInit a2 = o2.getAnnotation(StaticInit.class);
            return a2.order() - a1.order();
        });

        for (Method init : inits)  {
            if (!Modifier.isStatic(init.getModifiers())) {
                throw new CloudRuntimeException(String.format("the method[%s:%s] annotated by @StaticInit is not a static method", init.getDeclaringClass(), init.getName()));
            }

            logger.debug(String.format("calling static init method[%s:%s]", init.getDeclaringClass(), init.getName()));
            init.setAccessible(true);
            init.invoke(null);
        }
    }

    private static void initMessageSource() {
        locale = LocaleUtils.toLocale(CoreGlobalProperty.LOCALE);
        logger.debug(String.format("using locale[%s] for i18n logging messages", locale.toString()));

        if (loader == null) {
            throw new CloudRuntimeException("ComponentLoader is null. i18n has not been initialized, you call it too early");
        }

        BeanFactory beanFactory = loader.getSpringIoc();
        if (beanFactory == null) {
            throw new CloudRuntimeException("BeanFactory is null. i18n has not been initialized, you call it too early");
        }

        if (!(beanFactory instanceof MessageSource)) {
            throw new CloudRuntimeException("BeanFactory is not a spring MessageSource. i18n cannot be used");
        }

        messageSource = (MessageSource)beanFactory;
    }

    private static CloudBus bus;

    {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                if (bus != null) {
                    bus.stop();
                }
            }
        }));
    }

    public static String getGlobalProperty(String name) {
        return System.getProperty(name);
    }

    public static Map<String, String> getGlobalPropertiesStartWith(String prefix) {
        Properties props = System.getProperties();
        Enumeration e = props.propertyNames();

        Map<String, String> ret = new HashMap<String, String>();
        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            if (key.startsWith(prefix)) {
                ret.put(key, System.getProperty(key));
            }
        }

        return ret;
    }

    public static ComponentLoader createComponentLoaderFromWebApplicationContext(WebApplicationContext webAppCtx) {
        assert loader == null;
        try {
            if (webAppCtx != null) {
                loader = new ComponentLoaderImpl(webAppCtx);
            } else {
                loader = new ComponentLoaderImpl();
            }
        } catch (Exception e) {
            String err = "unable to create ComponentLoader";
            logger.warn(e.getMessage(), e);
            throw new CloudRuntimeException(err);
        }

        loader.getPluginRegistry();
        GlobalConfigFacade gcf = loader.getComponent(GlobalConfigFacade.class);
        if (gcf != null) {
            ((Component)gcf).start();
        }
        bus = loader.getComponentNoExceptionWhenNotExisting(CloudBus.class);
        if (bus != null)  {
            bus.start();
        }

        initMessageSource();

        return loader;
    }

    public static ComponentLoader getComponentLoader() {
		/*
		 * This part cannot be moved to static block at the beginning.
		 * Because component code loaded by Spring may call other functions in Platform which
		 * causes the static block to be executed, which results in cycle initialization of ComponentLoaderImpl.
		 */
        if (loader == null) {
            loader = createComponentLoaderFromWebApplicationContext(null);
        }

        return loader;
    }

    public static String getManagementServerId() {
        return msId;
    }

    public static <K extends Enum<K>, T extends Enum<T>> StateMachine<K, T> createStateMachine() {
        return new StateMachineImpl<K, T>();
    }

    public static String getUuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String getManagementServerIp() {
        if (managementServerIp != null) {
            return managementServerIp;
        }

        String ip = System.getProperty("management.server.ip");
        if (ip != null) {
            logger.info(String.format("get management IP[%s] from Java property[management.server.ip]", ip));
            return ip;
        }

        ip = System.getenv("ZSTACK_MANAGEMENT_SERVER_IP");
        if (ip != null) {
            logger.info(String.format("get management IP[%s] from environment variable[ZSTACK_MANAGEMENT_SERVER_IP]", ip));
            return ip;
        }

        Linux.ShellResult ret = Linux.shell("/sbin/ip route");
        String defaultLine = null;
        for (String s : ret.getStdout().split("\n")) {
            if (s.contains("default via")) {
                defaultLine = s;
                break;
            }
        }

        String err = "cannot get management server ip of this machine. there are three ways to get the ip.\n1) search for 'management.server.ip' java property\n2) search for 'ZSTACK_MANAGEMENT_SERVER_IP' environment variable\n3) search for default route printed out by '/sbin/ip route'\nhowever, all above methods failed";
        if (defaultLine == null) {
            throw new CloudRuntimeException(err);
        }

        try {
            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface iface : Collections.list(nets)) {
                String name = iface.getName();
                if (defaultLine.contains(name)) {
                    InetAddress ia = iface.getInetAddresses().nextElement();
                    ip = ia.getHostAddress();
                    break;
                }
            }
        } catch (SocketException e) {
            throw new CloudRuntimeException(e);
        }

        if (ip == null) {
            throw new CloudRuntimeException(err);
        }

        logger.info(String.format("get management IP[%s] from default route[/sbin/ip route]", ip));
        managementServerIp = ip;
        return managementServerIp;
    }

    public static String toI18nString(String code, Object... args) {
        return toI18nString(code, null, args);
    }

    public static String toI18nString(String code, Locale l, List args) {
        return toI18nString(code, l, args.toArray(new Object[args.size()]));
    }

    private static String stringFormat(String fmt, Object...args) {
        if (args == null || args.length == 0) {
            return fmt;
        } else {
            return String.format(fmt, args);
        }
    }

    public static String toI18nString(String code, Locale l, Object...args) {
        l = l == null ? locale : l;

        try {
            String ret;
            if (args.length > 0) {
                 ret = messageSource.getMessage(code, args, l);
            } else {
                 ret = messageSource.getMessage(code, null, l);
            }

            // if the result is an empty string which means the string is not translated in the locale,
            // return the original string so users won't get a confusing, empty string
            return ret.isEmpty() ? stringFormat(code, args) : ret;
        } catch (NoSuchMessageException e) {
            return stringFormat(code, args);
        }
    }

    public static String i18n(String str, Object...args) {
        return toI18nString(str, args);
    }

    public static String i18n(String str, Map<String, String> args) {
        Map<String, String> nargs = new HashMap<>();
        args.forEach((k, v) -> nargs.put(k, toI18nString(v)));

        return ln(toI18nString(str)).formatByMap(nargs);
    }

    public static boolean killProcess(int pid) {
        return killProcess(pid, 15);
    }

    public static boolean killProcess(int pid, Integer timeout) {
        timeout = timeout == null ? 30 : timeout;

        if (!TimeUtils.loopExecuteUntilTimeoutIgnoreExceptionAndReturn(timeout, 1, TimeUnit.SECONDS, () -> {
            ShellUtils.runAndReturn(String.format("kill %s", pid));
            return !new ProcessFinder().processExists(pid);
        })) {
            logger.warn(String.format("cannot kill the process[PID:%s] after %s seconds, kill -9 it", pid, timeout));
            ShellUtils.runAndReturn(String.format("kill -9 %s", pid));
        }

        if (!TimeUtils.loopExecuteUntilTimeoutIgnoreExceptionAndReturn(5, 1, TimeUnit.SECONDS, () -> !new ProcessFinder().processExists(pid))) {
            logger.warn(String.format("FAILED TO KILL -9 THE PROCESS[PID:%s], THE KERNEL MUST HAVE SOMETHING RUN", pid));
            return false;
        } else {
            return true;
        }
    }

    public static ErrorCode err(Enum errCode, String fmt, Object...args) {
        ErrorFacade errf = getComponentLoader().getComponent(ErrorFacade.class);
        if (SysErrors.INTERNAL == errCode) {
            return errf.instantiateErrorCode(errCode, String.format(fmt, args));
        } else {
            return errf.instantiateErrorCode(errCode, toI18nString(fmt, args));
        }
    }

    public static ErrorCode inerr(String fmt, Object...args) {
        return err(SysErrors.INTERNAL, fmt, args);
    }

    public static ErrorCode operr(String fmt, Object...args) {
        return err(SysErrors.OPERATION_ERROR, fmt, args);
    }

    public static ErrorCode argerr(String fmt, Object...args) {
        return err(SysErrors.INVALID_ARGUMENT_ERROR, fmt, args);
    }

    public static ErrorCode ioerr(String fmt, Object...args) {
        return err(SysErrors.IO_ERROR, fmt, args);
    }

    public static ErrorCode httperr(String fmt, Object...args) {
        return err(SysErrors.HTTP_ERROR, fmt, args);
    }

    public static Function<Supplier, Object> functionForMockTestObject = (Supplier t) -> t.get();

    // This is to make objects created by keyword 'new' mockable
    // developers call this method as a factory method like:
    //
    // JavaMailSenderImpl sender = Platform.New(()-> new JavaMailSenderImpl());
    //
    // in unit tests, we can replace functionForMockTestObject with a function which returns a mocked
    // object, for example:
    //
    // Platform.functionForMockTestObject = (Supplier t) -? {
    //      Object obj = t.get();
    //      return Mockito.spy(obj);
    // }
    public static <T> T New(Supplier supplier) {
        return (T) functionForMockTestObject.apply(supplier);
    }

    public static final String EXIT_REASON = "zstack.quit.reason";

    public static void exit(String reason) {
        new BootErrorLog().write(reason);
        System.setProperty(EXIT_REASON, reason);
        System.exit(1);
    }
}
