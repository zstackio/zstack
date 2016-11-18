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
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.defer.Deferred;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.AbstractService;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.managementnode.ManagementNodeChangeListener;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.*;
import org.zstack.header.tag.SystemTagValidator;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.search.GetQuery;
import org.zstack.search.SearchQuery;
import org.zstack.tag.TagManager;
import org.zstack.utils.*;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.*;
import java.util.concurrent.Callable;

public class PrimaryStorageManagerImpl extends AbstractService implements PrimaryStorageManager,
        ManagementNodeChangeListener, ManagementNodeReadyExtensionPoint {
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
    @Autowired
    private PrimaryStorageOverProvisioningManager ratioMgr;
    @Autowired
    private PrimaryStoragePhysicalCapacityManager physicalCapacityMgr;

    private Map<String, RecalculatePrimaryStorageCapacityExtensionPoint> recalculateCapacityExtensions = new HashMap<>();
    private Map<String, PrimaryStorageFactory> primaryStorageFactories = Collections.synchronizedMap(new HashMap<>());
    private Map<String, PrimaryStorageAllocatorStrategyFactory> allocatorFactories = Collections.synchronizedMap(new HashMap<>());
    private static final Set<Class> allowedMessageAfterSoftDeletion = new HashSet<>();

    static {
        allowedMessageAfterSoftDeletion.add(PrimaryStorageDeletionMsg.class);
    }

    void init() {
        PrimaryStorageSystemTags.PRIMARY_STORAGE_ALLOCATOR_UUID_TAG.installValidator(new SystemTagValidator() {
            @Override
            public void validateSystemTag(String resourceUuid, Class resourceType, String systemTag) {
                String uuid = PrimaryStorageSystemTags.PRIMARY_STORAGE_ALLOCATOR_UUID_TAG.getTokenByTag(
                        systemTag, PrimaryStorageSystemTags.PRIMARY_STORAGE_ALLOCATOR_UUID_TAG_TOKEN);
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
                    String sql = "select sum(psc.totalCapacity)," +
                            " sum(psc.availableCapacity)," +
                            " sum(psc.totalPhysicalCapacity)," +
                            " sum(psc.availablePhysicalCapacity)" +
                            " from PrimaryStorageCapacityVO psc" +
                            " where psc.uuid in (:psUuids)";
                    TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
                    q.setParameter("psUuids", msg.getPrimaryStorageUuids());
                    return q.getSingleResult();
                } else if (msg.getClusterUuids() != null && !msg.getClusterUuids().isEmpty()) {
                    String sql = "select sum(psc.totalCapacity)," +
                            " sum(psc.availableCapacity)," +
                            " sum(psc.totalPhysicalCapacity)," +
                            " sum(psc.availablePhysicalCapacity)" +
                            " from PrimaryStorageCapacityVO psc, PrimaryStorageClusterRefVO ref" +
                            " where ref.primaryStorageUuid = psc.uuid" +
                            " and ref.clusterUuid in (:clusterUuids)";
                    TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
                    q.setParameter("clusterUuids", msg.getClusterUuids());
                    return q.getSingleResult();
                } else if (msg.getZoneUuids() != null && !msg.getZoneUuids().isEmpty()) {
                    String sql = "select sum(psc.totalCapacity)," +
                            " sum(psc.availableCapacity)," +
                            " sum(psc.totalPhysicalCapacity)," +
                            " sum(psc.availablePhysicalCapacity)" +
                            " from PrimaryStorageCapacityVO psc, PrimaryStorageVO ps" +
                            " where ps.uuid = psc.uuid" +
                            " and ps.zoneUuid in (:zoneUuids)";
                    TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
                    q.setParameter("zoneUuids", msg.getZoneUuids());
                    return q.getSingleResult();
                }

                throw new CloudRuntimeException("should not be here");
            }
        }.call();

        Long total = ret.get(0, Long.class);
        Long avail = ret.get(1, Long.class);
        Long ptotal = ret.get(2, Long.class);
        Long pavail = ret.get(3, Long.class);
        reply.setTotalCapacity(total == null ? 0 : total);
        reply.setAvailableCapacity(avail == null ? 0 : avail);
        reply.setTotalPhysicalCapacity(ptotal == null ? 0 : ptotal);
        reply.setAvailablePhysicalCapacity(pavail == null ? 0 : pavail);
        bus.reply(msg, reply);
    }

    private void handle(APIGetPrimaryStorageAllocatorStrategiesMsg msg) {
        APIGetPrimaryStorageAllocatorStrategiesReply reply = new APIGetPrimaryStorageAllocatorStrategiesReply();
        reply.setPrimaryStorageAllocatorStrategies(PrimaryStorageAllocatorStrategyType.getAllExposedTypeNames());
        bus.reply(msg, reply);
    }

    private void handle(APIGetPrimaryStorageTypesMsg msg) {
        APIGetPrimaryStorageTypesReply reply = new APIGetPrimaryStorageTypesReply();
        List<String> ret = new ArrayList<>();
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

    @Deferred
    private void handle(final APIAddPrimaryStorageMsg msg) {
        PrimaryStorageType type = PrimaryStorageType.valueOf(msg.getType());
        final PrimaryStorageFactory factory = getPrimaryStorageFactory(type);

        PrimaryStorageVO primaryStorageVO = new PrimaryStorageVO();
        if (msg.getResourceUuid() != null) {
            primaryStorageVO.setUuid(msg.getResourceUuid());
        } else {
            primaryStorageVO.setUuid(Platform.getUuid());
        }
        primaryStorageVO.setUrl(msg.getUrl());
        primaryStorageVO.setType(type.toString());
        primaryStorageVO.setName(msg.getName());
        primaryStorageVO.setDescription(msg.getDescription());
        primaryStorageVO.setState(PrimaryStorageState.Enabled);
        primaryStorageVO.setStatus(PrimaryStorageStatus.Connecting);
        primaryStorageVO.setZoneUuid(msg.getZoneUuid());

        final APIAddPrimaryStorageEvent evt = new APIAddPrimaryStorageEvent(msg.getId());
        final PrimaryStorageInventory inv = factory.createPrimaryStorage(primaryStorageVO, msg);
        primaryStorageVO = dbf.findByUuid(primaryStorageVO.getUuid(), PrimaryStorageVO.class);

        tagMgr.createTagsFromAPICreateMessage(msg, inv.getUuid(), PrimaryStorageVO.class.getSimpleName());

        PrimaryStorageCapacityVO primaryStorageCapacityVO = dbf.findByUuid(primaryStorageVO.getUuid(), PrimaryStorageCapacityVO.class);
        if (primaryStorageCapacityVO == null) {
            primaryStorageCapacityVO = new PrimaryStorageCapacityVO();
            primaryStorageCapacityVO.setUuid(primaryStorageVO.getUuid());
            dbf.persist(primaryStorageCapacityVO);
        }

        final ConnectPrimaryStorageMsg cmsg = new ConnectPrimaryStorageMsg();
        cmsg.setPrimaryStorageUuid(inv.getUuid());
        cmsg.setNewAdded(true);
        bus.makeTargetServiceIdByResourceUuid(cmsg, PrimaryStorageConstant.SERVICE_ID, inv.getUuid());
        final PrimaryStorageVO finalVo = primaryStorageVO;
        bus.send(cmsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess() && !reply.getError().isError(PrimaryStorageErrors.DISCONNECTED)) {
                    evt.setErrorCode(reply.getError());
                    logger.warn(String.format("failed to connect primary storage[uuid:%s, name:%s, url:%s]",
                            finalVo.getUuid(), finalVo.getName(), finalVo.getUrl()));
                    dbf.remove(finalVo);
                } else {
                    PrimaryStorageInventory pinv = factory.getInventory(finalVo.getUuid());
                    logger.debug(String.format("successfully add primary storage[uuid:%s, name:%s, url: %s]",
                            finalVo.getUuid(), finalVo.getName(), finalVo.getUrl()));
                    evt.setInventory(pinv);
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
        } else if (msg instanceof RecalculatePrimaryStorageCapacityMsg) {
            handle((RecalculatePrimaryStorageCapacityMsg) msg);
        } else if (msg instanceof PrimaryStorageMessage) {
            passThrough((PrimaryStorageMessage) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(final RecalculatePrimaryStorageCapacityMsg msg) {
        RecalculatePrimaryStorageCapacityReply reply = new RecalculatePrimaryStorageCapacityReply();

        final List<String> psUuids = new ArrayList<>();

        if (msg.getPrimaryStorageUuid() != null) {
            psUuids.add(msg.getPrimaryStorageUuid());
        } else if (msg.getZoneUuid() != null) {
            SimpleQuery<PrimaryStorageVO> q = dbf.createQuery(PrimaryStorageVO.class);
            q.select(PrimaryStorageVO_.uuid);
            q.add(PrimaryStorageVO_.zoneUuid, Op.EQ, msg.getZoneUuid());
            List<String> uuids = q.listValue();
            psUuids.addAll(uuids);
        }


        if (psUuids.isEmpty()) {
            bus.reply(msg, reply);
            return;
        }

        final Map<String, Long> psCap = new HashMap<>();
        new Runnable() {
            @Override
            @Transactional(readOnly = true)
            public void run() {
                // calculate all volume size
                {
                    String sql = "select sum(vol.size), vol.primaryStorageUuid" +
                            " from VolumeVO vol" +
                            " where vol.primaryStorageUuid in (:psUuids)" +
                            " and vol.status = :volStatus" +
                            " group by vol.primaryStorageUuid";
                    TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
                    q.setParameter("psUuids", psUuids);
                    q.setParameter("volStatus", VolumeStatus.Ready);
                    List<Tuple> ts = q.getResultList();

                    for (Tuple t : ts) {
                        if (t.get(0, Long.class) == null) {
                            // no volume
                            continue;
                        }

                        long cap = t.get(0, Long.class);
                        String psUuid = t.get(1, String.class);
                        psCap.put(psUuid, ratioMgr.calculateByRatio(psUuid, cap));
                    }
                }

                // calculate all image cache size
                {
                    String sql = "select sum(i.size), i.primaryStorageUuid" +
                            " from ImageCacheVO i" +
                            " where i.primaryStorageUuid in (:psUuids)" +
                            " group by i.primaryStorageUuid";
                    TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
                    q.setParameter("psUuids", psUuids);
                    List<Tuple> ts = q.getResultList();
                    for (Tuple t : ts) {
                        if (t.get(0, Long.class) == null) {
                            // no image cache
                            continue;
                        }

                        // templates in image cache are physical size
                        // do not calculate over-provisioning
                        long cap = t.get(0, Long.class);
                        String psUuid = t.get(1, String.class);
                        Long ncap = psCap.get(psUuid);
                        ncap = ncap == null ? cap : ncap + cap;
                        psCap.put(psUuid, ncap);
                    }
                }
            }
        }.run();


        if (psCap.isEmpty()) {
            // the primary storage is empty
            for (String psUuid : psUuids) {
                new Runnable() {
                    @Override
                    @Transactional
                    public void run() {
                        String sql = "select ps.type" +
                                " from PrimaryStorageVO ps" +
                                " where ps.uuid = :psUuid";
                        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
                        q.setParameter("psUuid", psUuid);
                        String type = q.getSingleResult();

                        RecalculatePrimaryStorageCapacityExtensionPoint ext = recalculateCapacityExtensions.get(type);
                        RecalculatePrimaryStorageCapacityStruct struct = new RecalculatePrimaryStorageCapacityStruct();
                        struct.setPrimaryStorageUuid(psUuid);

                        if (ext != null) {
                            ext.beforeRecalculatePrimaryStorageCapacity(struct);
                        }

                        PrimaryStorageCapacityUpdater updater = new PrimaryStorageCapacityUpdater(psUuid);
                        updater.run(new PrimaryStorageCapacityUpdaterRunnable() {
                            @Override
                            public PrimaryStorageCapacityVO call(PrimaryStorageCapacityVO cap) {
                                cap.setAvailableCapacity(cap.getAvailablePhysicalCapacity());
                                logger.debug(String.format("re-calculated available capacity of the primary storage" +
                                                "[uuid:%s] with over-provisioning ratio[%s]",
                                        psUuid, ratioMgr.getRatio(psUuid)));
                                return cap;
                            }
                        });

                        if (ext != null) {
                            ext.afterRecalculatePrimaryStorageCapacity(struct);
                        }
                    }
                }.run();
            }
        } else {
            // there are volumes/images on the primary storage, re-calculate the available capacity
            for (final Map.Entry<String, Long> e : psCap.entrySet()) {
                final String psUuid = e.getKey();
                final long used = e.getValue();

                new Runnable() {
                    @Override
                    @Transactional
                    public void run() {
                        String sql = "select ps.type" +
                                " from PrimaryStorageVO ps" +
                                " where ps.uuid = :psUuid";
                        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
                        q.setParameter("psUuid", psUuid);
                        String type = q.getSingleResult();

                        RecalculatePrimaryStorageCapacityExtensionPoint ext = recalculateCapacityExtensions.get(type);
                        RecalculatePrimaryStorageCapacityStruct struct = new RecalculatePrimaryStorageCapacityStruct();
                        struct.setPrimaryStorageUuid(psUuid);

                        if (ext != null) {
                            ext.beforeRecalculatePrimaryStorageCapacity(struct);
                        }

                        PrimaryStorageCapacityUpdater updater = new PrimaryStorageCapacityUpdater(psUuid);
                        updater.run(new PrimaryStorageCapacityUpdaterRunnable() {
                            @Override
                            public PrimaryStorageCapacityVO call(PrimaryStorageCapacityVO cap) {
                                long before = cap.getAvailableCapacity();
                                long now = cap.getTotalCapacity()
                                        - used
                                        - (cap.getSystemUsedCapacity() == null ? 0 : cap.getSystemUsedCapacity());
                                cap.setAvailableCapacity(now);
                                logger.debug(String.format("re-calculated available capacity of the primary storage" +
                                                "[uuid:%s, before:%s, now:%s] with over-provisioning ratio[%s]",
                                        psUuid, before, now, ratioMgr.getRatio(psUuid)));
                                return cap;
                            }
                        });

                        if (ext != null) {
                            ext.afterRecalculatePrimaryStorageCapacity(struct);
                        }
                    }
                }.run();
            }
        }

        bus.reply(msg, reply);
    }

    private void handle(ReturnPrimaryStorageCapacityMsg msg) {
        long diskSize = msg.isNoOverProvisioning() ? msg.getDiskSize() : ratioMgr.calculateByRatio(msg.getPrimaryStorageUuid(), msg.getDiskSize());
        PrimaryStorageCapacityUpdater updater = new PrimaryStorageCapacityUpdater(msg.getPrimaryStorageUuid());
        if (updater.increaseAvailableCapacity(diskSize)) {
            if (logger.isTraceEnabled()) {
                logger.trace(String.format("Successfully return %s bytes to primary storage[uuid:%s]",
                        diskSize, msg.getPrimaryStorageUuid()));
            }
        }
    }

    private void handle(AllocatePrimaryStorageMsg msg) {
        AllocatePrimaryStorageReply reply = new AllocatePrimaryStorageReply(null);
        //
        String allocatorStrategyType = null;
        for (PrimaryStorageAllocatorStrategyExtensionPoint ext : pluginRgty.getExtensionList(PrimaryStorageAllocatorStrategyExtensionPoint.class)) {
            allocatorStrategyType = ext.getPrimaryStorageAllocatorStrategyName(msg);
            if (allocatorStrategyType != null) {
                break;
            }
        }

        if (allocatorStrategyType == null) {
            allocatorStrategyType = msg.getAllocationStrategy() == null ?
                    PrimaryStorageConstant.DEFAULT_PRIMARY_STORAGE_ALLOCATION_STRATEGY_TYPE
                    : msg.getAllocationStrategy();
        }

        if (msg.getExcludeAllocatorStrategies() != null && msg.getExcludeAllocatorStrategies().contains(allocatorStrategyType)) {
            throw new CloudRuntimeException(
                    String.format("%s is set as excluded, there is no available primary storage allocator strategy",
                            allocatorStrategyType));
        }

        PrimaryStorageAllocatorStrategyFactory factory = getPrimaryStorageAllocatorStrategyFactory(
                PrimaryStorageAllocatorStrategyType.valueOf(allocatorStrategyType));
        PrimaryStorageAllocatorStrategy strategy = factory.getPrimaryStorageAllocatorStrategy();
        //
        PrimaryStorageAllocationSpec spec = new PrimaryStorageAllocationSpec();
        spec.setRequiredPrimaryStorageTypes(msg.getRequiredPrimaryStorageTypes());
        spec.setImageUuid(msg.getImageUuid());
        spec.setDiskOfferingUuid(msg.getDiskOfferingUuid());
        spec.setVmInstanceUuid(msg.getVmInstanceUuid());
        spec.setPurpose(msg.getPurpose());
        spec.setSize(msg.getSize());
        spec.setNoOverProvisioning(msg.isNoOverProvisioning());
        spec.setRequiredClusterUuids(msg.getRequiredClusterUuids());
        spec.setRequiredHostUuid(msg.getRequiredHostUuid());
        spec.setRequiredZoneUuid(msg.getRequiredZoneUuid());
        spec.setRequiredPrimaryStorageUuid(msg.getRequiredPrimaryStorageUuid());
        spec.setTags(msg.getTags());
        spec.setAllocationMessage(msg);
        spec.setAvoidPrimaryStorageUuids(msg.getExcludePrimaryStorageUuids());
        List<PrimaryStorageInventory> ret = strategy.allocateAllCandidates(spec);
        //
        Iterator<PrimaryStorageInventory> it = ret.iterator();
        List<String> errs = new ArrayList<>();
        PrimaryStorageInventory target = null;
        while (it.hasNext()) {
            PrimaryStorageInventory psInv = it.next();

            if (!physicalCapacityMgr.checkCapacityByRatio(psInv.getUuid(), psInv.getTotalPhysicalCapacity(), psInv.getAvailablePhysicalCapacity())) {
                errs.add(String.format("primary storage[uuid:%s]'s physical capacity usage has exceeded the threshold[%s]",
                        psInv.getUuid(), physicalCapacityMgr.getRatio(psInv.getUuid())));
                continue;
            }

            long requiredSize = spec.getSize();
            if (!msg.isNoOverProvisioning()) {
                requiredSize = ratioMgr.calculateByRatio(psInv.getUuid(), requiredSize);
            }

            if (reserve(psInv, requiredSize)) {
                target = psInv;
                break;
            } else {
                errs.add(String.format("unable to reserve capacity on the primary storage[uuid:%s], it has no space", psInv.getUuid()));
                logger.debug(String.format("concurrent reservation on the primary storage[uuid:%s], try next one", psInv.getUuid()));
            }
        }

        if (target == null) {
            throw new OperationFailureException(errf.stringToOperationError(
                    String.format("cannot find any qualified primary storage, errors are %s", errs)));
        }

        reply.setPrimaryStorageInventory(target);
        reply.setSize(msg.getSize());
        bus.reply(msg, reply);
    }

    private boolean reserve(final PrimaryStorageInventory inv, final long size) {
        PrimaryStorageCapacityUpdater updater = new PrimaryStorageCapacityUpdater(inv.getUuid());
        return updater.run(new PrimaryStorageCapacityUpdaterRunnable() {
            @Override
            public PrimaryStorageCapacityVO call(PrimaryStorageCapacityVO cap) {
                long avail = cap.getAvailableCapacity() - size;
                if (avail <= 0) {
                    logger.warn(String.format("[Primary Storage Allocation] reserved capacity on primary storage[uuid:%s] failed," +
                            " no available capacity on it", inv.getUuid()));
                    return null;
                }

                long origin = cap.getAvailableCapacity();
                cap.setAvailableCapacity(avail);

                if (logger.isTraceEnabled()) {
                    logger.trace(String.format("[Primary Storage Allocation] reserved %s bytes on primary storage[uuid:%s," +
                            " available before:%s, available now:%s]", size, inv.getUuid(), origin, avail));
                }

                return cap;
            }
        });
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
                    throw new GlobalConfigException(String.format("%s is not a size string;" +
                            " a size string consists of a number ending with suffix B/K/M/G/T or without suffix;" +
                            " for example, 512M, 1G", newValue));
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
                throw new CloudRuntimeException(String.format("duplicate PrimaryStorageAllocatorStrategyFactory[%s, %s] for type[%s]",
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

        for (RecalculatePrimaryStorageCapacityExtensionPoint ext : pluginRgty.getExtensionList(RecalculatePrimaryStorageCapacityExtensionPoint.class)) {
            RecalculatePrimaryStorageCapacityExtensionPoint old = recalculateCapacityExtensions.get(ext.getPrimaryStorageTypeForRecalculateCapacityExtensionPoint());
            if (old != null) {
                throw new CloudRuntimeException(
                        String.format("duplicate RecalculatePrimaryStorageCapacityExtensionPoint[%s, %s] for type[%s]",
                                ext.getClass().getName(), old.getClass().getName(),
                                old.getPrimaryStorageTypeForRecalculateCapacityExtensionPoint()));
            }
            recalculateCapacityExtensions.put(ext.getPrimaryStorageTypeForRecalculateCapacityExtensionPoint(), ext);
        }
    }


    private PrimaryStorageAllocatorStrategyFactory getPrimaryStorageAllocatorStrategyFactory(PrimaryStorageAllocatorStrategyType type) {
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

    @Override
    public void nodeJoin(String nodeId) {
        logger.debug(String.format("management node[uuid:%s] join, node[uuid:%s] starts taking over primary storage...",
                nodeId, Platform.getManagementServerId()));
        loadPrimaryStorage();
    }

    @Override
    public void nodeLeft(String nodeId) {

    }

    private List<String> getPrimaryStorageManagedByUs() {
        List<String> ret = new ArrayList<>();
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
    public void iJoin(String nodeId) {
    }

    @AsyncThread
    @Override
    public void managementNodeReady() {
        logger.debug(String.format("management node[uuid:%s] joins, starts load primary storage ...",
                Platform.getManagementServerId()));
        loadPrimaryStorage();
    }
}
