package org.zstack.storage.primary;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.config.GlobalConfigException;
import org.zstack.core.config.GlobalConfigValidatorExtensionPoint;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.DbEntityLister;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.core.safeguard.Guard;
import org.zstack.header.AbstractService;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.managementnode.ManagementNodeChangeListener;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.*;
import org.zstack.header.tag.SystemTagValidator;
import org.zstack.search.GetQuery;
import org.zstack.search.SearchQuery;
import org.zstack.tag.TagManager;
import org.zstack.utils.*;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import javax.persistence.LockModeType;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.*;
import java.util.concurrent.Callable;

public class PrimaryStorageManagerImpl extends AbstractService implements PrimaryStorageManager, ManagementNodeChangeListener {
    private static final CLogger logger = Utils.getLogger(PrimaryStorageManager.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private DbEntityLister dl;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private TagManager tagMgr;
    @Autowired
    private ResourceDestinationMaker destMaker;

    private Map<String, PrimaryStorageFactory> primaryStorageFactories = Collections.synchronizedMap(new HashMap<String, PrimaryStorageFactory>());
    private Map<String, PrimaryStorageAllocatorStrategyFactory> allocatorFactories = Collections
            .synchronizedMap(new HashMap<String, PrimaryStorageAllocatorStrategyFactory>());
    private static final Set<Class> allowedMessageAfterSoftDeletion = new HashSet<Class>();

    static {
        allowedMessageAfterSoftDeletion.add(PrimaryStorageDeletionMsg.class);
    }

    void init() {
        PrimaryStorageSystemTags.PRIMARY_STORAGE_ALLOCATOR_UUID_TAG.installValidator(new SystemTagValidator() {
            @Override
            public void validateSystemTag(String resourceUuid, Class resourceType, String systemTag) {
                String uuid = PrimaryStorageSystemTags.PRIMARY_STORAGE_ALLOCATOR_UUID_TAG.getTokenByTag(systemTag, PrimaryStorageSystemTags.PRIMARY_STORAGE_ALLOCATOR_UUID_TAG_TOKEN);
                if (!StringDSL.isZstackUuid(uuid)) {
                    throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                            String.format("%s is invalid. %s is not a valid zstack uuid", systemTag, uuid)
                    ));
                }

                if (!dbf.isExist(uuid, PrimaryStorageVO.class)) {
                    throw new ApiMessageInterceptionException(errf.stringToInvalidArgumentError(
                            String.format("no primary storage[uuid:%s] found", resourceUuid)
                    ));
                }
            }
        });
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIAddPrimaryStorageMsg) {
            handle((APIAddPrimaryStorageMsg) msg);
        } else if (msg instanceof APIListPrimaryStorageMsg) {
            handle((APIListPrimaryStorageMsg) msg);
        } else if (msg instanceof APISearchPrimaryStorageMsg) {
        	handle((APISearchPrimaryStorageMsg) msg);
        } else if (msg instanceof APIGetPrimaryStorageMsg) {
            handle((APIGetPrimaryStorageMsg) msg);
        } else if (msg instanceof PrimaryStorageMessage) {
            passThrough((PrimaryStorageMessage) msg);
        } else if (msg instanceof APIGetPrimaryStorageTypesMsg) {
            handle((APIGetPrimaryStorageTypesMsg) msg);
        } else if (msg instanceof APIGetPrimaryStorageAllocatorStrategiesMsg) {
            handle((APIGetPrimaryStorageAllocatorStrategiesMsg) msg);
        } else if (msg instanceof APIGetPrimaryStorageCapacityMsg) {
            handle((APIGetPrimaryStorageCapacityMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(final APIGetPrimaryStorageCapacityMsg msg) {
        APIGetPrimaryStorageCapacityReply reply = new APIGetPrimaryStorageCapacityReply();

        Tuple ret = new Callable<Tuple>() {
            @Override
            @Transactional(readOnly = true)
            public Tuple call() {
                if (msg.getPrimaryStorageUuids() != null && !msg.getPrimaryStorageUuids().isEmpty()) {
                    String sql = "select sum(psc.totalCapacity), sum(psc.availableCapacity) from PrimaryStorageCapacityVO psc where psc.uuid in (:psUuids)";
                    TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
                    q.setParameter("psUuids", msg.getPrimaryStorageUuids());
                    return q.getSingleResult();
                } else if (msg.getClusterUuids() != null && !msg.getClusterUuids().isEmpty()) {
                    String sql = "select sum(psc.totalCapacity), sum(psc.availableCapacity) from PrimaryStorageCapacityVO psc, PrimaryStorageClusterRefVO ref where ref.primaryStorageUuid = psc.uuid and ref.clusterUuid in (:clusterUuids)";
                    TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
                    q.setParameter("clusterUuids", msg.getClusterUuids());
                    return q.getSingleResult();
                } else if (msg.getZoneUuids() != null && !msg.getZoneUuids().isEmpty()) {
                    String sql = "select sum(psc.totalCapacity), sum(psc.availableCapacity) from PrimaryStorageCapacityVO psc, PrimaryStorageVO ps where ps.uuid = psc.uuid and ps.zoneUuid in (:zoneUuids)";
                    TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
                    q.setParameter("zoneUuids", msg.getZoneUuids());
                    return q.getSingleResult();
                }

                throw new CloudRuntimeException("should not be here");
            }
        }.call();

        Long total = ret.get(0, Long.class);
        Long avail = ret.get(1, Long.class);
        reply.setTotalCapacity(total == null ? 0 : total);
        reply.setAvailableCapacity(avail == null ? 0 : avail);
        bus.reply(msg, reply);
    }

    private void handle(APIGetPrimaryStorageAllocatorStrategiesMsg msg) {
        APIGetPrimaryStorageAllocatorStrategiesReply reply = new APIGetPrimaryStorageAllocatorStrategiesReply();
        reply.setPrimaryStorageAllocatorStrategies(PrimaryStorageAllocatorStrategyType.getAllExposedTypeNames());
        bus.reply(msg, reply);
    }

    private void handle(APIGetPrimaryStorageTypesMsg msg) {
        APIGetPrimaryStorageTypesReply reply = new APIGetPrimaryStorageTypesReply();
        List<String> ret = new ArrayList<String>();
        ret.addAll(PrimaryStorageType.getAllTypeNames());
        reply.setPrimaryStorageTypes(ret);
        bus.reply(msg, reply);
    }

    private void handle(APIGetPrimaryStorageMsg msg) {
        GetQuery q = new GetQuery();
        String res = q.getAsString(msg, PrimaryStorageInventory.class);
        APIGetPrimaryStorageReply reply = new APIGetPrimaryStorageReply();
        reply.setInventory(res);
        bus.reply(msg, reply);
    }

    private void handle(APISearchPrimaryStorageMsg msg) {
    	SearchQuery<PrimaryStorageInventory> sq = SearchQuery.create(msg, PrimaryStorageInventory.class);
    	String content = sq.listAsString();
    	APISearchPrimaryStorageReply reply = new APISearchPrimaryStorageReply();
    	reply.setContent(content);
    	bus.reply(msg, reply);
	}

	private void handle(APIListPrimaryStorageMsg msg) {
        List<PrimaryStorageVO> vos = dl.listByApiMessage(msg, PrimaryStorageVO.class);
        List<PrimaryStorageInventory> invs = PrimaryStorageInventory.valueOf(vos);
        APIListPrimaryStorageReply reply = new APIListPrimaryStorageReply();
        reply.setInventories(invs);
        bus.reply(msg, reply);
    }

    private void passThrough(PrimaryStorageMessage pmsg) {
        PrimaryStorageVO vo = dbf.findByUuid(pmsg.getPrimaryStorageUuid(), PrimaryStorageVO.class);
        if (vo == null && allowedMessageAfterSoftDeletion.contains(pmsg.getClass())) {
            PrimaryStorageEO eo = dbf.findByUuid(pmsg.getPrimaryStorageUuid(), PrimaryStorageEO.class);
            vo = ObjectUtils.newAndCopy(eo, PrimaryStorageVO.class);
        }

        Message msg = (Message) pmsg;
        if (vo == null) {
            String err = String.format("Cannot find primary storage[uuid:%s], it may have been deleted", pmsg.getPrimaryStorageUuid());
            bus.replyErrorByMessageType(msg, errf.instantiateErrorCode(SysErrors.RESOURCE_NOT_FOUND, err));
            return;
        }

        PrimaryStorageFactory factory = getPrimaryStorageFactory(PrimaryStorageType.valueOf(vo.getType()));
        PrimaryStorage ps = factory.getPrimaryStorage(vo);
        ps.handleMessage(msg);
    }

    @Guard
    private void handle(final APIAddPrimaryStorageMsg msg) {
        PrimaryStorageType type = PrimaryStorageType.valueOf(msg.getType());
        final PrimaryStorageFactory factory = getPrimaryStorageFactory(type);

        PrimaryStorageVO vo = new PrimaryStorageVO();
        if (msg.getResourceUuid() != null) {
            vo.setUuid(msg.getResourceUuid());
        } else {
            vo.setUuid(Platform.getUuid());
        }
        vo.setUrl(msg.getUrl());
        vo.setType(type.toString());
        vo.setName(msg.getName());
        vo.setDescription(msg.getDescription());
        vo.setState(PrimaryStorageState.Enabled);
        vo.setStatus(PrimaryStorageStatus.Connecting);
        vo.setZoneUuid(msg.getZoneUuid());

        final APIAddPrimaryStorageEvent evt = new APIAddPrimaryStorageEvent(msg.getId());
        final PrimaryStorageInventory inv = factory.createPrimaryStorage(vo, msg);
        vo = dbf.findByUuid(vo.getUuid(), PrimaryStorageVO.class);

        tagMgr.createTagsFromAPICreateMessage(msg, inv.getUuid(), PrimaryStorageVO.class.getSimpleName());

        PrimaryStorageCapacityVO capvo =  dbf.findByUuid(vo.getUuid(), PrimaryStorageCapacityVO.class);
        if (capvo == null) {
            capvo = new PrimaryStorageCapacityVO();
            capvo.setUuid(vo.getUuid());
            dbf.persist(capvo);
        }

        final ConnectPrimaryStorageMsg cmsg = new ConnectPrimaryStorageMsg();
        cmsg.setPrimaryStorageUuid(inv.getUuid());
        cmsg.setNewAdded(true);
        bus.makeTargetServiceIdByResourceUuid(cmsg, PrimaryStorageConstant.SERVICE_ID, inv.getUuid());
        final PrimaryStorageVO finalVo = vo;
        bus.send(cmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    PrimaryStorageInventory pinv = factory.getInventory(finalVo.getUuid());
                    logger.debug(String.format("successfully add primary storage[uuid:%s, name:%s, url: %s]", finalVo.getUuid(), finalVo.getName(), finalVo.getUrl()));
                    evt.setInventory(pinv);
                } else {
                    evt.setErrorCode(reply.getError());
                    logger.warn(String.format("failed to connect primary storage[uuid:%s, name:%s, url:%s]", finalVo.getUuid(), finalVo.getName(), finalVo.getUrl()));
                    dbf.remove(finalVo);
                }

                bus.publish(evt);
            }
        });

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
        if (msg instanceof AllocatePrimaryStorageMsg) {
            handle((AllocatePrimaryStorageMsg) msg);
        } else if (msg instanceof ReturnPrimaryStorageCapacityMsg) {
            handle((ReturnPrimaryStorageCapacityMsg) msg);
        } else if (msg instanceof PrimaryStorageMessage) {
            passThrough((PrimaryStorageMessage) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(ReturnPrimaryStorageCapacityMsg msg) {
        returnPrimaryStorageCapacity(msg.getPrimaryStorageUuid(), msg.getDiskSize());
    }

    private void handle(AllocatePrimaryStorageMsg msg) {
        AllocatePrimaryStorageReply reply = new AllocatePrimaryStorageReply(null);
        String allocatorStrategyType = null;
        for (PrimaryStorageAllocatorStrategyExtensionPoint ext : pluginRgty.getExtensionList(PrimaryStorageAllocatorStrategyExtensionPoint.class)) {
            allocatorStrategyType = ext.getPrimaryStorageAllocatorStrategyName(msg);
            if (allocatorStrategyType != null) {
                break;
            }
        }

        if (allocatorStrategyType == null) {
            allocatorStrategyType = msg.getAllocationStrategy() == null ? PrimaryStorageConstant.DEFAULT_PRIMARY_STORAGE_ALLOCATION_STRATEGY_TYPE : msg.getAllocationStrategy();
        }

        if (msg.getExcludeAllocatorStrategies() != null && msg.getExcludeAllocatorStrategies().contains(allocatorStrategyType)) {
            throw new CloudRuntimeException(String.format("%s is set as excluded, there is no available primary storage allocator strategy", allocatorStrategyType));
        }

        PrimaryStorageAllocatorStrategyFactory factory = getPrimaryStorageAlloactorStrategyFactory(
                PrimaryStorageAllocatorStrategyType.valueOf(allocatorStrategyType)
        );
        PrimaryStorageAllocatorStrategy strategy = factory.getPrimaryStorageAllocatorStrategy();
        PrimaryStorageAllocationSpec spec = new PrimaryStorageAllocationSpec();
        spec.setDiskOfferingUuid(msg.getDiskOfferingUuid());
        spec.setVmInstanceUuid(msg.getVmInstanceUuid());
        spec.setSize(msg.getSize());
        spec.setRequiredClusterUuids(msg.getClusterUuids());
        spec.setRequiredHostUuid(msg.getHostUuid());
        spec.setRequiredZoneUuid(msg.getZoneUuid());
        spec.setRequiredPrimaryStorageUuid(msg.getPrimaryStorageUuid());
        spec.setTags(msg.getTags());
        spec.setAllocationMessage(msg);
        spec.setAvoidPrimaryStorageUuids(msg.getExcludePrimaryStorageUuids());
        List<PrimaryStorageInventory> ret = strategy.allocateAllCandidates(spec);
        Iterator<PrimaryStorageInventory> it = ret.iterator();

        PrimaryStorageInventory target = null;
        while (it.hasNext()) {
            PrimaryStorageInventory inv = it.next();
            if (reserve(inv, msg.getSize())) {
                target = inv;
                break;
            } else {
                logger.debug(String.format("concurrent reservation on primary storage[uuid:%s], try next one", inv.getUuid()));
            }
        }

        if (target == null) {
            throw new OperationFailureException(errf.stringToOperationError(String.format("failed to reserve capacity on all qualified primary storage, no primary storage has space available")));
        }

        reply.setPrimaryStorageInventory(target);
        reply.setSize(msg.getSize());
        bus.reply(msg, reply);
    }

    @Transactional
    private boolean reserve(PrimaryStorageInventory inv, long size) {
        PrimaryStorageCapacityVO cvo = dbf.getEntityManager().find(PrimaryStorageCapacityVO.class, inv.getUuid(), LockModeType.PESSIMISTIC_WRITE);
        if (cvo == null) {
            logger.warn(String.format("reserved capacity on primary storage[uuid:%s] failed, the primary storage has been deleted", inv.getUuid()));
            return false;
        }

        long avail = cvo.getAvailableCapacity() - size;
        if (avail <= 0) {
            logger.warn(String.format("reserved capacity on primary storage[uuid:%s] failed, no available capacity on it", inv.getUuid()));
            return false;
        }

        cvo.setAvailableCapacity(avail);
        dbf.getEntityManager().merge(cvo);
        if (logger.isTraceEnabled()) {
            logger.trace(String.format("reserved %s bytes on primary storage[uuid:%s, total:%s, available:%s]",
                    size, inv.getUuid(), cvo.getTotalCapacity(), avail));
        }
        return true;
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(PrimaryStorageConstant.SERVICE_ID);
    }


    @Override
    public boolean start() {
        populateExtensions();
        installGlobalConfigValidator();
        return true;
    }

    private void installGlobalConfigValidator() {
        PrimaryStorageGlobalConfig.RESERVED_CAPACITY.installValidateExtension(new GlobalConfigValidatorExtensionPoint() {
            @Override
            public void validateGlobalConfig(String category, String name, String oldValue, String newValue) throws GlobalConfigException {
                if (!SizeUtils.isSizeString(newValue)) {
                    throw new GlobalConfigException(String.format("%s is not a size string; a size string consists of a number ending with suffix B/K/M/G/T or without suffix; for example, 512M, 1G", newValue));
                }
            }
        });
    }

    @Override
    public boolean stop() {
        return true;
    }

    private void populateExtensions() {
        for (PrimaryStorageAllocatorStrategyFactory f : pluginRgty.getExtensionList(PrimaryStorageAllocatorStrategyFactory.class)) {
            PrimaryStorageAllocatorStrategyFactory old = allocatorFactories.get(f.getPrimaryStorageAllocatorStrategyType().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate PrimaryStorageAlloactorStrategyFactory[%s, %s] for type[%s]",
                        f.getClass().getName(), old.getClass().getName(), f.getPrimaryStorageAllocatorStrategyType()));
            }
            allocatorFactories.put(f.getPrimaryStorageAllocatorStrategyType().toString(), f);
        }

        for (PrimaryStorageFactory f : pluginRgty.getExtensionList(PrimaryStorageFactory.class)) {
            PrimaryStorageFactory old = primaryStorageFactories.get(f.getPrimaryStorageType().toString());
            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate PrimaryStorageFactory[%s, %s] for type[%s]",
                        f.getClass().getName(), old.getClass().getName(), old.getPrimaryStorageType()));
            }
            primaryStorageFactories.put(f.getPrimaryStorageType().toString(), f);
        }
    }


    private PrimaryStorageAllocatorStrategyFactory getPrimaryStorageAlloactorStrategyFactory(PrimaryStorageAllocatorStrategyType type) {
        PrimaryStorageAllocatorStrategyFactory factory = allocatorFactories.get(type.toString());
        if (factory == null) {
            throw new CloudRuntimeException(String.format("No PrimaryStorageAllocatorStrategyFactory for type: %s found", type));
        }
        return factory;
    }

    @Override
    public PrimaryStorageFactory getPrimaryStorageFactory(PrimaryStorageType type) {
        PrimaryStorageFactory factory = primaryStorageFactories.get(type.toString());
        if (factory == null) {
            throw new CloudRuntimeException(String.format("No PrimaryStorageFactory for type: %s found", type));
        }
        return factory;
    }

    @Transactional
    private void returnPrimaryStorageCapacity(String primaryStorageUuid, long diskSize) {
        PrimaryStorageCapacityVO cvo = dbf.getEntityManager().find(PrimaryStorageCapacityVO.class, primaryStorageUuid, LockModeType.PESSIMISTIC_WRITE);
        if (cvo != null) {
            long avail = cvo.getAvailableCapacity() + diskSize;
            if (avail > cvo.getTotalCapacity()) {
                avail = cvo.getTotalCapacity();
            }

            cvo.setAvailableCapacity(avail);
            dbf.getEntityManager().merge(cvo);
            if (logger.isTraceEnabled()) {
                logger.trace(String.format("Successfully return %s bytes to primary storage[uuid:%s]", diskSize, primaryStorageUuid));
            }
        }
    }

    @Override
    public void sendCapacityReportMessage(long total, long avail, String primaryStorageUuid) {
        PrimaryStorageReportPhysicalCapacityMsg msg = new PrimaryStorageReportPhysicalCapacityMsg();
        msg.setTotalCapacity(total);
        msg.setAvailableCapacity(avail);
        msg.setPrimaryStorageUuid(primaryStorageUuid);
        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, primaryStorageUuid);
        bus.send(msg);
    }

    @Override
    public void nodeJoin(String nodeId) {
        logger.debug(String.format("management node[uuid:%s] left, node[uuid:%s] starts taking over primary storage...", nodeId, Platform.getManagementServerId()));
        loadPrimaryStorage();
    }

    @Override
    public void nodeLeft(String nodeId) {

    }

    private List<String> getPrimaryStorageManagedByUs() {
        List<String> ret = new ArrayList<String>();
        SimpleQuery<PrimaryStorageVO> q = dbf.createQuery(PrimaryStorageVO.class);
        q.select(PrimaryStorageVO_.uuid);
        List<String> uuids = q.listValue();
        for (String uuid : uuids) {
            if (destMaker.isManagedByUs(uuid)) {
                ret.add(uuid);
            }
        }

        return ret;
    }

    private void loadPrimaryStorage() {
        List<String> uuids = getPrimaryStorageManagedByUs();
        if (uuids.isEmpty()) {
            return;
        }

        List<ConnectPrimaryStorageMsg> msgs = CollectionUtils.transformToList(uuids, new Function<ConnectPrimaryStorageMsg, String>() {
            @Override
            public ConnectPrimaryStorageMsg call(String arg) {
                ConnectPrimaryStorageMsg msg = new ConnectPrimaryStorageMsg();
                msg.setPrimaryStorageUuid(arg);
                bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, arg);
                return msg;
            }
        });

        bus.send(msgs);
    }

    @Override
    public void iAmDead(String nodeId) {

    }

    @Override
    @AsyncThread
    public void iJoin(String nodeId) {
        logger.debug(String.format("management node[uuid:%s] joins, starts load primary storage ...", nodeId));
        loadPrimaryStorage();
    }
}
