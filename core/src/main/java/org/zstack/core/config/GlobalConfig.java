package org.zstack.core.config;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.EventCallback;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.config.GlobalConfigCanonicalEvents.UpdateEvent;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.TypeUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;
import static org.zstack.utils.StringDSL.s;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class GlobalConfig {
    private static final CLogger logger = Utils.getLogger(GlobalConfig.class);

    private String name;
    private String category;
    private String description;
    private String type;
    private String validatorRegularExpression;
    private String defaultValue;
    private volatile String value;
    private boolean linked;
    private transient List<GlobalConfigUpdateExtensionPoint> updateExtensions = new ArrayList<>();
    private transient List<GlobalConfigValidatorExtensionPoint> validators = new ArrayList<>();
    private transient List<GlobalConfigUpdateExtensionPoint> localUpdateExtensions = new ArrayList<>();
    private GlobalConfigDef configDef;

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private EventFacade evtf;

    @Override
    public String toString() {
        return JSONObjectUtil.toJsonString(map(
                e("name", name),
                e("category", category),
                e("type", type),
                e("description", description),
                e("defaultValue", defaultValue),
                e("value", value),
                e("validatorRegularExpression", validatorRegularExpression)
        ));
    }

    public GlobalConfig(String category, String name) {
        this.category = category;
        this.name = name;
    }

    GlobalConfig() {
    }

    GlobalConfig copy(GlobalConfig g){
        setName(g.getName());
        setCategory(g.getCategory());
        setDescription(g.getDescription());
        setType(g.getType());
        setValidatorRegularExpression(g.getValidatorRegularExpression());
        setDefaultValue(g.getDefaultValue());
        setValue(g.value());
        setLinked(g.isLinked());

        validators = new ArrayList<>();
        updateExtensions = new ArrayList<>();
        localUpdateExtensions = new ArrayList<>();

        updateExtensions.addAll(g.getUpdateExtensions());
        localUpdateExtensions.addAll(g.getLocalUpdateExtensions());
        validators.addAll(g.getValidators());
        configDef = g.getConfigDef();
        return this;
    }

    private String makeUpdateEventPath() {
        return s(GlobalConfigCanonicalEvents.UPDATE_EVENT_PATH).formatByMap(map(
                e("nodeUuid", Platform.getManagementServerId()),
                e("category", category),
                e("name", name)
        ));
    }

    public GlobalConfigVO reload() {
        SimpleQuery<GlobalConfigVO> q = dbf.createQuery(GlobalConfigVO.class);
        q.add(GlobalConfigVO_.category, Op.EQ, category);
        q.add(GlobalConfigVO_.name, Op.EQ, name);
        return q.find();
    }

    public void installLocalUpdateExtension(GlobalConfigUpdateExtensionPoint ext) {
        localUpdateExtensions.add(ext);
    }

    public void installUpdateExtension(GlobalConfigUpdateExtensionPoint ext) {
        updateExtensions.add(ext);
    }

    public void installValidateExtension(GlobalConfigValidatorExtensionPoint ext) {
        validators.add(ext);
    }

    public String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    void setType(String type) {
        this.type = type;
    }

    public String getValidatorRegularExpression() {
        return validatorRegularExpression;
    }

    void setValidatorRegularExpression(String validatorRegularExpression) {
        this.validatorRegularExpression = validatorRegularExpression;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String value() {
        return value;
    }

    void setValue(String value) {
        this.value = value;
    }

    public <T> T value(Class<T> clz) {
        return TypeUtils.stringToValue(value, clz);
    }

    public <T> T defaultValue(Class<T> clz) {
        return TypeUtils.stringToValue(defaultValue, clz);
    }

    public static GlobalConfig valueOf(GlobalConfigVO vo) {
        GlobalConfig conf = new GlobalConfig();
        conf.setName(vo.getName());
        conf.setCategory(vo.getCategory());
        conf.setDefaultValue(vo.getDefaultValue());
        conf.setDescription(vo.getDescription());
        conf.setValue(vo.getValue());
        return conf;
    }

    public static GlobalConfig valueOf(GlobalConfig old) {
        GlobalConfig ng = new GlobalConfig();
        ng.setName(old.getName());
        ng.setValue(old.value());
        ng.setCategory(old.getCategory());
        ng.setDescription(old.getDescription());
        ng.setDefaultValue(ng.getDefaultValue());
        ng.setValidatorRegularExpression(old.getValidatorRegularExpression());
        return ng;
    }

    public GlobalConfigVO toVO() {
        GlobalConfigVO vo = new GlobalConfigVO();
        vo.setCategory(category);
        vo.setValue(value);
        vo.setDescription(description);
        vo.setDefaultValue(defaultValue);
        vo.setName(name);
        return vo;
    }

    public static GlobalConfig valueOf(org.zstack.core.config.schema.GlobalConfig.Config c) {
        GlobalConfig conf = new GlobalConfig();
        conf.setName(c.getName());
        conf.setCategory(c.getCategory());
        conf.setDefaultValue(c.getDefaultValue());
        conf.setDescription(c.getDescription());
        conf.setValue(c.getValue());
        conf.setType(c.getType());
        return conf;
    }

    public String getIdentity() {
        return produceIdentity(category, name);
    }

    public static String produceIdentity(String category, String name) {
        return String.format("%s.%s", category, name);
    }

    void validate() {
        validate(value);
    }

    private void validate(String newValue) {
        for  (GlobalConfigValidatorExtensionPoint ext : validators) {
            ext.validateGlobalConfig(category, name, value, newValue);
        }
    }

    void init() {
        evtf.on(s(GlobalConfigCanonicalEvents.UPDATE_EVENT_PATH).formatByMap(map(
                e("category", category),
                e("name", name)
        )), new EventCallback() {
            @Override
            public void run(Map tokens, Object data) {
                String nodeUuid = (String) tokens.get("nodeUuid");
                if (Platform.getManagementServerId().equals(nodeUuid)) {
                    return;
                }

                UpdateEvent evt = (UpdateEvent)data;
                update(evt.getNewValue(), false);

                logger.info(String.format("GlobalConfig[category: %s, name: %s] was updated in other management node[uuid:%s]," +
                        "in line with that change, updated ours. %s --> %s", category, name, nodeUuid, evt.getOldValue(), value));
            }
        });
    }

    private void update(String newValue, boolean localUpdate) {
        validate(newValue);

        SimpleQuery<GlobalConfigVO> q = dbf.createQuery(GlobalConfigVO.class);
        q.add(GlobalConfigVO_.category, Op.EQ, category);
        q.add(GlobalConfigVO_.name, Op.EQ, name);
        GlobalConfigVO vo = q.find();
        final GlobalConfig origin = valueOf(vo);

        value = newValue;

        if (localUpdate) {
            vo.setValue(newValue);
            dbf.update(vo);

            final GlobalConfig self = this;
            CollectionUtils.safeForEach(localUpdateExtensions, new ForEachFunction<GlobalConfigUpdateExtensionPoint>() {
                @Override
                public void run(GlobalConfigUpdateExtensionPoint ext) {
                    ext.updateGlobalConfig(origin, self);
                }
            });
        }

        for (GlobalConfigUpdateExtensionPoint ext : updateExtensions) {
            try {
                ext.updateGlobalConfig(origin, this);
            } catch (Throwable t) {
                logger.warn(String.format("unhandled exception when calling %s", ext.getClass()));
            }
        }

        if (localUpdate) {
            UpdateEvent evt = new UpdateEvent();
            evt.setOldValue(origin.value());
            evt.setNewValue(newValue);
            evtf.fire(makeUpdateEventPath(), evt);
        }

        logger.debug(String.format("updated global config[category:%s, name:%s]: %s to %s", category, name, origin.value(), value));
    }

    public void updateValue(Object val) {
        if (TypeUtils.nullSafeEquals(value, val)) {
            return;
        }

        String newValue = val == null ? null : val.toString();
        update(newValue, true);
    }

    boolean isLinked() {
        return linked;
    }

    void setLinked(boolean linked) {
        this.linked = linked;
    }

    public boolean isMe(GlobalConfig other) {
        return category.equals(other.getCategory()) && name.equals(other.getName());
    }

    public GlobalConfigDef getConfigDef() {
        return configDef;
    }

    public void setConfigDef(GlobalConfigDef configDef) {
        this.configDef = configDef;
    }

    public String getCanonicalName() {
        return String.format("Global config[category: %s, name: %s]", category, name);
    }

    void setValidators(List<GlobalConfigValidatorExtensionPoint> validators) {
        this.validators = validators;
    }

    void setUpdateExtensions(List<GlobalConfigUpdateExtensionPoint> updateExtensions) {
        this.updateExtensions = updateExtensions;
    }

    void setLocalUpdateExtensions(List<GlobalConfigUpdateExtensionPoint> localUpdateExtensions) {
        this.localUpdateExtensions = localUpdateExtensions;
    }

    public List<GlobalConfigValidatorExtensionPoint> getValidators() {
        return validators;
    }

    public List<GlobalConfigUpdateExtensionPoint> getUpdateExtensions() {
        return updateExtensions;
    }

    public List<GlobalConfigUpdateExtensionPoint> getLocalUpdateExtensions() {
        return localUpdateExtensions;
    }
}
