package org.zstack.resourceconfig;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
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

import javax.persistence.Tuple;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;
import static org.zstack.utils.StringDSL.s;
import static org.zstack.resourceconfig.ResourceConfigCanonicalEvents.*;
import static org.zstack.utils.clouderrorcode.CloudOperationsErrorCode.*;

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

    @Autowired
    private PlatformTransactionManager transactionManager;

    protected GlobalConfig globalConfig;
    private List<Class> resourceClasses;
    private Map<String, ResourceConfigGetter> configGetter = new HashMap<>();
    private List<ResourceConfigUpdateExtensionPoint> localUpdateExtensions = new ArrayList<>();
    private List<ResourceConfigUpdateExtensionPoint> updateExtensions = new ArrayList<>();
    private List<ResourceConfigDeleteExtensionPoint> localDeleteExtensions = new ArrayList<>();
    private List<ResourceConfigDeleteExtensionPoint> deleteExtensions = new ArrayList<>();
    private List<ResourceConfigDeleteValidatorExtensionPoint> deleteValidatorExtensions = new ArrayList<>();
    private List<ResourceConfigValidatorExtensionPoint> validatorExtensions = new ArrayList<>();
    private List<ResourceConfigTransactionalMutationExtensionPoint> transactionalMutationExtensions = new ArrayList<>();
    private List<ConfigTransactionalMutationExtensionPoint> configMutationExtensions = new ArrayList<>();

    public static ResourceConfig valueOf(GlobalConfig globalConfig, BindResourceConfig bindInfo) {
        ResourceConfig result = new ResourceConfig();
        result.globalConfig = globalConfig;
        result.resourceClasses = Arrays.asList(bindInfo.value());
        return result;
    }

    void wire(DatabaseFacade dbf, EventFacade evtf) {
        this.dbf = dbf;
        this.evtf = evtf;
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

    public void installDeleteValidatorExtension(ResourceConfigDeleteValidatorExtensionPoint ext) {
        deleteValidatorExtensions.add(ext);
    }

    public void installValidatorExtension(ResourceConfigValidatorExtensionPoint ext) {
        validatorExtensions.add(ext);
    }

    public void installTransactionalMutationExtension(ResourceConfigTransactionalMutationExtensionPoint ext) {
        transactionalMutationExtensions.add(ext);
    }

    public boolean requiresAtomicBulkTransaction() {
        return hasConfigMutationExtensions() || transactionalMutationExtensions.stream()
                .anyMatch(ResourceConfigTransactionalMutationExtensionPoint::requiresAtomicBulkTransaction);
    }

    public void installConfigMutationExtension(ConfigTransactionalMutationExtensionPoint extension) {
        if (!configMutationExtensions.contains(extension)) { configMutationExtensions.add(extension); }
    }

    public boolean hasConfigMutationExtensions() { return !configMutationExtensions.isEmpty(); }

    public void validateOnly(String newValue) {
        String oldValue = globalConfig.value();
        globalConfig.getValidators().forEach(it ->
                it.validateGlobalConfig(globalConfig.getCategory(), globalConfig.getName(), oldValue, newValue));
    }

    public void validateNewValue(String resourceUuid, String newValue) {
        String originValue = loadConfigValue(resourceUuid);
        String oldValue = originValue == null ? globalConfig.value() : originValue;

        globalConfig.getValidators().forEach(it ->
                it.validateGlobalConfig(globalConfig.getCategory(), globalConfig.getName(), oldValue, newValue));
        validatorExtensions.forEach(it -> it.validateResourceConfig(resourceUuid, oldValue, newValue));
    }

    @Transactional
    public void updateValue(String resourceUuid, String newValue) {
        updateValue(resourceUuid, newValue, ConfigMutationContext.internal());
    }

    @Transactional
    public void updateValue(String resourceUuid, String newValue, ConfigMutationContext context) {
        if (!hasConfigMutationExtensions()) {
            updateValue(resourceUuid, getResourceType(resourceUuid), newValue, true, requiresAtomicBulkTransaction());
            return;
        }
        applyMutations(Collections.singletonList(this), Collections.singletonList(
                mutation(resourceUuid, newValue, false)), context);
    }

    @Transactional
    public void deleteValue(String resourceUuid) {
        deleteValue(resourceUuid, ConfigMutationContext.internal());
    }

    @Transactional
    public void deleteValue(String resourceUuid, ConfigMutationContext context) {
        if (!hasConfigMutationExtensions()) {
            deleteValue(resourceUuid, getResourceType(resourceUuid), true, requiresAtomicBulkTransaction());
            return;
        }
        applyMutations(Collections.singletonList(this), Collections.singletonList(
                mutation(resourceUuid, null, true)), context);
    }

    ConfigMutation mutation(String resourceUuid, String newValue, boolean delete) {
        return new ConfigMutation(globalConfig.getCategory(), globalConfig.getName(), resourceUuid,
                getResourceType(resourceUuid), newValue, delete);
    }

    /** Caller owns the actual transaction. Shared participants see the complete batch once. */
    static void applyMutations(List<ResourceConfig> configs, List<ConfigMutation> changes,
            ConfigMutationContext context) {
        if (configs.isEmpty() || configs.size() != changes.size()) {
            throw new GlobalConfigException("Configuration mutation batch is empty or inconsistent");
        }
        Set<String> identities = new HashSet<>();
        Set<ConfigTransactionalMutationExtensionPoint> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        List<ConfigTransactionalMutationExtensionPoint> participants = new ArrayList<>();
        List<ConfigMutation> immutableChanges = Collections.unmodifiableList(new ArrayList<>(changes));
        for (int i = 0; i < configs.size(); i++) {
            ResourceConfig config = configs.get(i);
            ConfigMutation change = changes.get(i);
            String identity = change.getResourceUuid() + ":" + GlobalConfig.produceIdentity(change.getCategory(), change.getName());
            if (!identities.add(identity)) { throw new GlobalConfigException("Duplicate configuration in batch: " + identity); }
            if (!change.isDelete()) { config.validateNewValue(change.getResourceUuid(), change.getNewValue()); }
            else {
                String oldValue = config.loadConfigValue(change.getResourceUuid());
                String effectiveOld = oldValue == null ? config.globalConfig.value() : oldValue;
                config.deleteValidatorExtensions.forEach(it -> it.validateDelete(change.getResourceUuid(), effectiveOld));
            }
            for (ConfigTransactionalMutationExtensionPoint extension : config.configMutationExtensions) {
                if (seen.add(extension)) { participants.add(extension); }
            }
        }
        javax.persistence.EntityManager em = configs.get(0).dbf.getEntityManager();
        for (ConfigTransactionalMutationExtensionPoint extension : participants) {
            extension.beforeMutations(em, immutableChanges, context);
        }
        for (int i = 0; i < configs.size(); i++) {
            ConfigMutation change = changes.get(i);
            if (change.isDelete()) {
                configs.get(i).deleteValue(change.getResourceUuid(), change.getResourceType(), true, true);
            } else {
                configs.get(i).updateValue(change.getResourceUuid(), change.getResourceType(), change.getNewValue(), true, true);
            }
        }
        for (ConfigTransactionalMutationExtensionPoint extension : participants) {
            extension.afterMutations(em, immutableChanges, context);
        }
    }

    public <T> T defaultValue(Class<T> clz) {
        return globalConfig.value(clz);
    }

    public <T> T getResourceConfigValue(String resourceUuid, Class<T> clz) {
        String value = getResourceConfigValue(resourceUuid);
        return TypeUtils.stringToValue(value, clz);
    }

    public <T> Map<String, T> getResourceConfigValues(List<String> resourceUuids, Class<T> clz) {
        Map<String, T> values = new HashMap<>();
        getResourceConfigValues(resourceUuids).forEach((key, value) -> values.put(key, TypeUtils.stringToValue(value, clz)));
        return values;
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

                updateValue(evt.getResourceUuid(), evt.getResourceType(), newValue, false, false);
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

                DeleteEvent evt = (DeleteEvent) data;
                deleteValue(evt.getResourceUuid(), evt.getResourceType(), false, false);
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

    private void updateValue(String resourceUuid, String resourceType, String newValue,
            boolean localUpdate, boolean deferCallbacks) {
        String originValue = loadConfigValue(resourceUuid);
        String oldValue = originValue == null ? globalConfig.value() : originValue;

        if (localUpdate) {
            globalConfig.getValidators().forEach(it ->
                    it.validateGlobalConfig(globalConfig.getCategory(), globalConfig.getName(), oldValue, newValue));
            validatorExtensions.forEach(it -> it.validateResourceConfig(resourceUuid, oldValue, newValue));
            transactionalMutationExtensions.forEach(it ->
                    it.beforeUpdate(dbf.getEntityManager(), this, resourceUuid, resourceType, oldValue, newValue));
            updateValueInDb(resourceUuid, resourceType, newValue);
        }
        if (deferCallbacks) {
            afterCommitOrNow(() -> {
                if (localUpdate) {
                    for (ResourceConfigUpdateExtensionPoint ext : localUpdateExtensions) {
                        runPostCommitExtensionSafely(() -> ext.updateResourceConfig(this, resourceUuid, resourceType, oldValue, newValue));
                    }
                }
                for (ResourceConfigUpdateExtensionPoint ext : updateExtensions) {
                    runPostCommitExtensionSafely(() -> ext.updateResourceConfig(this, resourceUuid, resourceType, oldValue, newValue));
                }
                if (localUpdate) {
                    UpdateEvent evt = new UpdateEvent();
                    evt.setResourceUuid(resourceUuid); evt.setResourceType(resourceType); evt.setOldValue(oldValue);
                    runCallbackSafely(() -> evtf.fire(makeUpdateEventPath(), evt));
                }
            });
        } else {
            if (localUpdate) {
                for (ResourceConfigUpdateExtensionPoint ext : localUpdateExtensions) {
                    ext.updateResourceConfig(this, resourceUuid, resourceType, oldValue, newValue);
                }
            }
            for (ResourceConfigUpdateExtensionPoint ext : updateExtensions) {
                ext.updateResourceConfig(this, resourceUuid, resourceType, oldValue, newValue);
            }
            if (localUpdate) {
                UpdateEvent evt = new UpdateEvent();
                evt.setResourceUuid(resourceUuid); evt.setResourceType(resourceType); evt.setOldValue(oldValue);
                evtf.fire(makeUpdateEventPath(), evt);
            }
        }

        logger.debug(String.format("updated resource config[resourceUuid:%s, resourceType:%s, category:%s, name:%s]: %s to %s",
                resourceUuid, resourceType, globalConfig.getCategory(), globalConfig.getName(), oldValue, newValue));
    }

    private void deleteValue(String resourceUuid, String resourceType,
            boolean localDelete, boolean deferCallbacks) {
        String originValue = loadConfigValue(resourceUuid);
        String oldValue = originValue == null ? globalConfig.value() : originValue;

        if (localDelete) {
            deleteValidatorExtensions.forEach(it -> it.validateDelete(resourceUuid, oldValue));
            transactionalMutationExtensions.forEach(it ->
                    it.beforeDelete(dbf.getEntityManager(), this, resourceUuid, resourceType, oldValue));
            deleteInDb(resourceUuid);
        }
        if (deferCallbacks) {
            afterCommitOrNow(() -> {
                if (localDelete) {
                    for (ResourceConfigDeleteExtensionPoint ext : localDeleteExtensions) {
                        runPostCommitExtensionSafely(() -> ext.deleteResourceConfig(this, resourceUuid, resourceType, originValue));
                    }
                }
                for (ResourceConfigDeleteExtensionPoint ext : deleteExtensions) {
                    runPostCommitExtensionSafely(() -> ext.deleteResourceConfig(this, resourceUuid, resourceType, originValue));
                }
                if (localDelete) {
                    DeleteEvent evt = new DeleteEvent();
                    evt.setResourceUuid(resourceUuid); evt.setResourceType(resourceType); evt.setOldValue(oldValue);
                    runCallbackSafely(() -> evtf.fire(makeDeleteEventPath(), evt));
                }
            });
        } else {
            if (localDelete) {
                for (ResourceConfigDeleteExtensionPoint ext : localDeleteExtensions) {
                    ext.deleteResourceConfig(this, resourceUuid, resourceType, originValue);
                }
            }
            for (ResourceConfigDeleteExtensionPoint ext : deleteExtensions) {
                ext.deleteResourceConfig(this, resourceUuid, resourceType, originValue);
            }
            if (localDelete) {
                DeleteEvent evt = new DeleteEvent();
                evt.setResourceUuid(resourceUuid); evt.setResourceType(resourceType); evt.setOldValue(oldValue);
                evtf.fire(makeDeleteEventPath(), evt);
            }
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

    @Transactional(readOnly = true)
    protected Map<String, String> getResourceConfigValues(List<String> resourceUuids) {
        Map<String, String> valuesByResourceUuids = new HashMap<>();

        if (resourceUuids.isEmpty()) {
            return valuesByResourceUuids;
        }

        List<Tuple> resourceTypeUuidPairs = Q.New(ResourceVO.class).select(ResourceVO_.resourceType, ResourceVO_.uuid)
                .in(ResourceVO_.uuid, resourceUuids).listTuple();

        if (resourceTypeUuidPairs.isEmpty()) {
            logger.warn("no resource found, cannot get it's resource config, use global config instead");

            resourceUuids.forEach(it -> valuesByResourceUuids.put(it, globalConfig.value()));
            return valuesByResourceUuids;
        }

        resourceUuids.removeAll(resourceTypeUuidPairs.stream().map(it -> it.get(1, String.class)).collect(Collectors.toSet()));
        resourceUuids.forEach(it -> valuesByResourceUuids.put(it, globalConfig.value()));

        Map<String, List<String>> typeByResourceUuids = groupResourceUuidsByType(resourceTypeUuidPairs);

        if (typeByResourceUuids.keySet().size() >= 2) {
            throw new OperationFailureException(
                    operr(ORG_ZSTACK_RESOURCECONFIG_10000, "resources has inconsistent resourceTypes. Details: %s", typeByResourceUuids.toString()));
        }

        typeByResourceUuids.forEach((resourceType, resUuids) -> {
            ResourceConfigGetter getter = configGetter.get(resourceType);
            if (getter == null) {
                logger.warn(String.format("resource[type:%s] is not bound to global config[category:%s, name:%s]," +
                        " use global config instead", resourceType, globalConfig.getCategory(), globalConfig.getName()));

                resUuids.forEach(uuid -> valuesByResourceUuids.put(uuid, globalConfig.value()));
                return;
            }
            valuesByResourceUuids.putAll(getter.getResourceConfigValues(resUuids));
        });

        return valuesByResourceUuids;
    }

    private Map<String, List<String>> groupResourceUuidsByType(List<Tuple> resourceTypeUuidPairs) {
        return resourceTypeUuidPairs.stream().collect(Collectors.groupingBy(pair -> pair.get(0, String.class),
                Collectors.mapping(pair -> pair.get(1, String.class), Collectors.toList())));
    }

    public List<ResourceConfigInventory> getEffectiveResourceConfigs(String resourceUuid) {
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
        List<String> parentResourceUuidPairsSql = new ArrayList<>();

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

        private Map<String, String> getResourceConfigValues(List<String> resourceUuids) {
            Map<String, String> valuesByResourceUuids = new HashMap<>();

            loadResourceUuidsAndValues(resourceUuids).forEach(it -> valuesByResourceUuids.put(it.get(0, String.class), it.get(1, String.class)));
            resourceUuids.removeAll(valuesByResourceUuids.keySet());

            for (String sql : parentResourceUuidPairsSql) {
                List<Tuple> parentResourceUuidPairs =
                        SQL.New(String.format(sql, "'" + String.join("','", resourceUuids) + "'"), Tuple.class).list();
                if (parentResourceUuidPairs.isEmpty()) {
                    continue;
                }

                Map<String, List<String>> parentUuidAndResourceUuids = parentResourceUuidPairs.stream()
                        .collect(Collectors.groupingBy(tuple -> tuple.get(0, String.class),
                                Collectors.mapping(tuple -> tuple.get(1, String.class), Collectors.toList())));

                loadResourceUuidsAndValues(new ArrayList<>(parentUuidAndResourceUuids.keySet())).forEach(tuple -> {
                    if (parentUuidAndResourceUuids.containsKey(tuple.get(0, String.class))) {
                        List<String> uuids = parentUuidAndResourceUuids.get(tuple.get(0, String.class));
                        uuids.forEach(resourceUuid -> valuesByResourceUuids.put(resourceUuid, tuple.get(1, String.class)));
                        resourceUuids.removeAll(uuids);
                    }
                });
            }

            resourceUuids.forEach(it -> valuesByResourceUuids.put(it, globalConfig.value()));
            return valuesByResourceUuids;
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

            for (Class parentClass : connectedClasses.subList(1, connectedClasses.size())) {
                Optional.ofNullable(DBGraph.findVerticesWithSmallestWeight(resourceClass, parentClass)).ifPresent(vertex ->
                        parentResourceUuidPairsSql.add(vertex.toBidirectionalSQL("uuid", SimpleQuery.Op.IN, "(%s)")));
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
            throw new OperationFailureException(operr(ORG_ZSTACK_RESOURCECONFIG_10001, "cannot find resource[uuid: %s]", resourceUuid));
        }

        if (!configGetter.containsKey(resourceType)) {
            throw new OperationFailureException(operr(ORG_ZSTACK_RESOURCECONFIG_10002, "ResourceConfig [category:%s, name:%s]" +
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
        return s(ResourceConfigCanonicalEvents.DELETE_EVENT_PATH).formatByMap(map(
                e("nodeUuid", Platform.getManagementServerId()),
                e("category", globalConfig.getCategory()),
                e("name", globalConfig.getName())
        ));
    }

    private void afterCommitOrNow(Runnable callback) {
        if (!org.springframework.transaction.support.TransactionSynchronizationManager.isSynchronizationActive()) {
            runCallbackSafely(callback);
            return;
        }
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                new org.springframework.transaction.support.TransactionSynchronizationAdapter() {
                    @Override public void afterCommit() { runCallbackSafely(callback); }
                });
    }

    private void runCallbackSafely(Runnable callback) {
        try { callback.run(); }
        catch (Throwable t) {
            logger.warn(String.format("post-commit callback failed for ResourceConfig[category:%s, name:%s]",
                    globalConfig.getCategory(), globalConfig.getName()), t);
        }
    }

    private void runPostCommitExtensionSafely(Runnable callback) {
        runCallbackSafely(() -> org.zstack.core.db.AfterCommitTransactionExecutor.run(transactionManager, callback));
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

    private List<Tuple> loadResourceUuidsAndValues(List<String> resourceUuids) {
        return Q.New(ResourceConfigVO.class)
                .select(ResourceConfigVO_.resourceUuid, ResourceConfigVO_.value)
                .eq(ResourceConfigVO_.name, globalConfig.getName())
                .eq(ResourceConfigVO_.category, globalConfig.getCategory())
                .in(ResourceConfigVO_.resourceUuid, resourceUuids).listTuple();
    }

    List<Class> getResourceClasses() {
        return resourceClasses;
    }
}
