package org.zstack.core;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.LocaleUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.reflections.Reflections;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.web.context.WebApplicationContext;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusGlobalProperty;
import org.zstack.core.componentloader.ComponentLoader;
import org.zstack.core.componentloader.ComponentLoaderImpl;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.core.db.DatabaseGlobalProperty;
import org.zstack.core.encrypt.EncryptRSA;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.errorcode.GlobalErrorCodeI18nService;
import org.zstack.core.propertyvalidator.ValidatorTool;
import org.zstack.core.search.SearchGlobalProperty;
import org.zstack.core.search.SearchBackendConstant;
import org.zstack.core.statemachine.StateMachine;
import org.zstack.core.statemachine.StateMachineImpl;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.Component;
import org.zstack.header.core.StaticInit;
import org.zstack.header.core.encrypt.ENCRYPT;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.IdentityErrors;
import org.zstack.header.vo.BaseResource;
import org.zstack.utils.*;
import org.zstack.utils.data.StringTemplate;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.logging.CLoggerImpl;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.network.NetworkUtils;
import org.zstack.utils.path.PathUtil;
import org.zstack.utils.string.ErrorCodeElaboration;
import org.zstack.utils.string.StringSimilarity;
import org.zstack.utils.zsha2.ZSha2Helper;
import org.zstack.utils.zsha2.ZSha2Info;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Timestamp;
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
    private static String managementServerCidr;
    private static MessageSource messageSource;
    private static String encryptionKey = EncryptRSA.generateKeyString("ZStack open source");
    private static final String MANAGEMENT_SERVER_IP_PROPERTY = "management.server.ip";
    private static final String MANAGEMENT_SERVER_IP4_PROPERTY = "management.server.ip4";
    private static final String MANAGEMENT_SERVER_IP6_PROPERTY = "management.server.ip6";
    private static final String ZSTACK_MANAGEMENT_SERVER_IP_ENV = "ZSTACK_MANAGEMENT_SERVER_IP";
    private static final String IPV4_ADDRESS_COMMAND = "ip -4 add";
    private static final String IPV6_ADDRESS_COMMAND = "ip -6 addr";
    private static final String DEFAULT_ROUTE_COMMAND = "/sbin/ip route";
    private static final String DEFAULT_ROUTE_MARK = "default via";
    private static final String JGROUPS_INITIAL_HOST_FORMAT = "%s[%s],%s[%s]";
    private static final int IP_ADDRESS_COMMAND_CIDR_INDEX = 1;
    private static final int IP_ADDRESS_COMMAND_MIN_TOKEN_COUNT = 2;
    private static final String CIDR_SEPARATOR = "/";
    private static final String TEMP_FILE_SUFFIX = ".tmp";
    private static final String DATA_DIR_PROPERTY = "dataDir";
    private static final String DEFAULT_DATA_DIR = "/var/lib/zstack/";
    private static final String UNIT_TEST_ON_PROPERTY = "unitTestOn";
    private static final String JAVA_TMP_DIR_PROPERTY = "java.io.tmpdir";
    private static final String UNIT_TEST_DATA_DIR_NAME = "zstack-unit-test";
    private static final String MANAGEMENT_SERVER_ID_STATE_FILE_NAME = "management-server-id.properties";
    private static final String MANAGEMENT_SERVER_FINGERPRINT_VERSION = "2";
    private static final String MANAGEMENT_SERVER_FINGERPRINT_ALGORITHM = "sha256:";
    private static final String SUDO_COMMAND = "/usr/bin/sudo";
    private static final String DMIDECODE_COMMAND = "/usr/sbin/dmidecode";
    private static final int DMIDECODE_COMMAND_TIMEOUT_SECONDS = 3;
    private static final int DMI_SOURCE_NAME_INDEX = 0;
    private static final int DMI_SOURCE_FILE_INDEX = 1;
    private static final int DMI_SOURCE_COMMAND_ARG_INDEX = 2;
    private static final String[][] MANAGEMENT_SERVER_FINGERPRINT_DMI_SOURCES = {
            {"dmi:system-uuid", "/sys/class/dmi/id/product_uuid", "system-uuid"},
            {"dmi:system-serial-number", "/sys/class/dmi/id/product_serial", "system-serial-number"},
            {"dmi:baseboard-serial-number", "/sys/class/dmi/id/board_serial", "baseboard-serial-number"}
    };
    private static final List<String> UNUSABLE_MACHINE_IDENTITIES = Arrays.asList(
            "none",
            "unknown",
            "not specified",
            "to be filled by o.e.m.",
            "to be filled by oem",
            "default string",
            "system serial number"
    );
    private static final String ZSTACK_UUID_PATTERN = "[0-9a-fA-F]{32}";
    private static EncryptRSA rsa = new EncryptRSA();
    private static Map<String, Double> errorCounter = new HashMap<>();

    public static final String MANAGEMENT_SERVER_ID_PROPERTY = "managementServerId";
    public static final String MANAGEMENT_SERVER_FINGERPRINT_PROPERTY = "managementServerFingerprint";
    public static final String MANAGEMENT_SERVER_FINGERPRINT_VERSION_PROPERTY = "managementServerFingerprintVersion";
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
                Map<String, String> ret = linkGlobalPropertyMap(name);
                if (ret.isEmpty() && at.required()) {
                    throw new IllegalArgumentException(String.format("A required global property[%s] missing in zstack.properties", name));
                }

                if (at.encrypted()) {
                    ret.forEach((k, v) -> ret.put(k, rsa.decrypt(v, encryptionKey)));
                }
                valueToSet = ret;
            }  else if (List.class.isAssignableFrom(f.getType())) {
                List<String> ret = linkGlobalPropertyList(name);
                if (ret.isEmpty() && at.defaultListValue().length > 0) {
                    ret = Arrays.asList(at.defaultListValue());
                }

                if (ret.isEmpty() && at.required()) {
                    throw new IllegalArgumentException(String.format("A required global property[%s] missing in zstack.properties", name));
                }

                if (at.encrypted()) {
                    ret = ret.stream().map(it -> rsa.decrypt(it, encryptionKey)).collect(Collectors.toList());
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
                    if (at.encrypted()) {
                        value = rsa.decrypt(value, encryptionKey);
                    }
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

    private static List<String> linkGlobalPropertyList(String name) {
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
                Long.parseLong(index);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(String.format("[Illegal List Definition] %s is an invalid list key" +
                        " definition, the last character must be a number, for example %s1. %s is not a number", key, key, index));

            }

            ret.add(map.get(key));
        }

        return ret;
    }

    private static void validateGlobalProperty() {
        ValidatorTool validatorTool = new ValidatorTool();

        System.getProperties().stringPropertyNames().forEach(name->{
            String value = System.getProperty(name);
            validatorTool.checkProperty(name, value);
        });
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

    public static String getManagementPid() {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return "";
        }
        return ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
    }

    private static void writePidFile() throws IOException {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return;
        }

        File pidFile = new File(CoreGlobalProperty.PID_FILE_PATH);
        if (pidFile.exists()) {
            String pidStr = FileUtils.readFileToString(pidFile);
            try {
                long pid = Long.parseLong(pidStr);
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
                    url = url.trim();
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
                    url = url.trim();
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
                logger.debug(String.format("default DbFacadeDataSource.user to DB.user [%s]", DatabaseGlobalProperty.DbUser));
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

    private static void prepareHibernateSearchProperties() {
        if (!SearchGlobalProperty.SearchAutoRegister) {
            System.setProperty("Search.autoRegister", "false");
            logger.debug(String.format("default Search.autoRegister to Search.autoRegister [%s]", SearchGlobalProperty.SearchAutoRegister));
        }
        if (SearchGlobalProperty.SearchIndexBaseDir != null) {
            if (getGlobalProperty("Search.indexBaseDir") == null) {
                System.setProperty("Search.indexBaseDir", SearchGlobalProperty.SearchIndexBaseDir);
                logger.debug(String.format("default Search.indexBaseDir to Search.indexBaseDir [%s]", SearchGlobalProperty.SearchIndexBaseDir));
            }
        }
        if (SearchGlobalProperty.IndexWorkerExecution != null) {
            if (getGlobalProperty("IndexWorker.execution") == null) {
                System.setProperty("IndexWorker.execution", SearchGlobalProperty.IndexWorkerExecution);
                logger.debug(String.format("default IndexWorker.execution to IndexWorker.execution [%s]", SearchGlobalProperty.IndexWorkerExecution));
            }
        }
        if (SearchGlobalProperty.IndexWorkerFlushInterval != null) {
            if (getGlobalProperty("IndexWorker.flushInterval") == null) {
                System.setProperty("IndexWorker.flushInterval", SearchGlobalProperty.IndexWorkerFlushInterval);
                logger.debug(String.format("default IndexWorker.flushInterval to IndexWorker.flushIntervalr [%s]", SearchGlobalProperty.IndexWorkerFlushInterval));
            }
        }
        if (SearchGlobalProperty.JGroupInfinispanPort != null) {
            if (getGlobalProperty("JGroup.InfinispanPort") == null) {
                System.setProperty("JGroup.InfinispanPort", SearchGlobalProperty.JGroupInfinispanPort);
                logger.debug(String.format("default JGroup.InfinispanPort to JGroup.InfinispanPort [%s]", SearchGlobalProperty.JGroupInfinispanPort));
            }
        }
        if (SearchGlobalProperty.JGroupBackendPort != null) {
            if (getGlobalProperty("JGroup.BackendPort") == null) {
                System.setProperty("JGroup.BackendPort", SearchGlobalProperty.JGroupBackendPort);
                logger.debug(String.format("default JGroup.BackendPort to JGroup.BackendPort [%s]", SearchGlobalProperty.JGroupBackendPort));
            }
        }

        boolean mnHaEnvironment = ZSha2Helper.isMNHaEnvironment();
        String searchBackend = SearchBackendConstant.JGROUPS_BACKEND;
        if (mnHaEnvironment) {
            // jgroup configuration is required in multi node environment
            // if failed to get zsha2 info, this method thrown exception to stop management node from startup
            // so there is no need to handle exceptions
            ZSha2Info info = ZSha2Helper.getInfo(false);
            if (info.getNodeip() == null) {
                throw new RuntimeException("the ip of this node was null, please check the config of zsha2");
            }
            if (info.getPeerip() == null) {
                throw new RuntimeException("the ip of peer node was null, please check the config of zsha2");
            }
            SearchGlobalProperty.JGroupInfinispanInitialHosts = formatJGroupsInitialHosts(
                    info.getNodeip(), info.getPeerip(), Integer.parseInt(SearchGlobalProperty.JGroupInfinispanPort));
            SearchGlobalProperty.JGroupBackendInitialHosts = formatJGroupsInitialHosts(
                    info.getNodeip(), info.getPeerip(), Integer.parseInt(SearchGlobalProperty.JGroupBackendPort));
            if (getGlobalProperty("JGroup.InfinispanInitialHosts") == null) {
                System.setProperty("JGroup.InfinispanInitialHosts", SearchGlobalProperty.JGroupInfinispanInitialHosts);
                logger.debug(String.format("default JGroup.InfinispanInitialHosts to JGroup.InfinispanInitialHosts [%s]", SearchGlobalProperty.JGroupInfinispanInitialHosts));
            }
            if (getGlobalProperty("JGroup.BackendInitialHosts") == null) {
                System.setProperty("JGroup.BackendInitialHosts", SearchGlobalProperty.JGroupBackendInitialHosts);
                logger.debug(String.format("default JGroup.BackendInitialHosts to JGroup.BackendInitialHosts [%s]", SearchGlobalProperty.JGroupBackendInitialHosts));
            }
            searchBackend = selectHibernateSearchBackend(true);
        }
        if (getGlobalProperty("JGroup.Address") == null) {
            String serverIp = getCanonicalServerIp();
            System.setProperty("JGroup.Address", serverIp);
            logger.debug(String.format("default JGroup.Address to JGroup.Address [%s]", serverIp));
        }
        if (mnHaEnvironment) {
            SearchGlobalProperty.JGroupFlushBypass = "false";
            SearchGlobalProperty.JGroupJoinTimeout = "5000";
        } else {
            SearchGlobalProperty.ExclusiveIndexUse = "true";
        }

        if (mnHaEnvironment || getGlobalProperty(SearchBackendConstant.SEARCH_BACKEND_PROPERTY) == null) {
            System.setProperty(SearchBackendConstant.SEARCH_BACKEND_PROPERTY, searchBackend);
            logger.debug(String.format("default %s to %s [%s]", SearchBackendConstant.SEARCH_BACKEND_PROPERTY,
                    SearchBackendConstant.SEARCH_BACKEND_PROPERTY, searchBackend));
        }
        System.setProperty("Exclusive.indexUse", SearchGlobalProperty.ExclusiveIndexUse);
        System.setProperty("JGroup.FlushBypass", SearchGlobalProperty.JGroupFlushBypass);
        System.setProperty("JGroup.JoinTimeout", SearchGlobalProperty.JGroupJoinTimeout);
        logger.debug(String.format("default Exclusive.indexUse to Exclusive.indexUse [%s]", SearchGlobalProperty.ExclusiveIndexUse));
        logger.debug(String.format("default JGroup.FlushBypass to JGroup.FlushBypass [%s]", SearchGlobalProperty.JGroupFlushBypass));
        logger.debug(String.format("default JGroup.JoinTimeout to JGroup.JoinTimeout [%s]", SearchGlobalProperty.JGroupJoinTimeout));
    }

    public static String selectHibernateSearchBackend(boolean mnHaEnvironment) {
        if (!mnHaEnvironment) {
            return SearchBackendConstant.JGROUPS_BACKEND;
        }

        return SearchBackendConstant.ZSTACK_ZSHA2_JGROUPS_BACKEND;
    }

    static {
        FileInputStream in = null;
        try {
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

            in = new FileInputStream(globalPropertiesFile);
            System.getProperties().load(in);

            // get ms id should after global property setup
            msId = loadOrCreateManagementServerId(globalPropertiesFile, Platform::getUuid);

            collectDynamicObjectMetadata();
            linkGlobalProperty();
            validateGlobalProperty();
            prepareDefaultDbProperties();
            prepareHibernateSearchProperties();
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
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    logger.warn(String.format("FileInputStream close IOException：%s", e.getMessage()));
                }
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

    public static String getGlobalPropertyAnnotationName(Class clz, String fieldName) {
        try {
            String name = clz.getDeclaredField(fieldName).getAnnotation(GlobalProperty.class).name().trim();
            /* remove the last character '.' */
            return name.substring(0, name.length() - 1);
        } catch (Exception e) {
            return "";
        }
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

        ThreadFacade thdf = loader.getComponent(ThreadFacade.class);
        if (thdf != null) {
            thdf.start();
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

    public static synchronized String loadOrCreateManagementServerId(File propertiesFile, Supplier<String> idSupplier) {
        return loadOrCreateManagementServerId(propertiesFile, getManagementServerIdStateFile(), idSupplier);
    }

    public static synchronized String loadOrCreateManagementServerId(File propertiesFile, File stateFile, Supplier<String> idSupplier) {
        return loadOrCreateManagementServerId(propertiesFile, stateFile, idSupplier, Platform::getManagementServerFingerprint);
    }

    public static synchronized String loadOrCreateManagementServerId(File propertiesFile, File stateFile, Supplier<String> idSupplier, Supplier<String> fingerprintSupplier) {
        Properties properties = loadProperties(propertiesFile);

        String configuredId = properties.getProperty(MANAGEMENT_SERVER_ID_PROPERTY);
        if (isValidManagementServerId(configuredId)) {
            System.setProperty(MANAGEMENT_SERVER_ID_PROPERTY, configuredId);
            return configuredId;
        }

        String currentFingerprint = fingerprintSupplier.get();
        Properties state = loadProperties(stateFile);
        String persistedId = state.getProperty(MANAGEMENT_SERVER_ID_PROPERTY);
        if (isValidManagementServerId(persistedId)) {
            if (canReusePersistedManagementServerId(state, currentFingerprint)) {
                ensureManagementServerFingerprint(stateFile, state, currentFingerprint);
                System.setProperty(MANAGEMENT_SERVER_ID_PROPERTY, persistedId);
                return persistedId;
            }

            logger.warn(String.format("management server id state file[%s] fingerprint does not match current machine, regenerate management server id", stateFile.getAbsolutePath()));
        }

        String generatedId = idSupplier.get();
        if (!isValidManagementServerId(generatedId)) {
            throw new CloudRuntimeException(String.format("generated management server id[%s] is not a valid uuid", generatedId));
        }

        state.setProperty(MANAGEMENT_SERVER_ID_PROPERTY, generatedId);
        setManagementServerFingerprint(state, currentFingerprint);
        saveManagementServerId(stateFile, state);
        System.setProperty(MANAGEMENT_SERVER_ID_PROPERTY, generatedId);
        return generatedId;
    }

    private static Properties loadProperties(File file) {
        Properties properties = new Properties();
        if (file.exists()) {
            try (FileInputStream inputStream = new FileInputStream(file)) {
                properties.load(inputStream);
            } catch (IOException e) {
                throw new CloudRuntimeException(e);
            }
        }
        return properties;
    }

    public static File getManagementServerIdStateFile() {
        String dataDir = System.getProperty(DATA_DIR_PROPERTY);
        if (dataDir == null && Boolean.parseBoolean(System.getProperty(UNIT_TEST_ON_PROPERTY))) {
            dataDir = new File(System.getProperty(JAVA_TMP_DIR_PROPERTY), UNIT_TEST_DATA_DIR_NAME).getAbsolutePath();
        }
        if (dataDir == null) {
            dataDir = DEFAULT_DATA_DIR;
        }

        return new File(dataDir, MANAGEMENT_SERVER_ID_STATE_FILE_NAME);
    }

    private static boolean canReusePersistedManagementServerId(Properties state, String currentFingerprint) {
        if (StringUtils.isBlank(currentFingerprint)) {
            return true;
        }

        String persistedFingerprint = state.getProperty(MANAGEMENT_SERVER_FINGERPRINT_PROPERTY);
        String persistedFingerprintVersion = state.getProperty(MANAGEMENT_SERVER_FINGERPRINT_VERSION_PROPERTY);
        if (StringUtils.isBlank(persistedFingerprint) || !MANAGEMENT_SERVER_FINGERPRINT_VERSION.equals(persistedFingerprintVersion)) {
            return true;
        }

        return currentFingerprint.equals(persistedFingerprint);
    }

    private static void ensureManagementServerFingerprint(File stateFile, Properties state, String currentFingerprint) {
        if (StringUtils.isBlank(currentFingerprint)) {
            return;
        }

        String persistedFingerprint = state.getProperty(MANAGEMENT_SERVER_FINGERPRINT_PROPERTY);
        String persistedFingerprintVersion = state.getProperty(MANAGEMENT_SERVER_FINGERPRINT_VERSION_PROPERTY);
        if (currentFingerprint.equals(persistedFingerprint) && MANAGEMENT_SERVER_FINGERPRINT_VERSION.equals(persistedFingerprintVersion)) {
            return;
        }

        setManagementServerFingerprint(state, currentFingerprint);
        saveManagementServerId(stateFile, state);
    }

    private static void setManagementServerFingerprint(Properties state, String fingerprint) {
        if (StringUtils.isBlank(fingerprint)) {
            state.remove(MANAGEMENT_SERVER_FINGERPRINT_PROPERTY);
            state.remove(MANAGEMENT_SERVER_FINGERPRINT_VERSION_PROPERTY);
            return;
        }

        state.setProperty(MANAGEMENT_SERVER_FINGERPRINT_PROPERTY, fingerprint);
        state.setProperty(MANAGEMENT_SERVER_FINGERPRINT_VERSION_PROPERTY, MANAGEMENT_SERVER_FINGERPRINT_VERSION);
    }

    private static String getManagementServerFingerprint() {
        List<String> identities = new ArrayList<>();
        for (String[] source : MANAGEMENT_SERVER_FINGERPRINT_DMI_SOURCES) {
            String identity = readMachineIdentity(source[DMI_SOURCE_FILE_INDEX]);
            if (identity == null) {
                identity = readDmiMachineIdentity(source[DMI_SOURCE_COMMAND_ARG_INDEX]);
            }
            if (identity != null) {
                identities.add(String.format("%s=%s", source[DMI_SOURCE_NAME_INDEX], identity));
            }
        }

        if (identities.isEmpty()) {
            logger.warn("cannot calculate management server fingerprint because no stable DMI identity source is available");
            return null;
        }

        return MANAGEMENT_SERVER_FINGERPRINT_ALGORITHM + DigestUtils.sha256Hex(StringUtils.join(identities, "\n"));
    }

    private static String readMachineIdentity(String path) {
        File file = new File(path);
        if (!file.isFile() || !file.canRead()) {
            return null;
        }

        try {
            return normalizeMachineIdentity(FileUtils.readFileToString(file));
        } catch (IOException e) {
            logger.warn(String.format("unable to read machine identity file[%s], skip it", path), e);
            return null;
        }
    }

    private static String readDmiMachineIdentity(String dmiString) {
        File dmidecode = new File(DMIDECODE_COMMAND);
        File sudo = new File(SUDO_COMMAND);
        if (!dmidecode.isFile() || !sudo.isFile() || !sudo.canExecute()) {
            return null;
        }

        Process process = null;
        try {
            process = new ProcessBuilder(SUDO_COMMAND, "-n", DMIDECODE_COMMAND, "-s", dmiString).start();
            if (!process.waitFor(DMIDECODE_COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                logger.warn(String.format("unable to read DMI machine identity[%s] before timeout, skip it", dmiString));
                return null;
            }

            String output = IOUtils.toString(process.getInputStream(), "UTF-8");
            String error = IOUtils.toString(process.getErrorStream(), "UTF-8");
            if (process.exitValue() != 0) {
                logger.debug(String.format("unable to read DMI machine identity[%s], exit code[%s], output[%s], error[%s], skip it",
                        dmiString, process.exitValue(), output.trim(), error.trim()));
                return null;
            }

            return normalizeMachineIdentity(output);
        } catch (IOException e) {
            logger.debug(String.format("unable to read DMI machine identity[%s], skip it", dmiString), e);
            return null;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn(String.format("interrupted when reading DMI machine identity[%s], skip it", dmiString), e);
            return null;
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    private static String normalizeMachineIdentity(String output) {
        if (StringUtils.isBlank(output)) {
            return null;
        }

        for (String line : StringUtils.split(output, "\r\n")) {
            String identity = line.trim().toLowerCase();
            if (isUsableMachineIdentity(identity)) {
                return identity;
            }
        }

        return null;
    }

    private static boolean isUsableMachineIdentity(String identity) {
        if (StringUtils.isBlank(identity)) {
            return false;
        }

        if (UNUSABLE_MACHINE_IDENTITIES.contains(identity)) {
            return false;
        }

        String normalized = StringUtils.deleteWhitespace(identity).replace("-", "").replace(":", "");
        return !StringUtils.containsOnly(normalized, "0") && !StringUtils.containsOnly(normalized, "f");
    }

    private static boolean isValidManagementServerId(String id) {
        if (id == null) {
            return false;
        }

        try {
            UUID.fromString(id);
            return true;
        } catch (IllegalArgumentException ignored) {
            return id.matches(ZSTACK_UUID_PATTERN);
        }
    }

    private static void saveManagementServerId(File propertiesFile, Properties properties) {
        if (propertiesFile.getParentFile() != null && !propertiesFile.getParentFile().exists()) {
            try {
                FileUtils.forceMkdir(propertiesFile.getParentFile());
            } catch (IOException e) {
                throw new CloudRuntimeException(e);
            }
        }

        File tmp = new File(propertiesFile.getAbsolutePath() + TEMP_FILE_SUFFIX);
        try (FileOutputStream outputStream = new FileOutputStream(tmp)) {
            properties.store(outputStream, "ZStack properties");
            try {
                Files.move(tmp.toPath(), propertiesFile.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                Files.move(tmp.toPath(), propertiesFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new CloudRuntimeException(e);
        } finally {
            if (tmp.exists()) {
                tmp.delete();
            }
        }
    }

    public static <K extends Enum<K>, T extends Enum<T>> StateMachine<K, T> createStateMachine() {
        return new StateMachineImpl<K, T>();
    }

    public static String getUuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public static String getUuidFromBytes(byte[] name) {
        return UUID.nameUUIDFromBytes(name).toString().replace("-", "");
    }

    public static String getManagementServerIp() {
        if (managementServerIp == null) {
            managementServerIp = getManagementServerIpInternal();
        }

        return managementServerIp;
    }

    public static int getManagementNodeServicePort() {
        return Integer.parseInt(System.getProperty("RESTFacade.port", "8080"));
    }

    public static String getManagementServerVip() {
        if (!ZSha2Helper.isMNHaEnvironment()) {
            return getManagementServerIp();
        }
        return ZSha2Helper.getInfo(false).getDbvip();
    }

    public static String getManagementServerVipBaseUrl() {
        String ipAddress = getManagementServerVip();
        int port = getManagementNodeServicePort();
        String formattedIp;


        if (IPv6NetworkUtils.isIpv6Address(ipAddress)) {
            formattedIp = String.format("[%s]", ipAddress);
        } else {
            formattedIp = ipAddress;
        }

        return String.format("http://%s:%d", formattedIp, port);
    }

    public static String getCanonicalServerIp() {
        if (!ZSha2Helper.isMNHaEnvironment()) {
            return getManagementServerIp();
        }

        return ZSha2Helper.getInfo(false).getNodeip();
    }

    public static boolean isVIPNode() {
        if (!ZSha2Helper.isMNHaEnvironment() || CoreGlobalProperty.MN_VIP == null) {
            return true;
        }

        String vip = CoreGlobalProperty.MN_VIP;
        try {
            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            while (nets.hasMoreElements()) {
                NetworkInterface ifc = nets.nextElement();
                if (!ifc.isUp()) {
                    continue;
                }

                Enumeration<InetAddress> enumAdds = ifc.getInetAddresses();
                while (enumAdds.hasMoreElements()) {
                    if (vip.equals(enumAdds.nextElement().getHostAddress())) {
                        return true;
                    }
                }
            }
        } catch (SocketException e) {
            throw new CloudRuntimeException(e);
        }

        return false;
    }

    private static String getManagementServerCidrInternal(String mgtIp) {
        String command = IPv6NetworkUtils.isIpv6Address(mgtIp) ? IPV6_ADDRESS_COMMAND : IPV4_ADDRESS_COMMAND;
        Linux.ShellResult ret = Linux.shell(command);
        return parseManagementServerCidrFromIpAddressOutput(mgtIp, ret.getStdout());
    }

    public static String parseManagementServerCidrFromIpAddressOutput(String managementIp, String commandOutput) {
        if (commandOutput == null) {
            return null;
        }

        String normalizedManagementIp = normalizeManagementIp(managementIp);
        for (String line : commandOutput.split("\\n")) {
            String[] tokens = line.trim().split("\\s+");
            if (tokens.length < IP_ADDRESS_COMMAND_MIN_TOKEN_COUNT) {
                continue;
            }

            String cidr = tokens[IP_ADDRESS_COMMAND_CIDR_INDEX];
            if (!cidr.contains(CIDR_SEPARATOR)) {
                continue;
            }

            String ip = cidr.substring(0, cidr.indexOf(CIDR_SEPARATOR));
            if (!normalizedManagementIp.equals(normalizeManagementIp(ip))) {
                continue;
            }

            try {
                return NetworkUtils.getNetworkAddressFromCidr(cidr);
            } catch (RuntimeException e) {
                return null;
            }
        }

        return null;
    }

    public static String getManagementServerCidr() {
        if (managementServerCidr == null) {
            managementServerCidr = getManagementServerCidrInternal(getManagementServerIp());
        }

        return managementServerCidr;
    }

    public static String getManagementServerCidr(String managementIp) {
        return getManagementServerCidrInternal(normalizeManagementIp(managementIp));
    }

    public static String getManagementServerCidr(int ipVersion) {
        String currentIp = getManagementServerIp();
        if ((ipVersion == IPv6Constants.IPv6 && IPv6NetworkUtils.isIpv6Address(currentIp)) ||
                (ipVersion == IPv6Constants.IPv4 && NetworkUtils.isIpv4Address(currentIp))) {
            return getManagementServerCidr(currentIp);
        }

        String ip = ipVersion == IPv6Constants.IPv6 ? getManagementServerIp6() : getManagementServerIp4();
        return ip == null ? null : getManagementServerCidr(ip);
    }

    private static String getManagementServerIpInternal() {
        String ip = System.getProperty(MANAGEMENT_SERVER_IP_PROPERTY);
        if (ip != null) {
            logger.info(String.format("get management IP[%s] from Java property[%s]", ip, MANAGEMENT_SERVER_IP_PROPERTY));
            return normalizeManagementIp(ip);
        }

        ip = System.getenv(ZSTACK_MANAGEMENT_SERVER_IP_ENV);
        if (ip != null) {
            logger.info(String.format("get management IP[%s] from environment variable[%s]", ip, ZSTACK_MANAGEMENT_SERVER_IP_ENV));
            return normalizeManagementIp(ip);
        }

        Linux.ShellResult ret = Linux.shell(DEFAULT_ROUTE_COMMAND);
        String defaultLine = null;
        for (String s : ret.getStdout().split("\n")) {
            if (s.contains(DEFAULT_ROUTE_MARK)) {
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
                    ip = selectManagementServerIp(Collections.list(iface.getInetAddresses()));
                }
            }
        } catch (SocketException e) {
            throw new CloudRuntimeException(e);
        }

        if (ip == null) {
            throw new CloudRuntimeException(err);
        }

        logger.info(String.format("get management IP[%s] from default route[/sbin/ip route]", ip));
        return ip;
    }

    public static String getManagementServerIp6() {
        String ip = getManagementServerSecondaryIpProperty(MANAGEMENT_SERVER_IP6_PROPERTY, IPv6Constants.IPv6);
        if (ip != null) {
            return ip;
        }
        return getManagementServerIpOnManagementInterface(IPv6Constants.IPv6);
    }

    public static String getManagementServerIp4() {
        String ip = getManagementServerSecondaryIpProperty(MANAGEMENT_SERVER_IP4_PROPERTY, IPv6Constants.IPv4);
        if (ip != null) {
            return ip;
        }
        return getManagementServerIpOnManagementInterface(IPv6Constants.IPv4);
    }

    private static String getManagementServerSecondaryIpProperty(String property, int ipVersion) {
        String ip = System.getProperty(property);
        if (ip == null || ip.trim().isEmpty()) {
            return null;
        }

        String normalizedIp = normalizeManagementIp(ip);
        if ((ipVersion == IPv6Constants.IPv6 && !IPv6NetworkUtils.isIpv6Address(normalizedIp)) ||
                (ipVersion == IPv6Constants.IPv4 && !NetworkUtils.isIpv4Address(normalizedIp))) {
            throw new CloudRuntimeException(String.format(
                    "management IP[%s] from Java property[%s] is not IPv%s",
                    ip, property, ipVersion));
        }

        logger.info(String.format("get management IP[%s] from Java property[%s]", ip, property));
        return normalizedIp;
    }

    private static String getManagementServerIpOnManagementInterface(int ipVersion) {
        try {
            NetworkInterface iface = findManagementServerInterface();
            if (iface == null || !iface.isUp()) {
                return null;
            }

            for (InetAddress address : Collections.list(iface.getInetAddresses())) {
                if (address.isLoopbackAddress() || address.isLinkLocalAddress()) {
                    continue;
                }
                if (ipVersion == IPv6Constants.IPv6 && !(address instanceof Inet4Address)) {
                    return normalizeManagementIp(address.getHostAddress());
                }
                if (ipVersion == IPv6Constants.IPv4 && address instanceof Inet4Address) {
                    return normalizeManagementIp(address.getHostAddress());
                }
            }
        } catch (SocketException e) {
            throw new CloudRuntimeException(e);
        }

        return null;
    }

    private static NetworkInterface findManagementServerInterface() throws SocketException {
        String currentIp = normalizeManagementIp(getManagementServerIp());
        Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
        for (NetworkInterface iface : Collections.list(nets)) {
            for (InetAddress address : Collections.list(iface.getInetAddresses())) {
                if (currentIp.equals(normalizeManagementIp(address.getHostAddress()))) {
                    return iface;
                }
            }
        }

        return null;
    }

    public static String getManagementServerIp6Cidr() {
        String ip6 = getManagementServerIp6();
        return ip6 == null ? null : getManagementServerCidr(ip6);
    }

    public static List<String> getManagementServerIps() {
        LinkedHashSet<String> ips = new LinkedHashSet<>();
        ips.add(getManagementServerIp());
        ips.add(getManagementServerIp4());
        ips.add(getManagementServerIp6());
        ips.remove(null);
        return new ArrayList<>(ips);
    }

    public static List<String> getManagementServerIpsWithLocalFallback() {
        LinkedHashSet<String> ips = new LinkedHashSet<>(getManagementServerIps());
        ips.addAll(getLocalNonLoopbackIps());
        ips.remove(null);
        return new ArrayList<>(ips);
    }

    private static List<String> getLocalNonLoopbackIps() {
        List<String> ips = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface iface : Collections.list(nets)) {
                if (!iface.isUp()) {
                    continue;
                }
                for (InetAddress address : Collections.list(iface.getInetAddresses())) {
                    if (address.isLoopbackAddress() || address.isLinkLocalAddress()) {
                        continue;
                    }
                    ips.add(normalizeManagementIp(address.getHostAddress()));
                }
            }
        } catch (SocketException e) {
            logger.warn("failed to list local non-loopback IPs", e);
        }
        return ips;
    }

    public static String getRouteSourceIp(String remoteIp) {
        if (StringUtils.isBlank(remoteIp)) {
            return null;
        }

        remoteIp = normalizeManagementIp(remoteIp);
        String family;
        if (IPv6NetworkUtils.isIpv6Address(remoteIp)) {
            family = "-6";
        } else if (NetworkUtils.isIpv4Address(remoteIp)) {
            family = "-4";
        } else {
            return null;
        }

        Linux.ShellResult ret = Linux.shell(String.format("/sbin/ip %s route get %s", family, remoteIp));
        if (ret.getExitCode() != 0) {
            logger.warn(String.format("failed to get route source IP for remote[%s], stdout[%s], stderr[%s]",
                    remoteIp, ret.getStdout(), ret.getStderr()));
            return null;
        }

        String[] tokens = ret.getStdout().trim().split("\\s+");
        for (int i = 0; i < tokens.length - 1; i++) {
            if (!"src".equals(tokens[i])) {
                continue;
            }
            String sourceIp = normalizeManagementIp(tokens[i + 1]);
            if (IPv6NetworkUtils.isIpv6Address(remoteIp) && IPv6NetworkUtils.isIpv6Address(sourceIp)) {
                return sourceIp;
            }
            if (NetworkUtils.isIpv4Address(remoteIp) && NetworkUtils.isIpv4Address(sourceIp)) {
                return sourceIp;
            }
        }

        return null;
    }

    public static String selectManagementServerIp(Collection<InetAddress> addresses) {
        String ipv4 = null;
        String ipv6 = null;

        for (InetAddress address : addresses) {
            String hostAddress = normalizeManagementIp(address.getHostAddress());
            if (address.isLoopbackAddress() || address.isLinkLocalAddress()) {
                continue;
            }

            if (address instanceof Inet4Address) {
                ipv4 = hostAddress;
            } else {
                ipv6 = hostAddress;
            }
        }

        return ipv4 != null ? ipv4 : ipv6;
    }

    public static String formatJGroupsInitialHosts(String nodeIp, String peerIp, int port) {
        return String.format(JGROUPS_INITIAL_HOST_FORMAT,
                IPv6NetworkUtils.stripHostUrlBrackets(nodeIp), port,
                IPv6NetworkUtils.stripHostUrlBrackets(peerIp), port);
    }

    private static String normalizeManagementIp(String ip) {
        if (ip == null) {
            return null;
        }
        int scopeIndex = ip.indexOf('%');
        if (scopeIndex >= 0) {
            ip = ip.substring(0, scopeIndex);
        }
        return IPv6NetworkUtils.isIpv6Address(ip) ? IPv6NetworkUtils.normalizeIpv6(ip) : ip;
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

    private static volatile boolean slowElaborationWired = false;

    private static ErrorCodeElaboration elaborate(String fmt, Object...args) {
        if (!slowElaborationWired) {
            StringSimilarity.slowElaborationThresholdMs = CoreGlobalProperty.ELABORATION_SLOW_THRESHOLD_MS;
            slowElaborationWired = true;
        }

        try {
            ErrorCodeElaboration elaboration = StringSimilarity.findSimilar(fmt, args);
            if (elaboration == null) {
                return null;
            }
            if (StringSimilarity.matched(elaboration)) {
                return elaboration;
            }
        } catch (Throwable e) {
            logger.warn("exception happened when found elaboration");
            logger.warn(e.getMessage());
        }
        return null;
    }

    private static List<Enum> allowCode = CollectionDSL.list(IdentityErrors.INVALID_SESSION);

    public static ErrorCode err(String globalErrorCode, Enum errCode, String fmt, Object...args) {
        return err(globalErrorCode, errCode, null, fmt, args);
    }

    public static ErrorCode err(String globalErrorCode, Enum errCode, ErrorCode cause, String fmt, Object...args) {
        ErrorFacade errf = getComponentLoader().getComponent(ErrorFacade.class);
        String details = null;
        if (fmt != null) {
            try {
                details = SysErrors.INTERNAL == errCode ? String.format(fmt, args) : toI18nString(fmt, args);
            } catch (Exception e) {
                logger.warn("exception happened when format error message");
                logger.warn(e.getMessage());
                details = fmt;
            }
        }

        ErrorCode result = errf.instantiateErrorCode(errCode, details, cause);
        handleErrorElaboration(errCode, fmt, result, cause, args);
        addErrorCounter(result);
        result.setGlobalErrorCode(globalErrorCode);
        if (args != null && args.length > 0) {
            result.setFormatArgs(java.util.Arrays.stream(args)
                    .map(a -> a == null ? "null" : a.toString())
                    .toArray(String[]::new));
        }

        // populate message at creation time with default locale;
        // RestServer will override with client's Accept-Language if different
        try {
            ComponentLoader currentLoader = loader;
            if (currentLoader != null) {
                GlobalErrorCodeI18nService i18nService = currentLoader.getComponent(GlobalErrorCodeI18nService.class);
                if (i18nService != null) {
                    i18nService.localizeErrorCode(result, org.zstack.core.errorcode.LocaleUtils.DEFAULT_LOCALE);
                }
            }
        } catch (Exception e) {
            // i18n service not initialized during early startup
        }
        if (result.getMessage() == null) {
            result.setMessage(details != null ? details : result.getDescription());
        }

        return result;
    }

    private static void findElaborationFromCoreError(ErrorCode cause, ErrorCode result) {
        ErrorCode coreError = cause == null ? getCoreError(result) : getCoreError(cause);
        // use the core cause as elaboration if it existed
        if (coreError.getElaboration() != null) {
            result.setCost(coreError.getCost());
            result.setElaboration(coreError.getElaboration());
            result.setMessages(coreError.getMessages());
        } else if (cause instanceof ErrorCodeList && ((ErrorCodeList) cause).getCauses() != null) {
            // suppose elaborations are existed in causes...
            ErrorCodeList errList = (ErrorCodeList) cause;
            String costs = null;
            String elas = null;
            ErrorCodeElaboration messages = null;
            for (ErrorCode c: errList.getCauses()) {
                ErrorCode lcError = getCoreError(c);
                if (lcError.getElaboration() != null && !lcError.getElaboration().equals(elas) && !lcError.getMessages().equals(messages)) {
                    costs = costs == null ? lcError.getCost() : addTwoCosts(costs, lcError.getCost());
                    elas = elas == null ? lcError.getElaboration() : String.join(",", elas, lcError.getElaboration());
                    messages = messages == null ? lcError.getMessages() : messages.addElaborationMessage(lcError.getMessages());
                }
            }
            result.setCost(costs);
            result.setElaboration(elas);
            result.setMessages(messages);
        }
    }

    private static void generateElaboration(Enum errCode, ErrorCode result, String fmt, Object...args) {
        // try to find same error with fmt and args
        ErrorCodeElaboration ela = elaborate(fmt, args);

        // only elaborate the error code in allowCode if fmt missed
        if (ela == null && allowCode.contains(errCode)) {
            ela = elaborate(result.getDescription());
        }

        // failed to find elaboration, add the error code fmt string to missed list
        if (ela == null) {
            if (args != null) {
                StringSimilarity.addMissed(String.format(fmt, args));
            } else {
                StringSimilarity.addMissed(fmt);
            }

            // note: if allowCode failed to find elaboration,
            // we still need to add the description to missed list
            if (allowCode.contains(errCode)) {
                StringSimilarity.addMissed(result.getDescription());
            }

            return;
        }

        String prefix, msg;
        if (locale.equals(Locale.SIMPLIFIED_CHINESE)) {
            prefix = "错误信息: %s\n";
            msg = ela.getMessage_cn();
        } else {
            prefix = "Error message: %s\n";
            msg = ela.getMessage_en();
        }

        // tricky code that we treat the only one args error maybe use the cause or
        // error from other component directly, so we need to check if the args is
        // matched with the regex at first
        if (args != null && args.length == 1 && StringSimilarity.isRegexMatched(ela.getRegex(), String.valueOf(args[0]))) {
            result.setMessages(new ErrorCodeElaboration(ela, locale));
            String formatError = String.format(prefix, args[0]);
            result.setElaboration(StringSimilarity.formatElaboration(formatError));
        } else {
            result.setMessages(new ErrorCodeElaboration(ela, locale, args));
            result.setElaboration(StringSimilarity.formatElaboration(String.format(prefix, msg), args));
        }

        StringSimilarity.addErrors(fmt, ela);
    }

    private static void handleErrorElaboration(Enum errCode, String fmt, ErrorCode result, ErrorCode cause, Object...args) {
        if (!CoreGlobalProperty.ENABLE_ELABORATION) {
            return;
        }

        // start to generate elaboration...
        try {
            findElaborationFromCoreError(cause, result);

            // if the elaboration is not found, try to generate it
            if (result.getElaboration() == null && cause == null) {
                long start = System.currentTimeMillis();
                generateElaboration(errCode, result, fmt, args);
                result.setCost((System.currentTimeMillis() - start) + "ms");
            }
        } catch (Throwable e) {
            logger.warn("exception happened when found elaboration");
            logger.warn(e.getMessage());
        }
    }

    private static String addTwoCosts(String origin, String increase) {
        long c1 = Long.parseLong(origin.substring(0, origin.length() - 2).trim());
        long c2 = Long.parseLong(increase.substring(0, increase.length() - 2).trim());
        return (c1 + c2) + "ms";
    }

    private static ErrorCode getCoreError(ErrorCode result) {
        if (result.getCause() == null) {
            return result;
        } else {
            return getCoreError(result.getCause());
        }
    }

    public static String missingVariables(Object...args) {
        if (args.length == 1) {
            return String.format("[%s] is required", args[0]);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (Object arg : args) {
            if (arg == null) {
                continue;
            }

            sb.append(arg).append(", ");
        }
        sb.append("] ");
        return sb.append("are required").toString();
    }

    public static ErrorCode inerr(String globalErrorCode, String fmt, Object...args) {
        return err(globalErrorCode, SysErrors.INTERNAL, fmt, args);
    }

    // format error code from expand components
    public static ErrorCode experr(String globalErrorCode, String fmt, String err, Object...args) {
        return operr(globalErrorCode, fmt, err, args);
    }

    public static ErrorCode operr(String globalErrorCode, String fmt, Object...args) {
        return err(globalErrorCode, SysErrors.OPERATION_ERROR, fmt, args);
    }

    public static ErrorCode operr(String globalErrorCode, ErrorCode cause, String fmt, Object...args) {
        return err(globalErrorCode, SysErrors.OPERATION_ERROR, cause, fmt, args);
    }

    public static ErrorCode canerr(String globalErrorCode, String fmt, Object...args) {
        return err(globalErrorCode, SysErrors.CANCEL_ERROR, fmt, args);
    }

    public static ErrorCode argerr(String globalErrorCode, String fmt, Object...args) {
        return err(globalErrorCode, SysErrors.INVALID_ARGUMENT_ERROR, fmt, args);
    }

    public static ErrorCode touterr(String globalErrorCode, String fmt, Object...args) {
        return err(globalErrorCode, SysErrors.TIMEOUT, fmt, args);
    }

    public static ErrorCode touterr(String globalErrorCode, ErrorCode cause, String fmt, Object...args) {
        return err(globalErrorCode, SysErrors.TIMEOUT, cause, fmt, args);
    }

    public static ErrorCode ioerr(String globalErrorCode, String fmt, Object...args) {
        return err(globalErrorCode, SysErrors.IO_ERROR, fmt, args);
    }

    public static ErrorCode httperr(String globalErrorCode, String fmt, Object...args) {
        return err(globalErrorCode, SysErrors.HTTP_ERROR, fmt, args);
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

    public static final String SKIP_STOP = "skip.mn.exit";

    public static void exit(String reason) {
        new BootErrorLog().write(reason);
        System.setProperty(EXIT_REASON, reason);
        System.exit(1);
    }

    public static String randomAlphanumeric(int count) {
        return RandomStringUtils.randomAlphanumeric(count);
    }

    public static boolean isAfterManagementNodeStart(Timestamp ts) {
        RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
        Timestamp startMnTime = new Timestamp(bean.getStartTime());
        return ts.after(startMnTime);
    }

    public static void addErrorCounter(ErrorCode code) {
        errorCounter.compute(code.getCode().split("\\.")[0], (k, v) -> v == null ? 1 : v ++);
    }

    public static Map<String, Double> getErrorCounter() {
        return errorCounter;
    }

    public static boolean isSimulatorOn() {
        return StartMode.SIMULATOR.toString().equals(CoreGlobalProperty.START_MODE) || CoreGlobalProperty.SIMULATORS_ON;
    }

    public static boolean isMinimalOn() {
        return StartMode.MINIMAL.toString().equals(CoreGlobalProperty.START_MODE);
    }
}
