package org.zstack.core.plugin;

import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.abstraction.PluginDriver;
import org.zstack.abstraction.PluginValidator;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.external.plugin.*;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.managementnode.ManagementNodeVO;
import org.zstack.header.managementnode.ManagementNodeVO_;
import org.zstack.header.message.APIDeleteMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.utils.Bash;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static org.zstack.core.Platform.operr;

/**
 * PluginManagerImpl implementation of PluginManager.
 */
public class PluginManagerImpl extends AbstractService implements PluginManager {
    private static final CLogger logger = Utils.getLogger(PluginManagerImpl.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ThreadFacade thdf;

    private final Set<Class<? extends PluginDriver>> pluginMetadata = new HashSet<>();
    private final Map<String, PluginDriver> pluginInstances = new ConcurrentHashMap<>();
    private final Map<Class<? extends PluginDriver>, List<PluginDriver>>
            pluginRegisters = new HashMap<>();
    private final Map<Class<? extends PluginDriver>, PluginValidator>
            pluginValidators = new HashMap<>();
    private String fileDirPath = PathUtil.join(CoreGlobalProperty.DATA_DIR, "/plugins/");

    public String getFileDirPath() {
        return fileDirPath;
    }

    public void setFileDirPath(String fileDirPath) {
        this.fileDirPath = fileDirPath;
    }

    private void collectPluginProtocolMetadata() {
        ConfigurationBuilder builder = ConfigurationBuilder.build()
                .setUrls(ClasspathHelper.forPackage("org.zstack"))
                .setScanners(Scanners.SubTypes, Scanners.MethodsAnnotated,
                        Scanners.FieldsAnnotated, Scanners.TypesAnnotated,
                        Scanners.MethodsParameter)
                .setExpandSuperTypes(false)
                .filterInputsBy(new FilterBuilder().includePackage("org.zstack"));
        Reflections reflections = new Reflections(builder);
        reflections.getSubTypesOf(PluginDriver.class).forEach(clz -> {
            if (!clz.getCanonicalName().contains("org.zstack.abstraction")
                    || !clz.isInterface()) {
                return;
            }

            if (pluginMetadata.contains(clz)) {
                throw new CloudRuntimeException(
                        String.format("duplicate PluginProtocol[name: %s]", clz));
            }

            pluginMetadata.add(clz);
        });
    }

    private void registerPluginAsSingleton(
            Class<? extends PluginDriver> pluginRegisterClz,
            Class<? extends PluginDriver> pluginDriverClz) {
        try {
            PluginDriver pluginDriver = pluginRegisterClz
                    .getConstructor()
                    .newInstance();

            if (pluginValidators.containsKey(pluginRegisterClz)) {
                pluginValidators.get(pluginRegisterClz).validate(pluginDriver);
            }

            // String format all String methods of plugin from pluginRegister to logger.debug
            logger.debug(String.format("%s[class: %s, productKey: %s, version: %s," +
                            " capabilities: %s, description: %s, vendor: %s, url: %s," +
                            " license: %s]",
                    pluginInstances.containsKey(pluginDriver.uuid()) ? "reload plugin" : "register plugin",
                    pluginRegisterClz,
                    pluginDriver.uuid(),
                    pluginDriver.version(),
                    JSONObjectUtil.toJsonString(pluginDriver.features()),
                    pluginDriver.description(),
                    pluginDriver.vendor(),
                    pluginDriver.url(),
                    pluginDriver.license()));

            verifyPluginProduct(pluginDriver);

            pluginInstances.put(pluginDriver.uuid(), pluginDriver);
            pluginRegisters.computeIfAbsent(pluginDriverClz, k -> new ArrayList<>());
            pluginRegisters.get(pluginDriverClz).add(pluginDriver);

            PluginDriverVO vo = dbf.findByUuid(pluginDriver.uuid(), PluginDriverVO.class);
            if (vo == null) {
                vo = new PluginDriverVO();
                vo.setUuid(pluginDriver.uuid());
                vo.setName(pluginDriver.name());
                vo.setVendor(pluginDriver.vendor());
                vo.setFeatures(JSONObjectUtil.toJsonString(pluginDriver.features()));
                vo.setType(pluginDriver.type());
                vo.setDescription(pluginDriver.description());
                vo.setVersion(pluginDriver.version());
                vo.setLicense(pluginDriver.license());
                vo.setOptionTypes(JSONObjectUtil.toJsonString(pluginDriver.optionTypes()));
                dbf.persist(vo);
            } else {
                vo.setName(pluginDriver.name());
                vo.setVendor(pluginDriver.vendor());
                vo.setFeatures(JSONObjectUtil.toJsonString(pluginDriver.features()));
                vo.setType(pluginDriver.type());
                vo.setDescription(pluginDriver.description());
                vo.setVersion(pluginDriver.version());
                vo.setLicense(pluginDriver.license());
                vo.setOptionTypes(JSONObjectUtil.toJsonString(pluginDriver.optionTypes()));
                dbf.update(vo);
            }
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }

    private void getPluginInterfaceSingletons(Class<? extends PluginDriver> abstractPluginClz) {
        Platform.getReflections()
                .getSubTypesOf(abstractPluginClz)
                .forEach(pluginDriverClz -> {
                    if (pluginDriverClz.isInterface()) {
                        return;
                    }

                    registerPluginAsSingleton(pluginDriverClz, abstractPluginClz);
                });
    }

    private void loadPluginsFromMetadata() {
        pluginMetadata.forEach(this::getPluginInterfaceSingletons);

        pluginRegisters.forEach((pluginClazz, instanceList) -> {
            PluginValidator validator = pluginValidators.get(pluginClazz);
            if (validator == null) {
                return;
            }

            validator.validateAllPlugins(instanceList);
        });
    }

    private void verifyPluginProduct(PluginDriver pluginDriver) {
        if (!PluginGlobalConfig.ALLOW_UNKNOWN_PRODUCT_PLUGIN.value(Boolean.class)
                && pluginDriver.uuid() == null) {
            throw new OperationFailureException(operr("unknown product plugin name: %s",
                    pluginDriver.name()));
        }

        if (pluginDriver.name() == null
                || pluginDriver.uuid() == null
                || pluginDriver.vendor() == null) {
            throw new OperationFailureException(operr("plugin[%s] name," +
                    " productKey and vendor cannot be null",
                    pluginDriver.getClass()));
        }

        doVerification(pluginDriver.name(), pluginDriver.uuid());
    }

    private void doVerification(String productName, String productKey) {
        // TODO: verify plugin driver
    }

    private void collectPluginValidators(Class<?> validatorClazz) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        PluginValidator pluginValidator = ((Class<? extends PluginValidator>) validatorClazz).getConstructor().newInstance();
        pluginValidators.put(pluginValidator.pluginClass(), pluginValidator);
    }

    @Override
    public boolean start() {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return true;
        }
        new Bash() {
            @Override
            protected void scripts() {
                mkdirs(fileDirPath);
            }
        }.execute();
        scanAndLoadPlugins(fileDirPath);
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public boolean isFeatureSupported(String pluginProductKey,
                                         String capability) {
        if (logger.isTraceEnabled()) {
            logger.trace(String.format("check plugin[%s] capability[%s]",
                    pluginProductKey, capability));
            logger.trace(String.format("plugin features %s",
                    JSONObjectUtil.toJsonString(pluginInstances
                            .get(pluginProductKey)
                            .features())));
            logger.trace(String.format("plugin feature state: %s",
                    JSONObjectUtil.toJsonString(pluginInstances
                            .get(pluginProductKey)
                            .features().get(capability) == Boolean.TRUE)));
        }

        if (pluginInstances.get(pluginProductKey)
                .features() == null) {
            return true;
        }

        return pluginInstances.get(pluginProductKey)
                .features()
                .get(capability) == Boolean.TRUE;
    }

    @Override
    public <T extends PluginDriver> T getPlugin(String pluginProductKey) {
        if (!pluginInstances.containsKey(pluginProductKey)) {
            throw new CloudRuntimeException(String.format("Unsupported plugin %s",
                    pluginProductKey));
        }

        return (T) pluginInstances.get(pluginProductKey);
    }

    @Override
    public <T extends PluginDriver> List<T> getPluginList(Class<? extends PluginDriver> pluginClass) {
        return (List<T>) pluginRegisters.get(pluginClass);
    }

    @Override
    public boolean isPluginTypeExist(Class<? extends PluginDriver> pluginClass, String type) {
        return pluginRegisters.get(pluginClass)
                .stream()
                .anyMatch(plugin -> plugin.type().equals(type));
    }

    @Override
    public <T extends PluginDriver> T getPlugin(Class<? extends PluginDriver> pluginClass, String type) {
        if (pluginRegisters.get(pluginClass)
                .stream()
                .filter(plugin -> plugin.type().equals(type))
                .count() > 1) {
            throw new CloudRuntimeException(String.format("multi plugin with same type %s", type));
        }

        return (T) pluginRegisters.get(pluginClass)
                .stream()
                .filter(plugin -> plugin.type().equals(type))
                .findFirst()
                .orElse(null);
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg instanceof APIRefreshPluginDriversMsg) {
            handle((APIRefreshPluginDriversMsg) msg);
        } else if (msg instanceof APIDeletePluginDriversMsg) {
            handle((APIDeletePluginDriversMsg) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof RefreshPluginDriversMsg) {
            handle((RefreshPluginDriversMsg) msg);
        } else if (msg instanceof DeletePluginDriversMsg) {
            handle((DeletePluginDriversMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    protected List<File> getJarFiles(String dirPath) {
        File dir = new File(dirPath);
        List<File> jarFiles = new ArrayList<>();

        if (!dir.exists() || !dir.isDirectory()) {
            return jarFiles;
        }

        File[] files = dir.listFiles((file) -> file.isFile() && file.getName().endsWith(".jar"));
        if (files != null) {
            for (File file : files) {
                jarFiles.add(file);
            }
        }
        return jarFiles;
    }

    protected void loadPluginsFromJar(File jarFile) {
        URL jarUrl = null;
        try {
            jarUrl = jarFile.toURI().toURL();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        try (URLClassLoader classLoader = new URLClassLoader(new URL[]{jarUrl}, getClass().getClassLoader())) {
            try (JarFile jar = new JarFile(jarFile)) {
                Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String entryName = entry.getName();
                    if (!entryName.endsWith(".class")) {
                        continue;
                    }

                    if (entryName.matches(".*\\d.*")) {
                        // eg: org.zstack.CipherOnCloudCryptoPlugin$1.class
                        continue;
                    }

                    if (entryName.contains("Test")) {
                        continue;
                    }

                    String className = entryName.replace('/', '.').substring(0, entryName.length() - 6);
                    Class<?> tempClazz = classLoader.loadClass(className);
                    if (className.contains("Validator")) {
                        collectPluginValidators(tempClazz);
                        continue;
                    }

                    // Skip anonymous classes or inner classes (which are named with $)
                    // Anonymous classes or inner classes are typically not needed for plugin registration
                    if (entryName.contains("$")) {
                        continue;
                    }

                    registerPluginAsSingleton((Class<? extends PluginDriver>) tempClazz, (Class<? extends PluginDriver>) tempClazz.getInterfaces()[0]);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void scanAndLoadPlugins(String directoryPath) {
        List<File> jarFiles = getJarFiles(directoryPath);
        for (File jarFile : jarFiles) {
            loadPluginsFromJar(jarFile);
        }
    }

    private void handle(RefreshPluginDriversMsg msg) {
        RefreshPluginDriversReply reply = new RefreshPluginDriversReply();
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return getName();
            }

            @Override
            public void run(SyncTaskChain chain) {
                scanAndLoadPlugins(fileDirPath);
                bus.reply(msg, reply);
                chain.next();
            }

            @Override
            public String getName() {
                return String.format("refresh-%s-plugin-drivers", fileDirPath);
            }
        });
    }

    private void handle(DeletePluginDriversMsg msg) {
        DeletePluginDriversReply reply = new DeletePluginDriversReply();
        pluginInstances.remove(msg.getUuid());
        if (msg.getDeletionMode() == APIDeleteMessage.DeletionMode.Permissive) {
            SQL.New(PluginDriverVO.class).eq(PluginDriverVO_.uuid, msg.getUuid()).set(PluginDriverVO_.deleted, true).update();
        } else {
            SQL.New(PluginDriverVO.class).eq(PluginDriverVO_.uuid, msg.getUuid()).hardDelete();
        }
        bus.reply(msg, reply);
    }

    private void handle(APIDeletePluginDriversMsg msg) {
        APIDeletePluginDriversEvent event = new APIDeletePluginDriversEvent(msg.getId());
        new While<>(Q.New(ManagementNodeVO.class).select(ManagementNodeVO_.uuid).listValues()).step((mnUuid, com) -> {
            DeletePluginDriversMsg rmsg = new DeletePluginDriversMsg();
            rmsg.setUuid(msg.getUuid());
            rmsg.setDeletionMode(msg.getDeletionMode());
            bus.makeServiceIdByManagementNodeId(rmsg, SERVICE_ID, (String) mnUuid);
            bus.send(rmsg, new CloudBusCallBack(com) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        com.addError(reply.getError());
                        com.allDone();
                        return;
                    }

                    com.done();
                }
            });
        }, 2).run(new WhileDoneCompletion(msg) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errorCodeList.getCauses().isEmpty()) {
                    event.setError(errorCodeList.getCauses().get(0));
                }

                bus.publish(event);
            }
        });
    }

    private void handle(APIRefreshPluginDriversMsg msg) {
        APIRefreshPluginDriversEvent event = new APIRefreshPluginDriversEvent(msg.getId());
        new While<>(Q.New(ManagementNodeVO.class).select(ManagementNodeVO_.uuid).listValues()).step((mnUuid, com) -> {
            RefreshPluginDriversMsg rmsg = new RefreshPluginDriversMsg();
            bus.makeServiceIdByManagementNodeId(rmsg, SERVICE_ID, (String) mnUuid);
            bus.send(rmsg, new CloudBusCallBack(com) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        com.addError(reply.getError());
                        com.allDone();
                        return;
                    }

                    com.done();
                }
            });
        }, 2).run(new WhileDoneCompletion(msg) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errorCodeList.getCauses().isEmpty()) {
                    event.setError(errorCodeList.getCauses().get(0));
                }

                bus.publish(event);
            }
        });
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(SERVICE_ID);
    }
}
