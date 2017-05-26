package org.zstack.storage.snapshot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.cloudbus.ReplyMessagePreSendingExtensionPoint;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SQLBatchWithReturn;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.scheduler.SchedulerFacade;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.AbstractService;
import org.zstack.header.core.scheduler.SchedulerInventory;
import org.zstack.header.core.scheduler.SchedulerVO;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.identity.*;
import org.zstack.header.message.*;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.primary.VolumeSnapshotCapability.VolumeSnapshotArrangementType;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.vm.AfterReimageVmInstanceExtensionPoint;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.volume.*;
import org.zstack.identity.AccountManager;
import org.zstack.identity.QuotaUtil;
import org.zstack.storage.primary.PrimaryStorageCapacityUpdater;
import org.zstack.storage.volume.FireSnapShotCanonicalEvent;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.ExceptionDSL;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.logging.CLogger;

import static org.zstack.core.Platform.operr;

import javax.persistence.Query;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.*;

import static org.zstack.utils.CollectionDSL.list;

/**
 */
public class VolumeSnapshotManagerImpl extends AbstractService implements
        VolumeSnapshotManager,
        ReplyMessagePreSendingExtensionPoint,
        VolumeBeforeExpungeExtensionPoint,
        ResourceOwnerAfterChangeExtensionPoint,
        ReportQuotaExtensionPoint,
        AfterReimageVmInstanceExtensionPoint {
    private static final CLogger logger = Utils.getLogger(VolumeSnapshotManagerImpl.class);
    private String syncSignature;
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private SchedulerFacade schedulerFacade;

    private void passThrough(VolumeSnapshotMessage msg) {
        VolumeSnapshotVO vo = dbf.findByUuid(msg.getSnapshotUuid(), VolumeSnapshotVO.class);
        if (vo == null) {
            throw new OperationFailureException(errf.instantiateErrorCode(SysErrors.RESOURCE_NOT_FOUND,
                    String.format("cannot find volume snapshot[uuid:%s]", msg.getSnapshotUuid())
            ));
        }

        if (msg.getVolumeUuid() != null) {
            VolumeSnapshotTreeBase tree = new VolumeSnapshotTreeBase(vo, true);
            tree.handleMessage((Message) msg);
        } else if (msg.getTreeUuid() != null) {
            VolumeSnapshotTreeBase tree = new VolumeSnapshotTreeBase(vo, false);
            tree.handleMessage((Message) msg);
        } else {
            VolumeSnapshot snapshot = new VolumeSnapshotBase(vo);
            snapshot.handleMessage((Message) msg);
        }
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof VolumeSnapshotMessage) {
            passThrough((VolumeSnapshotMessage) msg);
        } else if (msg instanceof APIMessage) {
            handleApiMessage((APIMessage) msg);
        } else {
            handleLocalMessage(msg);
        }
    }

    private void handleLocalMessage(Message msg) {
        if (msg instanceof CreateVolumeSnapshotMsg) {
            handle((CreateVolumeSnapshotMsg) msg);
        } else if (msg instanceof VolumeSnapshotReportPrimaryStorageCapacityUsageMsg) {
            handle((VolumeSnapshotReportPrimaryStorageCapacityUsageMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    @Transactional(readOnly = true)
    private void handle(VolumeSnapshotReportPrimaryStorageCapacityUsageMsg msg) {
        String sql = "select sum(sp.size)" +
                " from VolumeSnapshotVO sp" +
                " where sp.type = :sptype" +
                " and sp.primaryStorageUuid = :prUuid";
        TypedQuery<Long> q = dbf.getEntityManager().createQuery(sql, Long.class);
        q.setParameter("sptype", VolumeSnapshotConstant.HYPERVISOR_SNAPSHOT_TYPE.toString());
        q.setParameter("prUuid", msg.getPrimaryStorageUuid());
        Long size = q.getSingleResult();

        VolumeSnapshotReportPrimaryStorageCapacityUsageReply reply = new VolumeSnapshotReportPrimaryStorageCapacityUsageReply();
        reply.setUsedSize(size == null ? 0 : size);
        bus.reply(msg, reply);
    }


    private void handle(APIGetVolumeSnapshotTreeMsg msg) {
        APIGetVolumeSnapshotTreeReply reply = new APIGetVolumeSnapshotTreeReply();
        if (msg.getTreeUuid() != null) {
            VolumeSnapshotTreeVO treeVO = dbf.findByUuid(msg.getTreeUuid(), VolumeSnapshotTreeVO.class);
            if (treeVO == null) {
                reply.setInventories(new ArrayList<>());
                bus.reply(msg, reply);
                return;
            }

            VolumeSnapshotTreeInventory inv = VolumeSnapshotTreeInventory.valueOf(treeVO);
            SimpleQuery<VolumeSnapshotVO> q = dbf.createQuery(VolumeSnapshotVO.class);
            q.add(VolumeSnapshotVO_.treeUuid, Op.EQ, msg.getTreeUuid());
            List<VolumeSnapshotVO> vos = q.list();
            VolumeSnapshotTree tree = VolumeSnapshotTree.fromVOs(vos);
            inv.setTree(tree.getRoot().toLeafInventory());
            reply.setInventories(Arrays.asList(inv));
        } else if (msg.getVolumeUuid() != null) {
            SimpleQuery<VolumeSnapshotTreeVO> q = dbf.createQuery(VolumeSnapshotTreeVO.class);
            q.add(VolumeSnapshotTreeVO_.volumeUuid, Op.EQ, msg.getVolumeUuid());
            List<VolumeSnapshotTreeVO> trees = q.list();
            if (trees.isEmpty()) {
                reply.setInventories(new ArrayList<>());
                bus.reply(msg, reply);
                return;
            }

            List<VolumeSnapshotTreeInventory> treeInventories = new ArrayList<>();
            for (VolumeSnapshotTreeVO vo : trees) {
                VolumeSnapshotTreeInventory inv = VolumeSnapshotTreeInventory.valueOf(vo);
                SimpleQuery<VolumeSnapshotVO> sq = dbf.createQuery(VolumeSnapshotVO.class);
                sq.add(VolumeSnapshotVO_.treeUuid, Op.EQ, vo.getUuid());
                List<VolumeSnapshotVO> vos = sq.list();
                VolumeSnapshotTree tree = VolumeSnapshotTree.fromVOs(vos);
                inv.setTree(tree.getRoot().toLeafInventory());
                treeInventories.add(inv);
            }

            reply.setInventories(treeInventories);
        }

        bus.reply(msg, reply);
    }


    @Transactional
    private VolumeSnapshotStruct newChain(VolumeSnapshotVO vo, boolean fullsnapshot) {
        VolumeSnapshotTreeVO chain = new VolumeSnapshotTreeVO();
        chain.setCurrent(true);
        chain.setVolumeUuid(vo.getVolumeUuid());
        chain.setUuid(Platform.getUuid());
        chain = dbf.getEntityManager().merge(chain);

        logger.debug(String.format("created new volume snapshot tree[tree uuid:%s, volume uuid:%s, full snapshot uuid:%s]",
                chain.getUuid(), vo.getVolumeUuid(), vo.getUuid()));

        vo.setTreeUuid(chain.getUuid());
        vo.setDistance(0);
        vo.setParentUuid(null);
        vo.setLatest(true);
        vo.setFullSnapshot(fullsnapshot);
        vo = dbf.getEntityManager().merge(vo);

        VolumeSnapshotStruct struct = new VolumeSnapshotStruct();
        struct.setCurrent(VolumeSnapshotInventory.valueOf(vo));
        struct.setFullSnapshot(fullsnapshot);
        return struct;
    }

    @Transactional
    private VolumeSnapshotStruct saveChainTypeSnapshot(VolumeSnapshotVO vo) {
        String sql = "select c" +
                " from VolumeSnapshotTreeVO c" +
                " where c.volumeUuid = :volUuid" +
                " and c.current = true";
        TypedQuery<VolumeSnapshotTreeVO> cq = dbf.getEntityManager().createQuery(sql, VolumeSnapshotTreeVO.class);
        cq.setParameter("volUuid", vo.getVolumeUuid());
        List<VolumeSnapshotTreeVO> rets = cq.getResultList();
        DebugUtils.Assert(rets.size() < 2, "can not have more than one VolumeSnapshotTreeVO with current=1");
        VolumeSnapshotTreeVO chain = rets.isEmpty() ? null : rets.get(0);
        if (chain == null) {
            return newChain(vo, false);
        } else {
            sql = "select s" +
                    " from VolumeSnapshotVO s" +
                    " where s.latest = true" +
                    " and s.volumeUuid = :volUuid" +
                    " and s.treeUuid = :chainUuid";
            TypedQuery<VolumeSnapshotVO> q = dbf.getEntityManager().createQuery(sql, VolumeSnapshotVO.class);
            q.setParameter("volUuid", vo.getVolumeUuid());
            q.setParameter("chainUuid", chain.getUuid());
            VolumeSnapshotVO latest = q.getSingleResult();

            if (VolumeSnapshotGlobalConfig.MAX_INCREMENTAL_SNAPSHOT_NUM.value(Integer.class) == latest.getDistance()) {
                chain.setCurrent(false);
                dbf.getEntityManager().merge(chain);
                return newChain(vo, true);
            }

            latest.setLatest(false);
            latest = dbf.getEntityManager().merge(latest);

            vo.setTreeUuid(latest.getTreeUuid());
            vo.setLatest(true);
            vo.setParentUuid(latest.getUuid());
            vo.setDistance(latest.getDistance() + 1);
            vo = dbf.getEntityManager().merge(vo);

            VolumeSnapshotStruct struct = new VolumeSnapshotStruct();
            struct.setParent(VolumeSnapshotInventory.valueOf(latest));
            struct.setCurrent(VolumeSnapshotInventory.valueOf(vo));
            return struct;
        }
    }

    @Transactional
    private VolumeSnapshotStruct saveIndividualTypeSnapshot(VolumeSnapshotVO vo) {
        String sql = "update VolumeSnapshotTreeVO tree" +
                " set tree.current = false" +
                " where tree.current = true" +
                " and tree.volumeUuid = :volUuid";
        Query q = dbf.getEntityManager().createQuery(sql);
        q.setParameter("volUuid", vo.getVolumeUuid());
        q.executeUpdate();

        return newChain(vo, false);
    }

    @Transactional
    private void rollbackSnapshot(String uuid) {
        VolumeSnapshotVO vo = dbf.getEntityManager().find(VolumeSnapshotVO.class, uuid);
        dbf.getEntityManager().remove(vo);

        String sql = "delete from AccountResourceRefVO where resourceUuid = :vsUuid and resourceType = 'VolumeSnapshotVO'";
        Query q = dbf.getEntityManager().createQuery(sql);
        q.setParameter("vsUuid", uuid);
        q.executeUpdate();

        if (vo.getParentUuid() != null) {
            VolumeSnapshotVO parent = dbf.getEntityManager().find(VolumeSnapshotVO.class, vo.getParentUuid());
            parent.setLatest(true);
            dbf.getEntityManager().merge(parent);
        } else {
            VolumeSnapshotTreeVO chain = dbf.getEntityManager().find(VolumeSnapshotTreeVO.class, vo.getTreeUuid());
            dbf.getEntityManager().remove(chain);
        }

    }


    private void handle(final CreateVolumeSnapshotMsg msg) {
        final CreateVolumeSnapshotReply ret = new CreateVolumeSnapshotReply();

        final VolumeVO vol = dbf.findByUuid(msg.getVolumeUuid(), VolumeVO.class);
        final String primaryStorageUuid = vol.getPrimaryStorageUuid();

        AskVolumeSnapshotCapabilityMsg askMsg = new AskVolumeSnapshotCapabilityMsg();
        askMsg.setPrimaryStorageUuid(primaryStorageUuid);
        askMsg.setVolume(VolumeInventory.valueOf(vol));
        bus.makeTargetServiceIdByResourceUuid(askMsg, PrimaryStorageConstant.SERVICE_ID, primaryStorageUuid);
        MessageReply reply = bus.call(askMsg);
        if (!reply.isSuccess()) {
            ret.setError(errf.stringToOperationError(
                    String.format("cannot ask primary storage[uuid:%s] for volume snapshot capability",
                            vol.getUuid()), reply.getError()));
            bus.reply(msg, ret);
            return;
        }

        AskVolumeSnapshotCapabilityReply areply = reply.castReply();
        VolumeSnapshotCapability capability = areply.getCapability();
        if (!capability.isSupport()) {
            ret.setError(operr("primary storage[uuid:%s] doesn't support volume snapshot;" +
                            " cannot create snapshot for volume[uuid:%s]", primaryStorageUuid, vol.getUuid()));
            bus.reply(msg, ret);
            return;
        }

        final VolumeSnapshotVO vo = new VolumeSnapshotVO();
        if (msg.getResourceUuid() != null) {
            vo.setUuid(msg.getResourceUuid());
        } else {
            vo.setUuid(Platform.getUuid());
        }
        vo.setName(msg.getName());
        vo.setDescription(msg.getDescription());
        vo.setVolumeUuid(msg.getVolumeUuid());
        vo.setFormat(vol.getFormat());
        vo.setState(VolumeSnapshotState.Enabled);
        vo.setStatus(VolumeSnapshotStatus.Creating);
        vo.setVolumeType(vol.getType().toString());

        final VolumeSnapshotStruct struct = new SQLBatchWithReturn<VolumeSnapshotStruct>() {
            @Override
            protected VolumeSnapshotStruct scripts() {
                VolumeSnapshotStruct s = null;
                if (VolumeSnapshotArrangementType.CHAIN == capability.getArrangementType()) {
                    s = saveChainTypeSnapshot(vo);
                } else if (VolumeSnapshotArrangementType.INDIVIDUAL == capability.getArrangementType()) {
                    s = saveIndividualTypeSnapshot(vo);
                } else {
                    DebugUtils.Assert(false, "should not be here");
                }

                acntMgr.createAccountResourceRef(msg.getAccountUuid(), vo.getUuid(), VolumeSnapshotVO.class);
                return s;
            }
        }.execute();

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("take-volume-snapshot-for-volume-%s", msg.getVolumeUuid()));
        chain.then(new ShareFlow() {
            VolumeSnapshotInventory snapshot;
            String volumeNewInstallPath;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "take-volume-snapshot";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        final TakeSnapshotMsg tmsg = new TakeSnapshotMsg();
                        tmsg.setPrimaryStorageUuid(primaryStorageUuid);
                        tmsg.setStruct(struct);
                        bus.makeTargetServiceIdByResourceUuid(tmsg, PrimaryStorageConstant.SERVICE_ID, primaryStorageUuid);
                        bus.send(tmsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.fail(reply.getError());
                                    return;
                                }

                                TakeSnapshotReply treply = (TakeSnapshotReply) reply;
                                volumeNewInstallPath = treply.getNewVolumeInstallPath();
                                snapshot = treply.getInventory();
                                trigger.next();
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "reserve-snapshot-size-on-primary-storage";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        ExceptionDSL.exceptionSafe(new Runnable() {
                            @Override
                            public void run() {
                                // TODO
                                PrimaryStorageCapacityUpdater updater =
                                        new PrimaryStorageCapacityUpdater(vol.getPrimaryStorageUuid());
                                updater.reserve(snapshot.getSize());
                            }
                        });

                        trigger.next();
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        if (volumeNewInstallPath != null) {
                            vol.setInstallPath(volumeNewInstallPath);
                            dbf.update(vol);
                        }

                        VolumeSnapshotVO svo = dbf.findByUuid(snapshot.getUuid(), VolumeSnapshotVO.class);
                        svo.setType(snapshot.getType());
                        svo.setPrimaryStorageUuid(snapshot.getPrimaryStorageUuid());
                        svo.setPrimaryStorageInstallPath(snapshot.getPrimaryStorageInstallPath());
                        svo.setStatus(VolumeSnapshotStatus.Ready);
                        svo.setSize(snapshot.getSize());
                        if (snapshot.getFormat() != null) {
                            svo.setFormat(snapshot.getFormat());
                        }
                        svo = dbf.updateAndRefresh(svo);
                        new FireSnapShotCanonicalEvent().
                                fireSnapShotStatusChangedEvent(
                                        VolumeSnapshotStatus.valueOf(snapshot.getStatus()),
                                        VolumeSnapshotInventory.valueOf(svo))
                        ;
                        ret.setInventory(VolumeSnapshotInventory.valueOf(svo));
                        bus.reply(msg, ret);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        rollbackSnapshot(struct.getCurrent().getUuid());
                        ret.setError(errCode);
                        bus.reply(msg, ret);
                    }
                });
            }
        }).start();
    }

    private void handle(APICreateVolumeSnapshotSchedulerMsg msg) {
        APICreateVolumeSnapshotSchedulerEvent evt = new APICreateVolumeSnapshotSchedulerEvent(msg.getId());
        CreateVolumeSnapshotJob job = new CreateVolumeSnapshotJob(msg);
        job.setVolumeUuid(msg.getVolumeUuid());
        job.setTargetResourceUuid(msg.getVolumeUuid());
        job.setSnapShotName(msg.getSnapShotName());
        job.setSnapShotDescription(msg.getVolumeSnapshotDescription());
        SchedulerVO schedulerVO = schedulerFacade.runScheduler(job);
        acntMgr.createAccountResourceRef(msg.getSession().getAccountUuid(), schedulerVO.getUuid(), SchedulerVO.class);
        if (schedulerVO != null) {
            schedulerVO = dbf.reload(schedulerVO);
            SchedulerInventory sinv = SchedulerInventory.valueOf(schedulerVO);
            evt.setInventory(sinv);
        }
        bus.publish(evt);
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIGetVolumeSnapshotTreeMsg) {
            handle((APIGetVolumeSnapshotTreeMsg) msg);
        } else if (msg instanceof APICreateVolumeSnapshotSchedulerMsg) {
            handle((APICreateVolumeSnapshotSchedulerMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(VolumeSnapshotConstant.SERVICE_ID);
    }

    @Override
    public boolean start() {
        pluginRgty.saveExtensionAsMap(CreateTemplateFromVolumeSnapshotExtensionPoint.class,
                new Function<Object, CreateTemplateFromVolumeSnapshotExtensionPoint>() {
                    @Override
                    public Object call(CreateTemplateFromVolumeSnapshotExtensionPoint arg) {
                        return arg.createTemplateFromVolumeSnapshotPrimaryStorageType();
                    }
                });

        bus.installBeforeDeliveryMessageInterceptor(new AbstractBeforeDeliveryMessageInterceptor() {
            @Override
            public void intercept(Message msg) {
                if (msg instanceof NeedQuotaCheckMessage) {
                    if (((NeedQuotaCheckMessage) msg).getAccountUuid() == null ||
                            ((NeedQuotaCheckMessage) msg).getAccountUuid().equals("")) {
                        // skip admin scheduler
                        return;
                    }
                    List<Quota> quotas = acntMgr.getMessageQuotaMap().get(msg.getClass());
                    if (quotas == null || quotas.size() == 0) {
                        return;
                    }
                    Map<String, Quota.QuotaPair> pairs = new QuotaUtil().
                            makeQuotaPairs(((NeedQuotaCheckMessage) msg).getAccountUuid());
                    for (Quota quota : quotas) {
                        quota.getOperator().checkQuota((NeedQuotaCheckMessage) msg, pairs);
                    }
                }
            }
        }, VolumeCreateSnapshotMsg.class);
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public List<Class> getReplyMessageClassForPreSendingExtensionPoint() {
        List<Class> ret = new ArrayList<>();
        ret.add(APIQueryVolumeSnapshotTreeReply.class);
        return ret;
    }

    @Override
    public void marshalReplyMessageBeforeSending(Message msg) {
        if (msg instanceof APIQueryVolumeSnapshotTreeReply) {
            marshal((APIQueryVolumeSnapshotTreeReply) msg);
        }
    }

    private void marshal(APIQueryVolumeSnapshotTreeReply reply) {
        if (reply.getInventories() == null) {
            // this is for count
            return;
        }

        for (VolumeSnapshotTreeInventory inv : reply.getInventories()) {
            SimpleQuery<VolumeSnapshotVO> sq = dbf.createQuery(VolumeSnapshotVO.class);
            sq.add(VolumeSnapshotVO_.treeUuid, Op.EQ, inv.getUuid());
            List<VolumeSnapshotVO> vos = sq.list();
            VolumeSnapshotTree tree = VolumeSnapshotTree.fromVOs(vos);
            inv.setTree(tree.getRoot().toLeafInventory());
        }
    }

    @Override
    public void volumeBeforeExpunge(VolumeInventory volume) {
        List<VolumeSnapshotDeletionMsg> msgs = new ArrayList<>();
        SimpleQuery<VolumeSnapshotTreeVO> cq = dbf.createQuery(VolumeSnapshotTreeVO.class);
        cq.select(VolumeSnapshotTreeVO_.uuid);
        cq.add(VolumeSnapshotTreeVO_.volumeUuid, Op.EQ, volume.getUuid());
        List<String> cuuids = cq.listValue();

        for (String cuuid : cuuids) {
            // deleting full snapshot of chain will cause whole chain to be deleted
            SimpleQuery<VolumeSnapshotVO> q = dbf.createQuery(VolumeSnapshotVO.class);
            q.select(VolumeSnapshotVO_.uuid);
            q.add(VolumeSnapshotVO_.treeUuid, Op.EQ, cuuid);
            q.add(VolumeSnapshotVO_.parentUuid, Op.NULL);
            String suuid = q.findValue();

            if (suuid == null) {
                // this is a storage snapshot, don't delete it on primary storage
                continue;
            }

            SimpleQuery<VolumeSnapshotVO> sq = dbf.createQuery(VolumeSnapshotVO.class);
            sq.select(VolumeSnapshotVO_.volumeUuid, VolumeSnapshotVO_.treeUuid);
            sq.add(VolumeSnapshotVO_.uuid, Op.EQ, suuid);
            Tuple t = sq.findTuple();
            String volumeUuid = t.get(0, String.class);
            String treeUuid = t.get(1, String.class);

            VolumeSnapshotDeletionMsg msg = new VolumeSnapshotDeletionMsg();
            msg.setSnapshotUuid(suuid);
            msg.setTreeUuid(treeUuid);
            msg.setVolumeUuid(volumeUuid);
            msg.setVolumeDeletion(true);
            String resourceUuid = volumeUuid != null ? volumeUuid : treeUuid;
            bus.makeTargetServiceIdByResourceUuid(msg, VolumeSnapshotConstant.SERVICE_ID, resourceUuid);

            msgs.add(msg);
        }

        if (!msgs.isEmpty()) {
            bus.call(msgs);
        }
    }

    @Override
    public void resourceOwnerAfterChange(AccountResourceRefInventory ref, String newOwnerUuid) {
        if (!VolumeVO.class.getSimpleName().equals(ref.getResourceType())) {
            return;
        }

        changeVolumeSnapshotOwner(ref, newOwnerUuid);
    }

    private void changeVolumeSnapshotOwner(AccountResourceRefInventory ref, String newOwnerUuid) {
        SimpleQuery<VolumeSnapshotVO> q = dbf.createQuery(VolumeSnapshotVO.class);
        q.select(VolumeSnapshotVO_.uuid);
        q.add(VolumeSnapshotVO_.volumeUuid, Op.EQ, ref.getResourceUuid());
        List<String> spUuids = q.listValue();

        for (String spUuid : spUuids) {
            acntMgr.changeResourceOwner(spUuid, newOwnerUuid);
        }
    }


    @Override
    public List<Quota> reportQuota() {
        Quota.QuotaOperator checker = new Quota.QuotaOperator() {
            @Override
            public void checkQuota(APIMessage msg, Map<String, Quota.QuotaPair> pairs) {
                if (!new QuotaUtil().isAdminAccount(msg.getSession().getAccountUuid())) {
                    if (msg instanceof APICreateVolumeSnapshotMsg) {
                        check((APICreateVolumeSnapshotMsg) msg, pairs);
                    } else if (msg instanceof APIChangeResourceOwnerMsg) {
                        check((APIChangeResourceOwnerMsg) msg, pairs);
                    }
                } else {
                    if (msg instanceof APIChangeResourceOwnerMsg) {
                        check((APIChangeResourceOwnerMsg) msg, pairs);
                    }
                }
            }

            @Override
            public void checkQuota(NeedQuotaCheckMessage msg, Map<String, Quota.QuotaPair> pairs) {
                if (!new QuotaUtil().isAdminAccount(msg.getAccountUuid())) {
                    if (msg instanceof VolumeCreateSnapshotMsg) {
                        check((VolumeCreateSnapshotMsg) msg, pairs);
                    }
                }
            }

            @Override
            public List<Quota.QuotaUsage> getQuotaUsageByAccount(String accountUuid) {
                List<Quota.QuotaUsage> usages = new ArrayList<>();

                Quota.QuotaUsage usage;

                usage = new Quota.QuotaUsage();
                usage.setName(VolumeSnapshotConstant.QUOTA_VOLUME_SNAPSHOT_NUM);
                usage.setUsed(getUsedVolumeSnapshotNum(accountUuid));
                usages.add(usage);

                return usages;
            }

            private long getUsedVolumeSnapshotNum(String accountUuid) {
                SimpleQuery<AccountResourceRefVO> queryVolumeSnapshotNum = dbf.createQuery(AccountResourceRefVO.class);
                queryVolumeSnapshotNum.add(AccountResourceRefVO_.accountUuid, Op.EQ, accountUuid);
                queryVolumeSnapshotNum.add(AccountResourceRefVO_.resourceType, Op.EQ, VolumeSnapshotVO.class.getSimpleName());
                return queryVolumeSnapshotNum.count();
            }

            @Transactional(readOnly = true)
            private void check(APIChangeResourceOwnerMsg msg, Map<String, Quota.QuotaPair> pairs) {
                String currentAccountUuid = msg.getSession().getAccountUuid();
                String resourceTargetOwnerAccountUuid = msg.getAccountUuid();
                if (new QuotaUtil().isAdminAccount(resourceTargetOwnerAccountUuid)) {
                    return;
                }

                String resourceType = new QuotaUtil().getResourceType(msg.getResourceUuid());
                long volumeSnapshotNumAsked;
                if (resourceType.equals(VmInstanceVO.class.getSimpleName())) {
                    String sql = "select count(s)" +
                            " from VolumeVO v, VolumeSnapshotVO s" +
                            " where s.volumeUuid = v.uuid" +
                            " and v.vmInstanceUuid = :vmInstanceUuid";
                    TypedQuery<Long> q = dbf.getEntityManager().createQuery(sql, Long.class);
                    q.setParameter("vmInstanceUuid", msg.getResourceUuid());
                    volumeSnapshotNumAsked = q.getSingleResult();
                } else if (resourceType.equals(VolumeVO.class.getSimpleName())) {
                    String sql = "select count(s)" +
                            " from VolumeSnapshotVO s" +
                            " where s.volumeUuid = :volumeUuid";
                    TypedQuery<Long> q = dbf.getEntityManager().createQuery(sql, Long.class);
                    q.setParameter("volumeUuid", msg.getResourceUuid());
                    volumeSnapshotNumAsked = q.getSingleResult();
                } else if (resourceType.equals(VolumeSnapshotVO.class.getSimpleName())) {
                    volumeSnapshotNumAsked = 1;
                } else {
                    return;
                }
                checkVolumeSnapshotNumQuota(currentAccountUuid,
                        resourceTargetOwnerAccountUuid,
                        volumeSnapshotNumAsked,
                        pairs);
            }

            private void check(VolumeCreateSnapshotMsg msg, Map<String, Quota.QuotaPair> pairs) {
                checkVolumeSnapshotNumQuota(msg.getAccountUuid(), msg.getAccountUuid(), 1, pairs);
            }

            private void check(APICreateVolumeSnapshotMsg msg, Map<String, Quota.QuotaPair> pairs) {
                String resourceTargetOwnerUuid = new QuotaUtil().getResourceOwnerAccountUuid(msg.getVolumeUuid());
                checkVolumeSnapshotNumQuota(msg.getSession().getAccountUuid(), resourceTargetOwnerUuid, 1, pairs);
            }

            private void checkVolumeSnapshotNumQuota(String currentAccountUuid,
                                                     String resourceTargetOwnerAccountUuid,
                                                     long volumeSnapshotNumAsked,
                                                     Map<String, Quota.QuotaPair> pairs) {
                long volumeSnapshotNumQuota = pairs.get(VolumeSnapshotConstant.QUOTA_VOLUME_SNAPSHOT_NUM).getValue();
                long volumeSnapshotNumUsed = getUsedVolumeSnapshotNum(resourceTargetOwnerAccountUuid);
                {
                    QuotaUtil.QuotaCompareInfo quotaCompareInfo;
                    quotaCompareInfo = new QuotaUtil.QuotaCompareInfo();
                    quotaCompareInfo.currentAccountUuid = currentAccountUuid;
                    quotaCompareInfo.resourceTargetOwnerAccountUuid = resourceTargetOwnerAccountUuid;
                    quotaCompareInfo.quotaName = VolumeSnapshotConstant.QUOTA_VOLUME_SNAPSHOT_NUM;
                    quotaCompareInfo.quotaValue = volumeSnapshotNumQuota;
                    quotaCompareInfo.currentUsed = volumeSnapshotNumUsed;
                    quotaCompareInfo.request = volumeSnapshotNumAsked;
                    new QuotaUtil().CheckQuota(quotaCompareInfo);
                }
            }
        };


        Quota quota = new Quota();
        Quota.QuotaPair p;

        p = new Quota.QuotaPair();
        p.setName(VolumeSnapshotConstant.QUOTA_VOLUME_SNAPSHOT_NUM);
        p.setValue(200);
        quota.addPair(p);

        quota.addMessageNeedValidation(APICreateVolumeSnapshotMsg.class);
        quota.addMessageNeedValidation(APIChangeResourceOwnerMsg.class);
        quota.addMessageNeedValidation(VolumeCreateSnapshotMsg.class);
        quota.setOperator(checker);

        return list(quota);
    }

    @Transactional
    @Override
    public void afterReimageVmInstance(VolumeInventory inventory) {
        String rootVolumeUuid = inventory.getUuid();

        String sql = "update VolumeSnapshotVO s" +
                " set s.latest = false" +
                " where s.latest = true" +
                " and s.volumeUuid = :volumeUuid";
        Query q = dbf.getEntityManager().createQuery(sql);
        q.setParameter("volumeUuid", rootVolumeUuid);
        q.executeUpdate();

        sql = "update VolumeSnapshotTreeVO tree" +
                " set tree.current = false" +
                " where tree.current = true" +
                " and tree.volumeUuid = :volUuid";
        q = dbf.getEntityManager().createQuery(sql);
        q.setParameter("volUuid", rootVolumeUuid);
        q.executeUpdate();
    }
}
