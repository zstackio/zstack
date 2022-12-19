package org.zstack.resourceconfig;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.EventCallback;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.config.*;
import org.zstack.core.db.*;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.vo.ResourceVO;
import org.zstack.header.vo.ResourceVO_;
import org.zstack.utils.TypeUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.*;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;
import static org.zstack.utils.StringDSL.s;
import static org.zstack.resourceconfig.ResourceConfigCanonicalEvents.*;

/**
 * Created by MaJin on 2019/2/23.
 */

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class ResourceConfig {
    private static final CLogger logger = Utils.getLogger(ResourceConfig.class);

    @Autowired
    private DatabaseFacade dbf;

    @Autowired
    private EventFacade evtf;

    protected GlobalConfig globalConfig;
    private List<Class> resourceClasses;
    private Map<String, ResourceConfigGetter> configGetter = new HashMap<>();
    private List<ResourceConfigUpdateExtensionPoint> localUpdateExtensions = new ArrayList<>();
    private List<ResourceConfigUpdateExtensionPoint> updateExtensions = new ArrayList<>();
    private List<ResourceConfigDeleteExtensionPoint> localDeleteExtensions = new ArrayList<>();
    private List<ResourceConfigDeleteExtensionPoint> deleteExtensions = new ArrayList<>();
    private List<ResourceConfigValidatorExtensionPoint> validatorExtensions = new ArrayList<>();

    public static ResourceConfig valueOf(GlobalConfig globalConfig, BindResourceConfig bindInfo) {
        ResourceConfig result = new ResourceConfig();
        result.globalConfig = globalConfig;
        result.resourceClasses = Arrays.asList(bindInfo.value());
        return result;
    }

    public void installLocalUpdateExtension(ResourceConfigUpdateExtensionPoint ext) {
        localUpdateExtensions.add(ext);
    }

    public void installUpdateExtension(ResourceConfigUpdateExtensionPoint ext) {
        updateExtensions.add(ext);
    }

    public void installLocalDeleteExtension(ResourceConfigDeleteExtensionPoint ext) {
        localDeleteExtensions.add(ext);
    }

    public void installDeleteExtension(ResourceConfigDeleteExtensionPoint ext) {
        deleteExtensions.add(ext);
    }

    public void installValidatorExtension(ResourceConfigValidatorExtensionPoint ext) {
        validatorExtensions.add(ext);
    }

    public void validateOnly(String newValue) {
        String oldValue = globalConfig.value();
        globalConfig.getValidators().forEach(it ->
                it.validateGlobalConfig(globalConfig.getCategory(), globalConfig.getName(), oldValue, newValue));
    }

    public void updateValue(String resourceUuid, String newValue) {
        String resourceType = getResourceType(resourceUuid);
        updateValue(resourceUuid, resourceType, newValue, true);
    }

    public void deleteValue(String resourceUuid) {
        String resourceType = getResourceType(resourceUuid);
        deleteValue(resourceUuid, resourceType, true);
    }

    public <T> T defaultValue(Class<T> clz) {
        return globalConfig.value(clz);
    }

    public <T> T getResourceConfigValue(String resourceUuid, Class<T> clz) {
        String value = getResourceConfigValue(resourceUuid);
        return TypeUtils.stringToValue(value, clz);
    }

    void init() {
        installEventTrigger();
        initResourceConfigNodes();
    }

    private void installEventTrigger() {
        evtf.on(s(ResourceConfigCanonicalEvents.UPDATE_EVENT_PATH).formatByMap(map(
                e("category", globalConfig.getCategory()),
                e("name", globalConfig.getName())
        )), new EventCallback() {
            @Override
            public void run(Map tokens, Object data) {
                String nodeUuid = (String) tokens.get("nodeUuid");
                if (Platform.getManagementServerId().equals(nodeUuid)) {
                    return;
                }

                UpdateEvent evt = (UpdateEvent) data;
                String newValue = Q.New(ResourceConfigVO.class).select(ResourceConfigVO_.value)
                        .eq(ResourceConfigVO_.resourceUuid, evt.getResourceUuid())
                        .eq(ResourceConfigVO_.category, globalConfig.getCategory())
                        .eq(ResourceConfigVO_.name, globalConfig.getName())
                        .findValue();

                updateValue(evt.getResourceUuid(), evt.getResourceType(), newValue, false);
                logger.info(String.format("ResourceConfig [resourceUuid:%s, category:%s, name:%s] was updated in other" +
                                " management node[uuid:%s], in line with that change, updated ours. %s --> %s",
                        evt.getResourceUuid(), globalConfig.getCategory(), globalConfig.getName(), nodeUuid, evt.getOldValue(), newValue));
            }
        });

        evtf.on(s(ResourceConfigCanonicalEvents.DELETE_EVENT_PATH).formatByMap(map(
                e("category", globalConfig.getCategory()),
                e("name", globalConfig.getName())
        )), new EventCallback() {
            @Override
            public void run(Map tokens, Object data) {
                String nodeUuid = (String) tokens.get("nodeUuid");
                if (Platform.getManagementServerId().equals(nodeUuid)) {
                    return;
                }

                DeleteEvent evt = (DeleteEvent)data;
                deleteValue(evt.getResourceUuid(), evt.getResourceType(), false);
                logger.info(String.format("ResourceConfig[resourceUuid: %s category: %s, name: %s] was deleted from" +
                                " other management node[uuid:%s], in line with that change, deleted ours.",
                        evt.getResourceUuid(), globalConfig.getCategory(), globalConfig.getName(), nodeUuid));
            }
        });
    }

    private void initResourceConfigNodes() {
        for (int i = 0; i < resourceClasses.size(); i++) {
            Class clz = resourceClasses.get(i);
            ResourceConfigGetter getter = new ResourceConfigGetter();
            getter.init(resourceClasses.subList(i, resourceClasses.size()));
            configGetter.put(clz.getSimpleName(), getter);
        }
    }

    private void updateValue(String resourceUuid, String resourceType, String newValue, boolean localUpdate) {
        String originValue = loadConfigValue(resourceUuid);
        String oldValue = originValue == null ? globalConfig.value() : originValue;

        if (localUpdate) {
            globalConfig.getValidators().forEach(it ->
                    it.validateGlobalConfig(globalConfig.getCategory(), globalConfig.getName(), oldValue, newValue));
            validatorExtensions.forEach(it -> it.validateResourceConfig(resourceUuid, oldValue, newValue));
            updateValueInDb(resourceUuid, resourceType, newValue);
            localUpdateExtensions.forEach(it -> it.updateResourceConfig(this, resourceUuid, resourceType, oldValue, newValue));
        }

        updateExtensions.forEach(it -> it.updateResourceConfig(this, resourceUuid, resourceType, oldValue, newValue));

        if (localUpdate) {
            UpdateEvent evt = new UpdateEvent();
            evt.setResourceUuid(resourceUuid);
            evt.setOldValue(oldValue);
            evtf.fire(makeUpdateEventPath(), evt);
        }

        logger.debug(String.format("updated resource config[resourceUuid:%s, resourceType:%s, category:%s, name:%s]: %s to %s",
                resourceUuid, resourceType, globalConfig.getCategory(), globalConfig.getName(), oldValue, newValue));
    }

    private void deleteValue(String resourceUuid, String resourceType, boolean localDelete) {
        String originValue = loadConfigValue(resourceUuid);
        String oldValue = originValue == null ? globalConfig.value() : originValue;

        if (localDelete) {
            deleteInDb(resourceUuid);
            localDeleteExtensions.forEach(it -> it.deleteResourceConfig(this, resourceUuid, resourceType, originValue));
        }

        deleteExtensions.forEach(it -> it.deleteResourceConfig(this, resourceUuid, resourceType, originValue));

        if (localDelete) {
            DeleteEvent evt = new DeleteEvent();
            evt.setResourceUuid(resourceUuid);
            evt.setResourceType(resourceType);
            evt.setOldValue(oldValue);
            evtf.fire(makeDeleteEventPath(), evt);
        }

        logger.debug(String.format("deleted resource config[resourceUuid:%s, resourceType:%s, category:%s, name:%s]",
                resourceUuid, resourceType, globalConfig.getCategory(), globalConfig.getName()));
    }


    @Transactional(readOnly = true)
    protected String getResourceConfigValue(String resourceUuid) {
        String resourceType = Q.New(ResourceVO.class).select(ResourceVO_.resourceType).eq(ResourceVO_.uuid, resourceUuid).findValue();
        if (resourceType == null) {
            logger.warn(String.format("no resource[uuid:%s] found, cannot get it's resource config," +
                    " use global config instead", resourceUuid));
            return globalConfig.value();
        }

        ResourceConfigGetter getter = configGetter.get(resourceType);
        if (getter == null) {
            logger.warn(String.format("resource[uuid:%s, type:%s] is not bound to global config[category:%s, name:%s]," +
                    " use global config instead", resourceUuid, resourceType, globalConfig.getCategory(), globalConfig.getName()));
            return globalConfig.value();
        }

        return getter.getResourceConfigValue(resourceUuid);
    }

    List<ResourceConfigInventory> getEffectiveResourceConfigs(String resourceUuid) {
        String resourceType = Q.New(ResourceVO.class).select(ResourceVO_.resourceType).eq(ResourceVO_.uuid, resourceUuid).findValue();
        if (resourceType == null) {
            logger.warn(String.format("no resource[uuid:%s] found, cannot get it's resource config," +
                    " use global config instead", resourceUuid));
            return Collections.emptyList();
        }

        ResourceConfigGetter getter = configGetter.get(resourceType);
        if (getter == null) {
            logger.warn(String.format("resource[uuid:%s, type:%s] is not bound to global config[category:%s, name:%s]," +
                    " use global config instead", resourceUuid, resourceType, globalConfig.getCategory(), globalConfig.getName()));
            return Collections.emptyList();
        }

        return getter.getConnectedResourceConfigs(resourceUuid);
    }

    private class ResourceConfigGetter {
        String resourceType;
        List<String> parentTypeSql = new ArrayList<>();

        private String getResourceConfigValue(String resourceUuid) {
            String v = loadConfigValue(resourceUuid);
            if (v != null) {
                return v;
            }

            for (String sql : parentTypeSql) {
                String resUuid = SQL.New(String.format(sql, resourceUuid), String.class).find();
                if (resUuid == null) {
                    continue;
                }

                v = loadConfigValue(resUuid);
                if (v != null) {
                    return v;
                }
            }

            return globalConfig.value();
        }

        private List<ResourceConfigInventory> getConnectedResourceConfigs(String resourceUuid) {
            List<ResourceConfigInventory> results = new ArrayList<>();
            Optional.ofNullable(loadConfig(resourceUuid)).ifPresent(it ->
                    results.add(ResourceConfigInventory.valueOf(it)));
            for (String sql : parentTypeSql) {
                String resUuid = SQL.New(String.format(sql, resourceUuid), String.class).find();
                if (resUuid == null) {
                    continue;
                }

                Optional.ofNullable(loadConfig(resUuid)).ifPresent(it ->
                        results.add(ResourceConfigInventory.valueOf(it)));
            }
            return results;
        }

        private void init(List<Class> connectedClasses) {
            ResourceConfigGetter getter = new ResourceConfigGetter();
            Class resourceClass = connectedClasses.get(0);
            for (Class parentClass : connectedClasses.subList(1, connectedClasses.size())) {
                Optional.ofNullable(DBGraph.findVerticesWithSmallestWeight(resourceClass, parentClass)).ifPresent(vertex ->
                        parentTypeSql.add(vertex.toSQL("uuid", SimpleQuery.Op.EQ, "'%s'")));
            }

            getter.resourceType = resourceClass.getSimpleName();
        }
    }

    @Transactional
    protected void updateValueInDb(String resourceUuid, String resourceType, String newValue) {
        ResourceConfigVO vo = loadConfig(resourceUuid);
        if (vo != null) {
            vo.setValue(newValue);
            dbf.getEntityManager().merge(vo);
            return;
        }

        vo = new ResourceConfigVO();
        vo.setUuid(Platform.getUuid());
        vo.setCategory(globalConfig.getCategory());
        vo.setName(globalConfig.getName());
        vo.setValue(newValue);
        vo.setDescription(globalConfig.getDescription());
        vo.setResourceUuid(resourceUuid);
        vo.setResourceType(resourceType);
        dbf.getEntityManager().persist(vo);
    }

    protected void deleteInDb(String resourceUuid) {
        SQL.New(ResourceConfigVO.class).eq(ResourceConfigVO_.resourceUuid, resourceUuid)
                .eq(ResourceConfigVO_.name, globalConfig.getName())
                .eq(ResourceConfigVO_.category, globalConfig.getCategory())
                .delete();
    }

    private String getResourceType(String resourceUuid) {
        String resourceType = Q.New(ResourceVO.class).eq(ResourceVO_.uuid, resourceUuid).select(ResourceVO_.resourceType).findValue();
        if (resourceType == null) {
            throw new OperationFailureException(operr("cannot find resource[uuid: %s]", resourceUuid));
        }

        if (!configGetter.containsKey(resourceType)) {
            throw new OperationFailureException(operr("ResourceConfig [category:%s, name:%s]" +
                    " cannot bind to resourceType: %s", globalConfig.getCategory(), globalConfig.getName(), resourceType));
        }
        return resourceType;
    }

    private String makeUpdateEventPath() {
        return s(ResourceConfigCanonicalEvents.UPDATE_EVENT_PATH).formatByMap(map(
                e("nodeUuid", Platform.getManagementServerId()),
                e("category", globalConfig.getCategory()),
                e("name", globalConfig.getName())
        ));
    }

    private String makeDeleteEventPath() {
        return s(ResourceConfigCanonicalEvents.UPDATE_EVENT_PATH).formatByMap(map(
                e("nodeUuid", Platform.getManagementServerId()),
                e("category", globalConfig.getCategory()),
                e("name", globalConfig.getName())
        ));
    }

    ResourceConfigVO loadConfig(String resourceUuid) {
        return Q.New(ResourceConfigVO.class)
                .eq(ResourceConfigVO_.name, globalConfig.getName())
                .eq(ResourceConfigVO_.category, globalConfig.getCategory())
                .eq(ResourceConfigVO_.resourceUuid, resourceUuid)
                .find();
    }

    public boolean resourceConfigCreated(String resourceUuid) {
        return Q.New(ResourceConfigVO.class)
                .eq(ResourceConfigVO_.name, globalConfig.getName())
                .eq(ResourceConfigVO_.category, globalConfig.getCategory())
                .eq(ResourceConfigVO_.resourceUuid, resourceUuid)
                .isExists();
    }

    private String loadConfigValue(String resourceUuid) {
        return Q.New(ResourceConfigVO.class).select(ResourceConfigVO_.value)
                .eq(ResourceConfigVO_.name, globalConfig.getName())
                .eq(ResourceConfigVO_.category, globalConfig.getCategory())
                .eq(ResourceConfigVO_.resourceUuid, resourceUuid)
                .findValue();
    }


    List<Class> getResourceClasses() {
        return resourceClasses;
    }
}
