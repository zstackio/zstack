package org.zstack.tag;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.*;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.defer.Defer;
import org.zstack.core.defer.Deferred;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.log.LogSafeGson;
import org.zstack.header.AbstractService;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.AccountConstant;
import org.zstack.header.identity.SessionInventory;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.tag.*;
import org.zstack.query.QueryFacade;
import org.zstack.utils.*;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.metamodel.EntityType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.*;
import static org.zstack.utils.CollectionDSL.list;
import static org.zstack.utils.CollectionUtils.removeDuplicateFromList;

public class TagManagerImpl extends AbstractService implements TagManager,
        SoftDeleteEntityExtensionPoint, GlobalApiMessageInterceptor, SystemTagLifeCycleExtension,
        HardDeleteEntityExtensionPoint {
    private static final CLogger logger = Utils.getLogger(TagManagerImpl.class);

    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected CloudBus bus;
    @Autowired
    private QueryFacade qf;
    @Autowired
    protected ErrorFacade errf;
    @Autowired
    private PluginRegistry pluginRgty;

    private List<SystemTag> systemTags = new ArrayList<>();
    private List<SystemTag> adminOnlySystemTags = new ArrayList<>();
    private List<PatternedSystemTag> sensitiveTags = new ArrayList<>();
    private Map<String, List<SystemTag>> resourceTypeSystemTagMap = new HashMap<>();
    private Map<String, Class> resourceTypeClassMap = new HashMap<>();
    private Map<Class, Class> resourceTypeCreateMessageMap = new HashMap<>();
    private Map<String, List<SystemTagCreateMessageValidator>> createMessageValidators = new HashMap<>();
    private Map<String, List<SystemTagResourceDeletionOperator>> resourceDeletionOperators = new HashMap<>();
    private Map<String, List<SystemTagLifeCycleExtension>> lifeCycleExtensions = new HashMap<>();
    private List<CreateTagFromMsgExtensionPoint> createTagExtensions = new ArrayList<>();
    private List<Class> autoDeleteTagClasses;


    private void initSystemTags() throws IllegalAccessException {
        Set<Class<?>> classes = BeanUtils.reflections.getTypesAnnotatedWith(TagDefinition.class);
        for (Class clz : classes) {
            List<Field> fields = FieldUtils.getAllFields(clz);
            for (Field f : fields) {
                if (!Modifier.isStatic(f.getModifiers())) {
                    continue;
                }

                if (!SystemTag.class.isAssignableFrom(f.getType())) {
                    continue;
                }

                f.setAccessible(true);
                SystemTag stag = (SystemTag) f.get(null);
                if (stag == null) {
                    throw new CloudRuntimeException(String.format("%s.%s defines a null system tag",
                            f.getDeclaringClass(), f.getName()));
                }

                if (PatternedSystemTag.class.isAssignableFrom(f.getType())) {
                    PatternedSystemTag ptag = new PatternedSystemTag(stag.getTagFormat(), stag.getResourceClass());
                    ptag.setValidators(stag.getValidators());
                    f.set(null, ptag);
                    systemTags.add(ptag);
                    stag = ptag;
                } else if (EphemeralSystemTag.class.isAssignableFrom(f.getType())) {
                    // pass
                    // ephemeral tag is not needed to inject and validate
                    systemTags.add(stag);
                } else {
                    SystemTag sstag = new SystemTag(stag.getTagFormat(), stag.getResourceClass());
                    sstag.setValidators(stag.getValidators());
                    f.set(null, sstag);
                    systemTags.add(sstag);
                    stag = sstag;
                }

                if (f.isAnnotationPresent(AdminOnlyTag.class)) {
                    adminOnlySystemTags.add(stag);
                }

                if (f.isAnnotationPresent(SensitiveTag.class) && stag instanceof PatternedSystemTag) {
                    sensitiveTags.add((PatternedSystemTag) stag);
                    ((PatternedSystemTag) stag).sensitiveTokens = Arrays.asList(f.getAnnotation(SensitiveTag.class).tokens());
                }

                stag.setTagMgr(this);
                List<SystemTag> lst = resourceTypeSystemTagMap.get(stag.getResourceClass().getSimpleName());
                if (lst == null) {
                    lst = new ArrayList<>();
                    resourceTypeSystemTagMap.put(stag.getResourceClass().getSimpleName(), lst);
                }
                lst.add(stag);
            }
        }
    }

    void init() {
        for (EntityType<?> entity : dbf.getEntityManager().getMetamodel().getEntities()) {
            Class type = entity.getJavaType();
            String name = type.getSimpleName();
            resourceTypeClassMap.put(name, type);
            if (logger.isTraceEnabled()) {
                logger.trace(String.format("discovered tag resource type[%s], class[%s]", name, type));
            }
        }

        try {
            // this makes sure DatabaseFacade is injected into every SystemTag object
            initSystemTags();
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }

        registrySensitiveTagHider();

        Set<Class<?>> createMessageClass = BeanUtils.reflections.getTypesAnnotatedWith(TagResourceType.class)
                .stream().filter(i->i.isAnnotationPresent(TagResourceType.class)).collect(Collectors.toSet());
        for (Class cmsgClz : createMessageClass) {
            TagResourceType at = (TagResourceType) cmsgClz.getAnnotation(TagResourceType.class);
            Class resType = at.value();
            if (!resourceTypeClassMap.values().contains(resType)) {
                throw new CloudRuntimeException(String.format(
                        "tag resource type[%s] defined in @TagResourceType of class[%s] is not a VO entity",
                        resType.getName(), cmsgClz.getName()));
            }
            resourceTypeCreateMessageMap.put(cmsgClz, resType);
        }

        autoDeleteTagClasses = new ArrayList<>(BeanUtils.reflections.getTypesAnnotatedWith(AutoDeleteTag.class));
        List<String> clzNames = CollectionUtils.transformToList(autoDeleteTagClasses, new Function<String, Class>() {
            @Override
            public String call(Class arg) {
                return arg.getSimpleName();
            }
        });

        logger.debug(String.format("tags of following resources are auto-deleting enabled: %s", clzNames));
    }

    private void registrySensitiveTagHider() {
        LogSafeGson.registryTagHider(this::hideSensitiveInfoInTag);
    }

    private String hideSensitiveInfoInTag(String tag) {
        for (PatternedSystemTag sensitiveTag : sensitiveTags) {
            if (sensitiveTag.isMatch(tag)) {
                return sensitiveTag.hideSensitiveInfo(tag);
            }
        }

        return tag;
    }

    private void populateExtensions() {
        for (SystemTagLifeCycleExtension ext : pluginRgty.getExtensionList(SystemTagLifeCycleExtension.class)) {
            for (String resType : ext.getResourceTypeOfSystemTags()) {
                if (!resourceTypeClassMap.containsKey(resType)) {
                    throw new CloudRuntimeException(String.format("%s returns a unknown resource type[%s] for system tag",
                            ext.getClass(), resType));
                }

                List<SystemTagLifeCycleExtension> lst = lifeCycleExtensions.get(resType);
                if (lst == null) {
                    lst = new ArrayList<>();
                    lifeCycleExtensions.put(resType, lst);
                }

                lst.add(ext);
            }
        }

        createTagExtensions.addAll(pluginRgty.getExtensionList(CreateTagFromMsgExtensionPoint.class));
    }

    private boolean isTagExisting(String resourceUuid, String tag, TagType type, String resourceType) {
        if (type == TagType.User) {
            return Q.New(UserTagVO.class).eq(UserTagVO_.resourceType, resourceType)
                    .eq(UserTagVO_.tag, tag)
                    .eq(UserTagVO_.resourceUuid, resourceUuid)
                    .isExists();
        } else {
            return Q.New(SystemTagVO.class).eq(SystemTagVO_.resourceType, resourceType)
                    .eq(SystemTagVO_.tag, tag)
                    .eq(SystemTagVO_.resourceUuid, resourceUuid)
                    .isExists();
        }
    }

    @Transactional
    private TagInventory createTag(String resourceUuid, String tag, TagType type, String resourceType) {
        if (!resourceTypeClassMap.keySet().contains(resourceType)) {
            throw new IllegalArgumentException(String.format("no resource type[%s] found for tag", resourceType));
        }

        if (isTagExisting(resourceUuid, tag, type, resourceType)) {
            throw new OperationFailureException(operr("Duplicated Tag[tag:%s, type:%s, resourceType:%s, resourceUuid:%s]",
                    tag, type, resourceType, resourceUuid));
        }

        if (type == TagType.User) {
            UserTagVO vo = new UserTagVO();
            vo.setResourceType(resourceType);
            vo.setResourceUuid(resourceUuid);
            vo.setUuid(Platform.getUuid());
            vo.setTag(tag);
            vo.setType(type);
            dbf.getEntityManager().persist(vo);
            dbf.getEntityManager().flush();
            dbf.getEntityManager().refresh(vo);
            return UserTagInventory.valueOf(vo);
        } else {
            SystemTagVO vo = new SystemTagVO();
            vo.setResourceType(resourceType);
            vo.setUuid(Platform.getUuid());
            vo.setResourceUuid(resourceUuid);
            vo.setInherent(true);
            vo.setTag(tag);
            vo.setType(type);

            preTagCreated(SystemTagInventory.valueOf(vo));

            dbf.getEntityManager().persist(vo);
            dbf.getEntityManager().flush();
            dbf.getEntityManager().refresh(vo);

            SystemTagInventory stag = SystemTagInventory.valueOf(vo);
            fireTagCreated(list(stag));
            return stag;
        }
    }

    @Override
    @Deferred
    @Transactional(noRollbackFor = ApiMessageInterceptionException.class)
    public SystemTagInventory createNonInherentSystemTag(String resourceUuid, String tag, String resourceType) {
        if (isTagExisting(resourceUuid, tag, TagType.System, resourceType)) {
            return null;
        }

        validateSystemTag(resourceUuid, resourceType, tag);

        SystemTagVO vo = new SystemTagVO();
        vo.setResourceType(resourceType);
        vo.setUuid(Platform.getUuid());
        vo.setResourceUuid(resourceUuid);
        vo.setInherent(false);
        vo.setTag(tag);
        vo.setType(TagType.System);

        preTagCreated(SystemTagInventory.valueOf(vo));

        dbf.getEntityManager().persist(vo);
        dbf.getEntityManager().flush();
        dbf.getEntityManager().refresh(vo);

        SystemTagInventory inv = SystemTagInventory.valueOf(vo);

        final SystemTagVO finalVo = vo;
        Defer.guard(() -> dbf.remove(finalVo));

        fireTagCreated(list(inv));
        return inv;
    }

    @Override
    public SystemTagInventory createInherentSystemTag(String resourceUuid, String tag, String resourceType) {
        if (isTagExisting(resourceUuid, tag, TagType.System, resourceType)) {
            return null;
        }

        validateSystemTag(resourceUuid, resourceType, tag);

        SystemTagVO vo = new SystemTagVO();
        vo.setResourceType(resourceType);
        vo.setUuid(Platform.getUuid());
        vo.setResourceUuid(resourceUuid);
        vo.setInherent(true);
        vo.setTag(tag);
        vo.setType(TagType.System);

        preTagCreated(SystemTagInventory.valueOf(vo));

        vo = dbf.persistAndRefresh(vo);

        SystemTagInventory inv = SystemTagInventory.valueOf(vo);
        fireTagCreated(list(inv));

        return inv;
    }

    @Override
    public void createInherentSystemTags(List<String> sysTags, String resourceUuid, String resourceType) {
        for (String tag : sysTags) {
            createInherentSystemTag(resourceUuid, tag, resourceType);
        }
    }

    @Override
    public void createNonInherentSystemTags(List<String> sysTags, String resourceUuid, String resourceType) {
        for (String tag : sysTags) {
            createNonInherentSystemTag(resourceUuid, tag, resourceType);
        }
    }

    @Override
    @Transactional
    public void createTagsFromAPICreateMessage(APICreateMessage msg, String resourceUuid, String resourceType) {
        createTagsFromAPICreateMessage(msg, resourceUuid, resourceType, true);
    }

    @Override
    public void createTagsFromAPICreateMessage(APICreateMessage msg, String resourceUuid, String resourceType, boolean forceMatch) {
        if (forceMatch) {
            createTags(msg.getSystemTags(), msg.getUserTags(), resourceUuid, resourceType);
        } else {
            // User tags first, then system tags
            createTags(Collections.emptyList(), msg.getUserTags(), resourceUuid, resourceType);
            if (msg.getSystemTags() != null) {
                for (String tag: msg.getSystemTags()) {
                    if (isValidSystemTag(resourceUuid, resourceType, tag)) {
                        createTags(Collections.singletonList(tag), Collections.emptyList(), resourceUuid, resourceType);
                    }
                }
            }
        }
        createTagExtensions.forEach(it -> it.afterCreateTagFromMsg(msg, resourceUuid));
    }

    @Override
    public TagInventory createSysTag(String resourceUuid, String tag, String resourceType) {
        validateSystemTag(resourceUuid, resourceType, tag);

        return createTag(resourceUuid, tag, TagType.System, resourceType);
    }

    @Override
    public TagInventory createUserTag(String resourceUuid, String tag, String resourceType) {
        return createTag(resourceUuid, tag, TagType.User, resourceType);
    }

    @Override
    public TagInventory createSysTag(String resourceUuid, Enum tag, String resourceType) {
        validateSystemTag(resourceUuid, resourceType, tag.toString());

        return createSysTag(resourceUuid, tag.toString(), resourceType);
    }

    @Override
    public TagInventory createUserTag(String resourceUuid, Enum tag, String resourceType) {
        return createUserTag(resourceUuid, tag.toString(), resourceType);
    }

    @Override
    @Transactional
    public void copySystemTag(String srcResourceUuid, String srcResourceType,
                              String dstResourceUuid, String dstResourceType) {
        copySystemTag(srcResourceUuid, srcResourceType, dstResourceUuid, dstResourceType, true);
        copySystemTag(srcResourceUuid, srcResourceType, dstResourceUuid, dstResourceType, false);
    }

    @Override
    @Transactional
    public void copySystemTag(String srcResourceUuid, String srcResourceType,
                              String dstResourceUuid, String dstResourceType, boolean inherent) {
        String sql = "select stag" +
                " from SystemTagVO stag" +
                " where stag.resourceUuid = :ruuid" +
                " and stag.resourceType = :rtype" +
                " and stag.inherent = :ih";
        TypedQuery<SystemTagVO> srcq = dbf.getEntityManager().createQuery(sql, SystemTagVO.class);
        srcq.setParameter("ruuid", srcResourceUuid);
        srcq.setParameter("rtype", srcResourceType);
        srcq.setParameter("ih", inherent);
        List<SystemTagVO> srctags = srcq.getResultList();
        if (srctags.isEmpty()) {
            return;
        }

        for (SystemTagVO stag : srctags) {
            SystemTagVO ntag = new SystemTagVO(stag);
            ntag.setUuid(Platform.getUuid());
            ntag.setResourceType(dstResourceType);
            ntag.setResourceUuid(dstResourceUuid);
            dbf.getEntityManager().persist(ntag);
        }
    }

    @Override
    public SystemTagInventory updateSystemTag(String tagUuid, String newTag) {
        SystemTagVO vo = dbf.findByUuid(tagUuid, SystemTagVO.class);
        SystemTagInventory old = SystemTagInventory.valueOf(vo);
        if (!vo.getTag().equals(newTag)) {
            vo.setTag(newTag);

            preTagUpdated(old, vo.toInventory());

            vo = dbf.updateAndRefresh(vo);
            SystemTagInventory nt = SystemTagInventory.valueOf(vo);
            fireTagUpdated(old, nt);
            return SystemTagInventory.valueOf(vo);
        } else {
            return old;
        }
    }

    @Override
    public List<String> findSystemTags(String resourceUuid) {
        SimpleQuery<SystemTagVO> q = dbf.createQuery(SystemTagVO.class);
        q.select(SystemTagVO_.tag);
        q.add(SystemTagVO_.resourceUuid, SimpleQuery.Op.EQ, resourceUuid);
        return q.listValue();
    }

    @Override
    public List<String> findUserTags(String resourceUuid) {
        SimpleQuery<UserTagVO> q = dbf.createQuery(UserTagVO.class);
        q.select(UserTagVO_.tag);
        q.add(UserTagVO_.resourceUuid, SimpleQuery.Op.EQ, resourceUuid);
        return q.listValue();
    }

    private boolean hasTag(String resourceUuid, String tag, TagType tagType) {
        if (tagType == TagType.System) {
            SimpleQuery<SystemTagVO> q = dbf.createQuery(SystemTagVO.class);
            q.add(SystemTagVO_.resourceUuid, SimpleQuery.Op.EQ, resourceUuid);
            q.add(SystemTagVO_.tag, SimpleQuery.Op.EQ, tag);
            return q.isExists();
        } else {
            SimpleQuery<UserTagVO> q = dbf.createQuery(UserTagVO.class);
            q.add(UserTagVO_.resourceUuid, SimpleQuery.Op.EQ, resourceUuid);
            q.add(UserTagVO_.tag, SimpleQuery.Op.EQ, tag);
            return q.isExists();
        }
    }

    @Override
    public boolean hasSystemTag(String resourceUuid, String tag) {
        return hasTag(resourceUuid, tag, TagType.System);
    }

    @Override
    public boolean hasSystemTag(String resourceUuid, Enum tag) {
        return hasTag(resourceUuid, tag.toString(), TagType.System);
    }

    @Override
    public void deleteSystemTag(String uuid) {
        SystemTagVO vo = Q.New(SystemTagVO.class).eq(SystemTagVO_.uuid, uuid).find();

        if (vo == null) {
            return;
        }

        preTagDeleted(SystemTagInventory.valueOf(vo));

        dbf.remove(vo);

        fireTagDeleted(Collections.singletonList(SystemTagInventory.valueOf(vo)));
    }

    @Override
    public void deleteSystemTag(String tag, String resourceUuid, String resourceType, Boolean inherit) {
        deleteSystemTag(tag, resourceUuid, resourceType, inherit, false);
    }

    private void deleteSystemTag(String tag, String resourceUuid, String resourceType, Boolean inherit, boolean useLike) {
        DebugUtils.Assert(tag != null || resourceUuid != null || resourceType != null,
                "tag, resourceUuid, resourceType cannot all be null");
        SimpleQuery<SystemTagVO> q = dbf.createQuery(SystemTagVO.class);
        if (tag != null) {
            if (useLike) {
                q.add(SystemTagVO_.tag, Op.LIKE, tag);
            } else {
                q.add(SystemTagVO_.tag, Op.EQ, tag);
            }
        }
        if (resourceUuid != null) {
            q.add(SystemTagVO_.resourceUuid, Op.EQ, resourceUuid);
        }
        if (resourceType != null) {
            q.add(SystemTagVO_.resourceType, Op.EQ, resourceType);
        }
        if (inherit != null) {
            q.add(SystemTagVO_.inherent, Op.EQ, inherit);
        }

        List<SystemTagVO> vos = q.list();

        if (!vos.isEmpty()) {
            for (SystemTagVO vo : vos) {
                preTagDeleted(SystemTagInventory.valueOf(vo));
            }
        }

        dbf.removeCollection(vos, SystemTagVO.class);

        if (!vos.isEmpty()) {
            fireTagDeleted(SystemTagInventory.valueOf(vos));
        }
    }

    @Override
    public void deleteSystemTagUseLike(String tag, String resourceUuid, String resourceType, Boolean inherit) {
        deleteSystemTag(tag, resourceUuid, resourceType, inherit, true);
    }

    void fireTagDeleted(List<SystemTagInventory> tags) {
        for (SystemTagInventory tag : tags) {
            List<SystemTagLifeCycleExtension> exts = lifeCycleExtensions.get(tag.getResourceType());
            if (exts != null) {
                for (SystemTagLifeCycleExtension ext : exts) {
                    try {
                        ext.tagDeleted(tag);
                    } catch (Exception e) {
                        logger.warn(String.format("unhandled exception when calling %s", ext.getClass()), e);
                    }
                }
            }
        }
    }

    void fireTagCreated(List<SystemTagInventory> tags) {
        for (SystemTagInventory tag : tags) {
            List<SystemTagLifeCycleExtension> exts = lifeCycleExtensions.get(tag.getResourceType());
            if (exts != null) {
                for (SystemTagLifeCycleExtension ext : exts) {
                    try {
                        ext.tagCreated(tag);
                    } catch (Exception e) {
                        logger.warn(String.format("unhandled exception when calling %s", ext.getClass()));
                    }
                }
            }
        }
    }

    private void fireTagUpdated(SystemTagInventory old, SystemTagInventory newTag) {
        List<SystemTagLifeCycleExtension> exts = lifeCycleExtensions.get(old.getResourceType());
        if (exts != null) {
            for (SystemTagLifeCycleExtension ext : exts) {
                try {
                    ext.tagUpdated(old, newTag);
                } catch (Exception e) {
                    logger.warn(String.format("unhandled exception when calling %s", ext.getClass()));
                }
            }
        }
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        bus.dealWithUnknownMessage(msg);
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APICreateUserTagMsg) {
            handle((APICreateUserTagMsg) msg);
        } else if (msg instanceof APICreateSystemTagMsg) {
            handle((APICreateSystemTagMsg) msg);
        } else if (msg instanceof APIDeleteTagMsg) {
            handle((APIDeleteTagMsg) msg);
        } else if (msg instanceof APIUpdateSystemTagMsg) {
            handle((APIUpdateSystemTagMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    @Deferred
    private void handle(APIUpdateSystemTagMsg msg) {
        APIUpdateSystemTagEvent evt = new APIUpdateSystemTagEvent(msg.getId());
        ErrorCode err = checkPemission(msg.getTag(), msg.getSession());
        if (err != null) {
            evt.setError(err);
            bus.publish(evt);
            return;
        }

        evt.setInventory(updateSystemTag(msg.getUuid(), msg.getTag()));
        bus.publish(evt);
    }

    private void handle(APICreateSystemTagMsg msg) {
        APICreateSystemTagEvent evt = new APICreateSystemTagEvent(msg.getId());
        ErrorCode err = checkPemission(msg.getTag(), msg.getSession());
        if (err != null) {
            evt.setError(err);
            bus.publish(evt);
            return;
        }

        SystemTagInventory inv = createNonInherentSystemTag(msg.getResourceUuid(), msg.getTag(), msg.getResourceType());
        evt.setInventory(inv);
        bus.publish(evt);
    }

    private void handle(APICreateUserTagMsg msg) {
        APICreateUserTagEvent evt = new APICreateUserTagEvent(msg.getId());
        UserTagInventory inv = (UserTagInventory) createUserTag(msg.getResourceUuid(), msg.getTag(), msg.getResourceType());
        evt.setInventory(inv);
        bus.publish(evt);
    }

    private void handle(APIDeleteTagMsg msg) {
        APIDeleteTagEvent evt = new APIDeleteTagEvent(msg.getId());
        SystemTagVO stag = dbf.findByUuid(msg.getUuid(), SystemTagVO.class);
        if (stag != null) {
            ErrorCode err = checkPemission(stag.getTag(), msg.getSession());
            if (err != null) {
                evt.setError(err);
                bus.publish(evt);
                return;
            }

            preTagDeleted(SystemTagInventory.valueOf(stag));
        }

        dbf.removeByPrimaryKey(msg.getUuid(), SystemTagVO.class);
        dbf.removeByPrimaryKey(msg.getUuid(), UserTagVO.class);
        dbf.removeByPrimaryKey(msg.getUuid(), TagPatternVO.class);

        if (stag != null) {
            fireTagDeleted(list(SystemTagInventory.valueOf(stag)));
        }

        bus.publish(evt);
    }

    @Override
    public Collection<String> getManagedEntityNames() {
        return resourceTypeClassMap.keySet();
    }

    private boolean isValidSystemTag(String resourceUuid, String resourceType, String tag) {
        boolean checked = false;
        List<SystemTag> tags = resourceTypeSystemTagMap.get(resourceType);
        if (tags != null) {
            for (SystemTag stag : tags) {
                if (stag.isMatch(tag)) {
                    stag.validate(resourceUuid, resourceTypeClassMap.get(resourceType), tag);
                    checked = true;
                }
            }
        }
        return checked;
    }


    @Override
    public void validateSystemTag(String resourceUuid, String resourceType, String tag) {
        if (!isValidSystemTag(resourceUuid, resourceType, tag)) {
            throw new ApiMessageInterceptionException(
                    argerr("no system tag matches[%s] for resourceType[%s]", tag, resourceType));
        }
    }

    @Override
    public void installAfterResourceDeletionOperator(String resourceType, SystemTagResourceDeletionOperator operator) {
        if (!resourceTypeClassMap.containsKey(resourceType)) {
            throw new CloudRuntimeException(String.format("cannot find resource type[%s] in tag system ", resourceType));
        }

        List<SystemTagResourceDeletionOperator> operators = resourceDeletionOperators.get(resourceType);
        if (operators == null) {
            operators = new ArrayList<>();
            resourceDeletionOperators.put(resourceType, operators);
        }
        operators.add(operator);
    }

    public List<String> filterSystemTags(List<String> systemTags, String resourceType) {
        List<SystemTag> tags = resourceTypeSystemTagMap.get(resourceType);
        return systemTags.stream()
                .filter(it -> tags.stream().anyMatch(sys -> sys.isMatch(it)))
                .collect(Collectors.toList());
    }

    @Override
    public void installCreateMessageValidator(String resourceType, SystemTagCreateMessageValidator validator) {
        if (!resourceTypeClassMap.containsKey(resourceType)) {
            throw new CloudRuntimeException(String.format("cannot find resource type[%s] in tag system ", resourceType));
        }

        List<SystemTagCreateMessageValidator> validators = createMessageValidators.get(resourceType);
        if (validators == null) {
            validators = new ArrayList<SystemTagCreateMessageValidator>();
            createMessageValidators.put(resourceType, validators);
        }
        validators.add(validator);
    }

    @Override
    public void createTags(List<String> systemTags, List<String> userTags, String resourceUuid, String resourceType) {
        if (systemTags != null && !systemTags.isEmpty()) {
            for (String sysTag : systemTags) {
                if (TagConstant.isEphemeralTag(sysTag)) {
                    continue;
                }

                // Not all systemTags are for the resources used by APICreateMessage
                createNonInherentSystemTag(resourceUuid, sysTag, resourceType);
            }
        }
        if (userTags != null && !userTags.isEmpty()) {
            for (String utag : userTags) {
                createUserTag(resourceUuid, utag, resourceType);
            }
        }
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(TagConstant.SERVICE_ID);
    }

    @Override
    public boolean start() {
        populateExtensions();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public List<Class> getEntityClassForSoftDeleteEntityExtension() {
        return null;
    }

    private List<String> getResourceTypes(Class entityClass) {
        List<String> types = new ArrayList<>();
        while (entityClass != Object.class) {
            types.add(entityClass.getSimpleName());
            entityClass = entityClass.getSuperclass();
        }
        return types;
    }

    @Transactional
    private void postDelete(Collection entityIds, Class entityClass) {
        List<String> rtypes = getResourceTypes(entityClass);
        String sql = "delete from SystemTagVO s" +
                " where s.resourceType in (:resourceTypes)" +
                " and s.resourceUuid in (:resourceUuids)";
        Query q = dbf.getEntityManager().createQuery(sql);
        q.setParameter("resourceTypes", rtypes);
        q.setParameter("resourceUuids", entityIds);
        q.executeUpdate();

        sql = "delete from UserTagVO s" +
                " where s.resourceType in (:resourceTypes)" +
                " and s.resourceUuid in (:resourceUuids)";
        q = dbf.getEntityManager().createQuery(sql);
        q.setParameter("resourceTypes", rtypes);
        q.setParameter("resourceUuids", entityIds);
        q.executeUpdate();

        List<SystemTagResourceDeletionOperator> operators = resourceDeletionOperators.get(entityClass.getSimpleName());

        if (operators == null) {
            return;
        }

        for (SystemTagResourceDeletionOperator operator : operators) {
            operator.execute(entityIds);
        }
    }

    @Override
    public void postSoftDelete(Collection entityIds, Class entityClass) {
        postDelete(entityIds, entityClass);
    }

    @Override
    public List<Class> getMessageClassToIntercept() {
        return list((Class) APICreateMessage.class);
    }

    @Override
    public InterceptorPosition getPosition() {
        return InterceptorPosition.END;
    }

    private boolean isCheckSystemTags(APIMessage msg) {
        if (msg.getSystemTags() == null) {
            return false;
        }

        if (msg.getSystemTags().isEmpty()) {
            return false;
        }

        for (String s : msg.getSystemTags()) {
            if (!TagConstant.isEphemeralTag(s)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        APICreateMessage cmsg = (APICreateMessage) msg;
        if (isCheckSystemTags(msg)) {
            cmsg.setSystemTags(removeDuplicateFromList(cmsg.getSystemTags()));

            for (String tag : cmsg.getSystemTags()) {
                boolean checked = false;
                for (SystemTag stag : systemTags) {
                    if (stag.isMatch(tag)) {
                        checked = true;
                        break;
                    }
                }

                ErrorCode err = checkPemission(tag, msg.getSession());
                if (err != null) {
                    throw new ApiMessageInterceptionException(err);
                }

                if (!checked) {
                    throw new ApiMessageInterceptionException(argerr("no system tag matches %s", tag));
                }
            }

            Class resourceType = resourceTypeCreateMessageMap.get(cmsg.getClass());
            if (resourceType == null) {
                throw new ApiMessageInterceptionException(inerr(
                        "API message[%s] doesn't define resource type by @TagResourceType",
                        cmsg.getClass().getName()
                ));
            }

            List<SystemTagCreateMessageValidator> validators = createMessageValidators.get(resourceType.getSimpleName());
            if (validators != null && !validators.isEmpty()) {
                for (SystemTagCreateMessageValidator validator : validators) {
                    validator.validateSystemTagInCreateMessage(cmsg);
                }
            }
        }

        return msg;
    }

    private ErrorCode checkPemission(String tag, SessionInventory session){
        if (session == null || session.getUuid() == null || AccountConstant.isAdminPermission(session.getAccountUuid())) {
            return null;
        }

        if (adminOnlySystemTags.stream().anyMatch(it -> it.isMatch(tag))) {
            return operr("tag[%s] is only for admin", tag);
        }
        return null;
    }

    private boolean isTagMatch(SystemTagInventory t, SystemTag s) {
        return s.isMatch(t.getTag());
    }

    @Override
    public List<String> getResourceTypeOfSystemTags() {
        List<String> lst = new ArrayList<>();
        lst.addAll(resourceTypeClassMap.keySet());
        return lst;
    }

    void preTagCreated(SystemTagInventory tag) {
        List<SystemTag> tags = resourceTypeSystemTagMap.get(tag.getResourceType());
        if (tags != null) {
            for (SystemTag stag : tags) {
                if (isTagMatch(tag, stag)) {
                    stag.callCreatedJudger(tag);
                }
            }
        }
    }

    @Override
    public void tagCreated(SystemTagInventory tag) {
        List<SystemTag> tags = resourceTypeSystemTagMap.get(tag.getResourceType());
        if (tags != null) {
            for (SystemTag stag : tags) {
                if (isTagMatch(tag, stag)) {
                    stag.callTagCreatedListener(tag);
                }
            }
        }
    }

    void preTagDeleted(SystemTagInventory tag) {
        List<SystemTag> tags = resourceTypeSystemTagMap.get(tag.getResourceType());
        if (tags != null) {
            for (SystemTag stag : tags) {
                if (isTagMatch(tag, stag)) {
                    stag.callDeletedJudger(tag);
                }
            }
        }
    }

    @Override
    public void tagDeleted(SystemTagInventory tag) {
        List<SystemTag> tags = resourceTypeSystemTagMap.get(tag.getResourceType());
        if (tags != null) {
            for (SystemTag stag : tags) {
                if (isTagMatch(tag, stag)) {
                    stag.callTagDeletedListener(tag);
                }
            }
        }
    }

    private void preTagUpdated(SystemTagInventory old, SystemTagInventory newTag) {
        List<SystemTag> tags = resourceTypeSystemTagMap.get(old.getResourceType());
        if (tags != null) {
            for (SystemTag stag : tags) {
                if (isTagMatch(old, stag) && isTagMatch(newTag, stag)) {
                    stag.callUpdatedJudger(old, newTag);
                }
            }
        }
    }

    @Override
    public void tagUpdated(SystemTagInventory old, SystemTagInventory newTag) {
        List<SystemTag> tags = resourceTypeSystemTagMap.get(old.getResourceType());
        if (tags != null) {
            for (SystemTag stag : tags) {
                if (isTagMatch(old, stag) && isTagMatch(newTag, stag)) {
                    stag.callTagUpdatedListener(old, newTag);
                }
            }
        }
    }

    @Override
    public List<Class> getEntityClassForHardDeleteEntityExtension() {
        return autoDeleteTagClasses;
    }

    @Override
    public void postHardDelete(Collection entityIds, Class entityClass) {
        postDelete(entityIds, entityClass);
    }
}
