package org.zstack.core.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.GLock;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.Message;
import org.zstack.utils.BeanUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.TypeUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.zstack.core.Platform.argerr;

public class GlobalConfigFacadeImpl extends AbstractService implements GlobalConfigFacade {
    private static final CLogger logger = Utils.getLogger(GlobalConfigFacadeImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;

    private JAXBContext context;
    private Map<String, GlobalConfig> allConfigs = new ConcurrentHashMap<>();

    private static final String CONFIG_FOLDER = "globalConfig";
    private static final String OTHER_CATEGORY = "Others";
    private static final String LOCK = "GlobalFacade.lock";

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIUpdateGlobalConfigMsg) {
            handle((APIUpdateGlobalConfigMsg) msg);
        } else if (msg instanceof APIListGlobalConfigMsg) {
            handle((APIListGlobalConfigMsg) msg);
        } else if (msg instanceof APIGetGlobalConfigMsg) {
            handle((APIGetGlobalConfigMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIGetGlobalConfigMsg msg) {
        GlobalConfig c = allConfigs.get(msg.getIdentity());
        APIGetGlobalConfigReply reply = new APIGetGlobalConfigReply();
        if (c == null) {
            ErrorCode err = argerr("unable to find GlobalConfig[category:%s, name:%s]", msg.getCategory(), msg.getName());
            reply.setError(err);
        } else {
            GlobalConfigInventory inv = GlobalConfigInventory.valueOf(c);
            reply.setInventory(inv);
        }
        bus.reply(msg, reply);
    }

    private List<GlobalConfigVO> listGlobalConfig(APIListGlobalConfigMsg msg) {
        return dbf.listAll(msg.getOffset(), msg.getLength(), GlobalConfigVO.class);
    }

    private void handle(APIListGlobalConfigMsg msg) {
        APIListGlobalConfigReply reply = new APIListGlobalConfigReply();

        List<GlobalConfigVO> vos = listGlobalConfig(msg);
        GlobalConfigInventory[] invs = new GlobalConfigInventory[vos.size()];
        for (int i = 0; i < vos.size(); i++) {
            invs[i] = GlobalConfigInventory.valueOf(vos.get(i));
        }

        reply.setInventories(invs);
        bus.reply(msg, reply);
    }

    private void handle(APIUpdateGlobalConfigMsg msg) {
        APIUpdateGlobalConfigEvent evt = new APIUpdateGlobalConfigEvent(msg.getId());
        GlobalConfig globalConfig = allConfigs.get(msg.getIdentity());
        if (globalConfig == null) {
            ErrorCode err = argerr("Unable to find GlobalConfig[category: %s, name: %s]", msg.getCategory(), msg.getName());
            evt.setError(err);
            bus.publish(evt);
            return;
        }

        try {
            globalConfig.updateValue(msg.getValue());

            GlobalConfigInventory inv = GlobalConfigInventory.valueOf(globalConfig.reload());
            evt.setInventory(inv);
        } catch (GlobalConfigException e) {
            evt.setError(argerr(e.getMessage()));
            logger.warn(e.getMessage(), e);
        }
        
        bus.publish(evt);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(GlobalConfigConstant.SERVICE_ID);
    }

    @Override
    public boolean start() {
        class GlobalConfigInitializer {
            Map<String, GlobalConfig> configsFromXml = new HashMap<String, GlobalConfig>();
            Map<String, GlobalConfig> configsFromDatabase = new HashMap<String, GlobalConfig>();
            List<Field> globalConfigFields = new ArrayList<Field>();

            void init() {
                GLock lock = new GLock(LOCK, 320);
                lock.lock();
                try {
                    parseGlobalConfigFields();
                    loadConfigFromXml();
                    loadConfigFromJava();
                    loadConfigFromDatabase();
                    createValidatorForBothXmlAndDatabase();
                    validateConfigFromXml();
                    validateConfigFromDatabase();
                    persistConfigInXmlButNotInDatabase();
                    mergeXmlDatabase();
                    initAllConfig();
                    link();
                    allConfigs.putAll(configsFromXml);
                    // re-validate after merging xml's with db's
                    validateAll();
                } catch (IllegalArgumentException ie) {
                    throw ie;
                } catch (Exception e) {
                    throw new CloudRuntimeException(e);
                } finally {
                    lock.unlock();
                }
            }

            private void parseGlobalConfigFields() {
                List<Class> definitionClasses = BeanUtils.scanClass("org.zstack", GlobalConfigDefinition.class);
                for (Class def : definitionClasses) {
                    for (Field field : def.getDeclaredFields()) {
                        if (Modifier.isStatic(field.getModifiers()) && GlobalConfig.class.isAssignableFrom(field.getType())) {
                            field.setAccessible(true);

                            try {
                                GlobalConfig config = (GlobalConfig) field.get(null);
                                if (config == null) {
                                    throw new CloudRuntimeException(String.format("GlobalConfigDefinition[%s] defines a null GlobalConfig[%s]." +
                                                    "You must assign a value to it using new GlobalConfig(category, name)",
                                            def.getClass().getName(), field.getName()));
                                }

                                globalConfigFields.add(field);
                            } catch (IllegalAccessException e) {
                                throw new CloudRuntimeException(e);
                            }
                        }
                    }
                }
            }

            private void loadConfigFromJava() {
                for (Field field : globalConfigFields) {
                    try {
                        GlobalConfig config = (GlobalConfig) field.get(null);
                        if (config == null) {
                            throw new CloudRuntimeException(String.format("GlobalConfigDefinition[%s] defines a null GlobalConfig[%s]." +
                                            "You must assign a value to it using new GlobalConfig(category, name)",
                                    field.getDeclaringClass().getClass().getName(), field.getName()));
                        }

                        GlobalConfigDef d = field.getAnnotation(GlobalConfigDef.class);
                        if (d == null) {
                            continue;
                        }

                        GlobalConfig c = new GlobalConfig();
                        c.setCategory(config.getCategory());
                        c.setName(config.getName());
                        c.setDescription(d.description());
                        c.setDefaultValue(d.defaultValue());
                        c.setValue(d.defaultValue());
                        c.setType(d.type().getName());
                        if (!"".equals(d.validatorRegularExpression())) {
                            c.setValidatorRegularExpression(d.validatorRegularExpression());
                        }

                        if (configsFromXml.containsKey(c.getIdentity())) {
                            throw new CloudRuntimeException(String.format("duplicate global configuration. %s defines a" +
                                    " global config[category: %s, name: %s] that has been defined by a XML configure or" +
                                    " another java class", field.getDeclaringClass().getName(), c.getCategory(), c.getName()));
                        }

                        configsFromXml.put(c.getIdentity(), c);
                    } catch (IllegalAccessException e) {
                        throw new CloudRuntimeException(e);
                    }
                }
            }

            private void initAllConfig() {
                for (GlobalConfig config : configsFromXml.values()) {
                    config.init();
                }
            }

            private void mergeXmlDatabase() {
                for (GlobalConfig g : configsFromDatabase.values()) {
                    GlobalConfig x = configsFromXml.get(g.getIdentity());
                    if (x == null) {
                        configsFromXml.put(g.getIdentity(), g);
                    } else {
                        x.setValue(g.value());
                        x.setDefaultValue(g.getDefaultValue());
                    }
                }
            }

            private void validateAll() {
                for (GlobalConfig g : allConfigs.values()) {
                    try {
                        g.validate();
                    } catch (Exception e) {
                        throw new IllegalArgumentException(String.format("exception happened when validating global config:\n%s", g.toString()), e);
                    }
                }
            }

            private void validateConfigFromDatabase() {
                logger.debug(String.format("validating global config loaded from database"));
                for (GlobalConfig g : configsFromDatabase.values()) {
                    g.validate();
                }
            }

            private void validateConfigFromXml() {
                logger.debug(String.format("validating global config loaded from XML files"));
                for (GlobalConfig g : configsFromXml.values()) {
                    g.validate();
                }
            }

            private void persistConfigInXmlButNotInDatabase() {
                List<GlobalConfigVO> toSave = new ArrayList<GlobalConfigVO>();
                for (GlobalConfig config : configsFromXml.values()) {
                    if (configsFromDatabase.containsKey(config.getIdentity())) {
                        continue;
                    }

                    logger.debug(String.format("Add a new global config to database: %s", config.toString()));
                    toSave.add(config.toVO());
                }

                if (!toSave.isEmpty()) {
                    dbf.persistCollection(toSave);
                }
            }

            private void loadConfigFromDatabase() {
                List<GlobalConfigVO> vos = dbf.listAll(GlobalConfigVO.class);
                for (GlobalConfigVO vo : vos) {
                    GlobalConfig c = GlobalConfig.valueOf(vo);
                    configsFromDatabase.put(c.getIdentity(), c);
                }
            }

            private void loadConfigFromXml() throws JAXBException, FileNotFoundException {
                context = JAXBContext.newInstance("org.zstack.core.config.schema");
                List<String> filePaths = PathUtil.scanFolderOnClassPath(CONFIG_FOLDER);
                for (String path : filePaths) {
                    File f = new File(path);
                    parseConfig(f);
                }
            }

            private void createValidator(final GlobalConfig g) throws ClassNotFoundException {
                g.installValidateExtension(new GlobalConfigValidatorExtensionPoint() {
                    Class<?> typeClass;
                    Method typeClassValueOfMethod;
                    String regularExpression;

                    {
                        if (g.getType() != null) {
                            typeClass = Class.forName(g.getType());

                            try {
                                typeClassValueOfMethod = typeClass.getMethod("valueOf", String.class);
                            } catch (Exception e) {
                                String err = String.format("GlobalConfig[category:%s, name:%s] specifies type[%s] which doesn't have a static valueOf() method, ignore this type",
                                        g.getCategory(), g.getName(), g.getType());
                                logger.warn(err);
                            }
                        }

                        regularExpression = g.getValidatorRegularExpression();
                    }

                    @Override
                    public void validateGlobalConfig(String category, String name, String oldValue, String newValue) throws GlobalConfigException {
                        if (typeClassValueOfMethod != null) {
                            try {
                                typeClassValueOfMethod.invoke(typeClass, newValue);
                            } catch (Exception e) {
                                String err = String.format("GlobalConfig[category:%s, name:%s] is of type %s, the value[%s] cannot be converted to that type, %s",
                                        g.getCategory(), g.getName(), typeClass.getName(), newValue, e.getMessage());
                                throw new GlobalConfigException(err, e);
                            }

                            try {
                                typeClassValueOfMethod.invoke(typeClass, g.getDefaultValue());
                            } catch (Exception e) {
                                String err = String.format("GlobalConfig[category:%s, name:%s] is of type %s, the default value[%s] cannot be converted to that type, %s",
                                        g.getCategory(), g.getName(), typeClass.getName(), g.getDefaultValue(), e.getMessage());
                                throw new GlobalConfigException(err, e);
                            }
                        }

                        if (typeClass != null && (Boolean.class).isAssignableFrom(typeClass)) {
                            if (newValue == null ||
                                    (!newValue.equalsIgnoreCase("true") &&
                                            !newValue.equalsIgnoreCase("false"))
                                    ) {
                                String err = String.format("GlobalConfig[category:%s, name:%s]'s value[%s] is not a valid boolean string[true, false].",
                                        g.getCategory(), g.getName(), newValue);
                                throw new GlobalConfigException(err);
                            }

                            if (g.getDefaultValue() == null ||
                                    (!g.getDefaultValue().equalsIgnoreCase("true") &&
                                            !g.getDefaultValue().equalsIgnoreCase("false"))
                                    ) {
                                String err = String.format("GlobalConfig[category:%s, name:%s]'s default value[%s] is not a valid boolean string[true, false].",
                                        g.getCategory(), g.getName(), g.getDefaultValue());
                                throw new GlobalConfigException(err);

                            }
                        }

                        if (regularExpression != null) {
                            Pattern p = Pattern.compile(regularExpression);
                            if (newValue != null) {
                                Matcher mt = p.matcher(newValue);
                                if (!mt.matches()) {
                                    String err = String.format("GlobalConfig[category:%s, name:%s]'s value[%s] doesn't match validatorRegularExpression[%s]",
                                            g.getCategory(), g.getName(), newValue, regularExpression);
                                    throw new GlobalConfigException(err);
                                }
                            }
                            if (g.getDefaultValue() != null) {
                                Matcher mt = p.matcher(g.getDefaultValue());
                                if (!mt.matches()) {
                                    String err = String.format("GlobalConfig[category:%s, name:%s]'s default value[%s] doesn't match validatorRegularExpression[%s]",
                                            g.getCategory(), g.getName(), g.getDefaultValue(), regularExpression);
                                    throw new GlobalConfigException(err);
                                }
                            }
                        }
                    }
                });
            }

            private void createValidatorForBothXmlAndDatabase() throws ClassNotFoundException {
                for (GlobalConfig g : configsFromXml.values()) {
                    createValidator(g);
                }
                for (GlobalConfig g : configsFromDatabase.values()) {
                    createValidator(g);
                }
            }

            private void parseConfig(File file) throws JAXBException, FileNotFoundException {
                if (!file.getName().endsWith("xml")) {
                    logger.warn(String.format("file[%s] in global config folder is not end with .xml, skip it", file.getAbsolutePath()));
                    return;
                }

                Unmarshaller unmarshaller = context.createUnmarshaller();
                org.zstack.core.config.schema.GlobalConfig gb = (org.zstack.core.config.schema.GlobalConfig) unmarshaller.unmarshal(file);
                for (org.zstack.core.config.schema.GlobalConfig.Config c : gb.getConfig()) {
                    String category = c.getCategory();
                    category = category == null ? OTHER_CATEGORY : category;
                    c.setCategory(category);
                    if (c.getValue() == null) {
                        c.setValue(c.getDefaultValue());
                    }
                    if (c.getDefaultValue() == null) {
                        throw new IllegalArgumentException(String.format("GlobalConfig[category:%s, name:%s] must have a default value", c.getCategory(), c.getName()));
                    }
                    GlobalConfig config = GlobalConfig.valueOf(c);
                    if (configsFromXml.containsKey(config.getIdentity())) {
                        throw new IllegalArgumentException(String.format("duplicate GlobalConfig[category: %s, name: %s]", config.getCategory(), config.getName()));
                    }
                    configsFromXml.put(config.getIdentity(), config);
                }
            }

            private void link() {
                for (Field field : globalConfigFields) {
                    field.setAccessible(true);
                    try {
                        GlobalConfig config = (GlobalConfig) field.get(null);
                        if (config == null) {
                            throw new CloudRuntimeException(String.format("GlobalConfigDefinition[%s] defines a null GlobalConfig[%s]." +
                                    "You must assign a value to it using new GlobalConfig(category, name)",
                                    field.getDeclaringClass().getName(), field.getName()));
                        }

                        link(field, config);
                    } catch (IllegalAccessException e) {
                        throw new CloudRuntimeException(e);
                    }
                }

                for (GlobalConfig c : configsFromXml.values()) {
                    if (!c.isLinked()) {
                        logger.warn(String.format("GlobalConfig[category: %s, name: %s] is not linked to any definition", c.getCategory(), c.getName()));
                    }
                }
            }

            private void link(Field field, final GlobalConfig old) throws IllegalAccessException {
                GlobalConfig xmlConfig = configsFromXml.get(old.getIdentity());
                DebugUtils.Assert(xmlConfig != null, String.format("unable to find GlobalConfig[category:%s, name:%s] for linking to %s.%s",
                        old.getCategory(), old.getName(), field.getDeclaringClass().getName(), field.getName()));
                final GlobalConfig config = old.copy(xmlConfig);
                field.set(null, config);
                // all global config base on Field allConfigs which is origin from configsFromXml, so update its value
                configsFromXml.put(old.getIdentity(), config);

                final GlobalConfigValidation at = field.getAnnotation(GlobalConfigValidation.class);
                if (at != null) {
                    config.installValidateExtension(new GlobalConfigValidatorExtensionPoint() {
                        @Override
                        public void validateGlobalConfig(String category, String name, String oldValue, String value) throws GlobalConfigException {
                            if (at.notNull() && value == null) {
                                throw new GlobalConfigException(String.format("%s cannot be null", config.getCanonicalName()));
                            }
                        }
                    });

                    config.installValidateExtension(new GlobalConfigValidatorExtensionPoint() {
                        @Override
                        public void validateGlobalConfig(String category, String name, String oldValue, String newValue) throws GlobalConfigException {
                            if (at.notEmpty() && newValue.trim().equals("")) {
                                throw new GlobalConfigException(String.format("%s cannot be empty string", config.getCanonicalName()));
                            }
                        }
                    });

                    if (at.inNumberRange().length > 0 || at.numberGreaterThan() != Long.MIN_VALUE || at.numberLessThan() != Long.MAX_VALUE) {
                        if (config.getType() != null && TypeUtils.isTypeOf(config.getType(), Long.class, Integer.class)) {
                            throw new CloudRuntimeException(String.format("%s has @GlobalConfigValidation defined on field[%s.%s] which indicates its numeric type, but its type is neither Long nor Integer, it's %s",
                                    config.getCanonicalName(), field.getDeclaringClass(), field.getName(), config.getType()));
                        }
                        if (config.getType() == null) {
                            logger.warn(String.format("%s has @GlobalConfigValidation defined on field[%s.%s] which indicates it's numeric type, but its is null, assume it's Long type",
                                    config.getCanonicalName(), field.getDeclaringClass(), field.getName()));
                            config.setType(Long.class.getName());
                        }
                    }

                    if (at.numberLessThan() != Long.MAX_VALUE) {
                        config.installValidateExtension(new GlobalConfigValidatorExtensionPoint() {
                            @Override
                            public void validateGlobalConfig(String category, String name, String oldValue, String value) throws GlobalConfigException {
                                try {
                                    long num = Long.valueOf(value);
                                    if (num > at.numberLessThan()) {
                                        throw new GlobalConfigException(String.format("%s must be less than %s, but got %s",
                                                config.getCanonicalName(), at.numberLessThan(), num));
                                    }
                                } catch (NumberFormatException e) {
                                    throw new GlobalConfigException(String.format("%s is not a number or out of range of a Long type", value), e);
                                }
                            }
                        });
                    }

                    if (at.numberGreaterThan() != Long.MIN_VALUE) {
                        config.installValidateExtension(new GlobalConfigValidatorExtensionPoint() {
                            @Override
                            public void validateGlobalConfig(String category, String name, String oldValue, String value) throws GlobalConfigException {
                                try {
                                    long num = Long.valueOf(value);
                                    if (num < at.numberGreaterThan()) {
                                        throw new GlobalConfigException(String.format("%s must be greater than %s, but got %s",
                                                config.getCanonicalName(), at.numberGreaterThan(), num));
                                    }
                                } catch (NumberFormatException e) {
                                    throw new GlobalConfigException(String.format("%s is not a number or out of range of a Long type", value), e);
                                }
                            }
                        });
                    }

                    if (at.inNumberRange().length > 0) {
                        DebugUtils.Assert(at.inNumberRange().length == 2, String.format("@GlobalConfigValidation.inNumberRange defined on field[%s.%s] must have two elements, where the first one is lower bound and the second one is upper bound",
                                field.getDeclaringClass(), field.getName()));

                        config.installValidateExtension(new GlobalConfigValidatorExtensionPoint() {
                            @Override
                            public void validateGlobalConfig(String category, String name, String oldValue, String value) throws GlobalConfigException {
                                try {
                                    long num = Long.valueOf(value);
                                    long lowBound = at.inNumberRange()[0];
                                    long upBound = at.inNumberRange()[1];
                                    if (!(num >= lowBound && num <= upBound)) {
                                        throw new GlobalConfigException(String.format("%s must in range of [%s, %s]",
                                                config.getCanonicalName(), lowBound, upBound));
                                    }
                                } catch (NumberFormatException e) {
                                    throw new GlobalConfigException(String.format("%s is not a number or out of range of a Long type", value), e);
                                }
                            }
                        });
                    }

                    if (at.validValues().length > 0) {
                        final List<String> validValues = new ArrayList<String>();
                        Collections.addAll(validValues, at.validValues());
                        config.installValidateExtension(new GlobalConfigValidatorExtensionPoint() {
                            @Override
                            public void validateGlobalConfig(String category, String name, String oldValue, String newValue) throws GlobalConfigException {
                                if (!validValues.contains(newValue)) {
                                    throw new GlobalConfigException(String.format("%s is not a valid value. Valid values are %s", newValue, validValues));
                                }
                            }
                        });
                    }
                }

                config.setConfigDef(field.getAnnotation(GlobalConfigDef.class));
                config.setLinked(true);
                logger.debug(String.format("linked GlobalConfig[category:%s, name:%s, value:%s] to %s.%s",
                        config.getCategory(), config.getName(), config.getDefaultValue(), field.getDeclaringClass().getName(), field.getName()));
            }
        }

        GlobalConfigInitializer initializer = new GlobalConfigInitializer();
        initializer.init();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public Map<String, GlobalConfig> getAllConfig() {
        SimpleQuery<GlobalConfigVO> query = dbf.createQuery(GlobalConfigVO.class);
        List<GlobalConfigVO> vos = query.list();
        Map<String, GlobalConfig> ret = new HashMap<String, GlobalConfig>(vos.size());
        for (GlobalConfigVO vo : vos) {
            GlobalConfig c = GlobalConfig.valueOf(vo);
            ret.put(c.getIdentity(), c);
        }
        return ret;
    }

    @Override
    public <T> T getConfigValue(String category, String name, Class<T> clz) {
        GlobalConfig c = allConfigs.get(GlobalConfig.produceIdentity(category, name));
        DebugUtils.Assert(c!=null, String.format("cannot find GlobalConfig[category:%s, name:%s]", category, name));
        return c.value(clz);
    }

    @Override
    public GlobalConfig createGlobalConfig(GlobalConfigVO vo) {
        vo = dbf.persistAndRefresh(vo);
        GlobalConfig c = GlobalConfig.valueOf(vo);
        allConfigs.put(GlobalConfig.produceIdentity(vo.getCategory(), vo.getName()), c);
        return c;
    }

    @Override
    public String updateConfig(String category, String name, String value) {
        GlobalConfig c = allConfigs.get(GlobalConfig.produceIdentity(category,name));
        DebugUtils.Assert(c != null, String.format("cannot find GlobalConfig[category:%s, name:%s]", category, name));
        c.updateValue(value);
        return c.value();
    }
}
