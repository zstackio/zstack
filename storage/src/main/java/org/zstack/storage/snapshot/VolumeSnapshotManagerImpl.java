package org.zstack.storage.snapshot;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.cascade.CascadeConstant;
import org.zstack.core.cascade.CascadeFacade;
import org.zstack.core.cloudbus.*;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.*;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.AbstractService;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ExceptionSafe;
import org.zstack.header.core.NopeCompletion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.identity.*;
import org.zstack.header.message.*;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.primary.VolumeSnapshotCapability.VolumeSnapshotArrangementType;
import org.zstack.header.storage.snapshot.*;
import org.zstack.header.storage.snapshot.group.*;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.header.vm.*;
import org.zstack.header.vm.devices.VmInstanceDeviceAddressArchiveVO;
import org.zstack.header.vm.devices.VmInstanceDeviceAddressArchiveVO_;
import org.zstack.header.vm.devices.VmInstanceDeviceAddressGroupVO;
import org.zstack.header.vm.devices.VmInstanceDeviceAddressGroupVO_;
import org.zstack.header.volume.*;
import org.zstack.identity.AccountManager;
import org.zstack.identity.QuotaUtil;
import org.zstack.storage.primary.PrimaryStorageCapacityChecker;
import org.zstack.storage.primary.PrimaryStorageGlobalConfig;
import org.zstack.storage.snapshot.group.VolumeSnapshotGroupBase;
import org.zstack.storage.snapshot.group.VolumeSnapshotGroupChecker;
import org.zstack.storage.volume.FireSnapShotCanonicalEvent;
import org.zstack.storage.volume.VolumeSystemTags;
import org.zstack.tag.TagManager;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.zql.ZQL;

import javax.persistence.Query;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;
import static org.zstack.core.progress.ProgressReportService.reportProgress;
import static org.zstack.storage.snapshot.VolumeSnapshotTagHelper.getBackingVolumeTag;
import static org.zstack.utils.CollectionDSL.list;

/**
 */
public class VolumeSnapshotManagerImpl extends AbstractService implements
        VolumeSnapshotManager,
        MarshalReplyMessageExtensionPoint,
        VolumeBeforeExpungeExtensionPoint,
        ResourceOwnerAfterChangeExtensionPoint,
        ReportQuotaExtensionPoint,
        AfterReimageVmInstanceExtensionPoint,
        VmJustBeforeDeleteFromDbExtensionPoint,
        VolumeJustBeforeDeleteFromDbExtensionPoint,
        OverwriteVolumeExtensionPoint {
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
    private TagManager tagMgr;
    @Autowired
    private EventFacade evtf;
    @Autowired
    private CascadeFacade casf;

    private void passThrough(VolumeSnapshotMessage msg) {
        VolumeSnapshotVO vo = dbf.findByUuid(msg.getSnapshotUuid(), VolumeSnapshotVO.class);
        if (vo == null) {
            throw new OperationFailureException(err(SysErrors.RESOURCE_NOT_FOUND,
                    "cannot find volume snapshot[uuid:%s]", msg.getSnapshotUuid()
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

    private void passThrough(VolumeSnapshotGroupMessage msg) {
        VolumeSnapshotGroupVO vo = dbf.findByUuid(msg.getGroupUuid(), VolumeSnapshotGroupVO.class);
        if (vo == null) {
            throw new OperationFailureException(err(SysErrors.RESOURCE_NOT_FOUND,
                    "cannot find volume snapshot[uuid:%s]", msg.getGroupUuid()
            ));
        }

        VolumeSnapshotGroupBase group = new VolumeSnapshotGroupBase(vo);
        group.handleMessage((Message) msg);
    }

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof VolumeSnapshotMessage) {
            passThrough((VolumeSnapshotMessage) msg);
        } else if (msg instanceof VolumeSnapshotGroupMessage) {
            handleSnapshotGroup((VolumeSnapshotGroupMessage) msg);
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
        } else if (msg instanceof MarkRootVolumeAsSnapshotMsg) {
            handle((MarkRootVolumeAsSnapshotMsg) msg);
        } else if (msg instanceof AskVolumeSnapshotStructMsg) {
            handle((AskVolumeSnapshotStructMsg) msg);
        } else if (msg instanceof GetVolumeSnapshotTreeRootNodeMsg) {
            handle((GetVolumeSnapshotTreeRootNodeMsg) msg);
        } else if (msg instanceof GetVolumeSnapshotEncryptedMsg) {
            handle((GetVolumeSnapshotEncryptedMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(GetVolumeSnapshotEncryptedMsg msg) {
        GetVolumeSnapshotEncryptedReply snapshotEncryptedReply = new GetVolumeSnapshotEncryptedReply();
        GetVolumeSnapshotEncryptedOnPrimaryStorageMsg encryptedMsg = new GetVolumeSnapshotEncryptedOnPrimaryStorageMsg();
        encryptedMsg.setSnapshotUuid(msg.getSnapshotUuid());
        encryptedMsg.setPrimaryStorageUuid(msg.getPrimaryStorageUuid());
        encryptedMsg.setPrimaryStorageInstallPath(msg.getPrimaryStorageInstallPath());
        bus.makeTargetServiceIdByResourceUuid(encryptedMsg, PrimaryStorageConstant.SERVICE_ID, msg.getPrimaryStorageUuid());
        bus.send(encryptedMsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                GetVolumeSnapshotEncryptedOnPrimaryStorageReply encryptedReply = reply.castReply();
                if (!reply.isSuccess()) {
                    snapshotEncryptedReply.setError(reply.getError());
                    bus.reply(msg, snapshotEncryptedReply);
                    return;
                }

                snapshotEncryptedReply.setEncrypt(encryptedReply.getEncrypt());
                snapshotEncryptedReply.setSnapshotUuid(encryptedReply.getSnapshotUuid());
                bus.reply(msg, snapshotEncryptedReply);
            }
        });

    }

    private void handleSnapshotGroup(VolumeSnapshotGroupMessage msg) {
        if (msg.getBackendOperation() == SnapshotBackendOperation.NONE || !(msg instanceof APIMessage)) {
            passThrough(msg);
            return;
        }

        List<Tuple> ts = SQL.New("select snap.volumeUuid, snap.primaryStorageUuid" +
                " from VolumeSnapshotVO snap, VolumeSnapshotGroupRefVO ref" +
                " where ref.volumeSnapshotGroupUuid = :groupUuid" +
                " and snap.uuid = ref.volumeSnapshotUuid", Tuple.class)
                .param("groupUuid", msg.getGroupUuid())
                .list();
        List<String> volumeUuids = ts.stream().map(it -> it.get(0, String.class)).filter(Objects::nonNull).collect(Collectors.toList());
        String vmInstanceUuid = volumeUuids.isEmpty() ? null : Q.New(VolumeVO.class).select(VolumeVO_.vmInstanceUuid)
                .in(VolumeVO_.uuid, volumeUuids).eq(VolumeVO_.type, VolumeType.Root).findValue();

        Map<String, List<String>> psVolumeRef = ts.stream().collect(Collectors.groupingBy(t -> ((Tuple)t).get(1, String.class),
                        Collectors.mapping(t -> ((Tuple)t).get(0, String.class), Collectors.toList())));

        final ErrorCode[] err = new ErrorCode[1];
        new While<>(psVolumeRef.entrySet()).each((e, completion) -> {
            CheckVolumeSnapshotOperationOnPrimaryStorageMsg cmsg = new CheckVolumeSnapshotOperationOnPrimaryStorageMsg();
            cmsg.setPrimaryStorageUuid(e.getKey());
            cmsg.setVolumeUuids(e.getValue());
            cmsg.setVmInstanceUuid(vmInstanceUuid);
            cmsg.setOperation(msg.getBackendOperation());
            bus.makeLocalServiceId(cmsg, PrimaryStorageConstant.SERVICE_ID);
            bus.send(cmsg, new CloudBusCallBack(completion) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        err[0] = reply.getError();
                        completion.allDone();
                        return;
                    }

                    completion.done();
                }
            });
        }).run(new WhileDoneCompletion((Message) msg) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (err[0] == null) {
                    passThrough(msg);
                    return;
                }

                APIEvent event = new APIEvent(((APIMessage) msg).getId());
                event.setError(err[0]);
                bus.publish(event);
            }
        });
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

    private void handle(APIBatchDeleteVolumeSnapshotMsg msg) {
        APIBatchDeleteVolumeSnapshotEvent event = new APIBatchDeleteVolumeSnapshotEvent(msg.getId());
        Map<String, List<String>> ancestorMap = getAncestorSnapshots(msg.getUuids());

        final String issuer = VolumeSnapshotVO.class.getSimpleName();
        List<VolumeSnapshotVO> vos = Q.New(VolumeSnapshotVO.class).in(VolumeSnapshotVO_.uuid, ancestorMap.keySet()).list();
        final List<VolumeSnapshotInventory> ctx = VolumeSnapshotInventory.valueOf(vos);
        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("batch-delete-snapshots-%s", msg.getUuids()));

        reportProgress("20");
        if (msg.getDeletionMode() == APIDeleteMessage.DeletionMode.Permissive) {
            chain.then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    casf.asyncCascade(CascadeConstant.DELETION_CHECK_CODE, issuer, ctx, new Completion(trigger) {
                        @Override
                        public void success() {
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            trigger.fail(errorCode);
                        }
                    });
                }
            }).then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    casf.asyncCascade(CascadeConstant.DELETION_DELETE_CODE, issuer, ctx, new Completion(trigger) {
                        @Override
                        public void success() {
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            trigger.fail(errorCode);
                        }
                    });
                }
            });
        } else {
            chain.then(new NoRollbackFlow() {
                @Override
                public void run(final FlowTrigger trigger, Map data) {
                    casf.asyncCascade(CascadeConstant.DELETION_FORCE_DELETE_CODE, issuer, ctx, new Completion(trigger) {
                        @Override
                        public void success() {
                            trigger.next();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            trigger.fail(errorCode);
                        }
                    });
                }
            });
        }

        Map<String, BatchDeleteVolumeSnapshotStruct> results = Stream.concat(ancestorMap.keySet().stream(), ancestorMap.values().stream().flatMap(Collection::stream))
                .collect(Collectors.toMap(uuid -> uuid, uuid -> {
                    BatchDeleteVolumeSnapshotStruct r = new BatchDeleteVolumeSnapshotStruct();
                    r.setSnapshotUuid(uuid);
                    return r;
                }));

        chain.done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                reportProgress("95");
                casf.asyncCascadeFull(CascadeConstant.DELETION_CLEANUP_CODE, issuer, ctx, new NopeCompletion());
                ctx.stream().filter(inventory -> results.containsKey(inventory.getUuid()) && results.get(inventory.getUuid()).isSuccess())
                        .forEach(inventory -> {
                            new FireSnapShotCanonicalEvent()
                                    .fireSnapShotStatusChangedEvent(VolumeSnapshotStatus.valueOf(inventory.getStatus()), inventory);
                        });
                event.setResults(new ArrayList<>(results.values()));
                bus.publish(event);
            }
        }).error(new FlowErrorHandler(msg) {
            private void handleErrorCode(ErrorCodeList errorCodeList) {
                errorCodeList.getCauses().forEach(err -> {
                    String snapshotUuid = (String) err.getFromOpaque(VolumeSnapshotConstant.SNAPSHOT_UUID);
                    List<String> spUuids = new ArrayList<>();
                    spUuids.add(snapshotUuid);
                    if (CollectionUtils.isNotEmpty(ancestorMap.get(snapshotUuid))) {
                        spUuids.addAll(ancestorMap.get(snapshotUuid));
                    }

                    spUuids.forEach(spUuid -> results.get(spUuid).setError(err));
                });
            }

            @Override
            public void handle(ErrorCode errCode, Map data) {
                reportProgress("95");
                if (!errCode.isError(VolumeSnapshotErrors.BATCH_DELETE_ERROR)) {
                    event.setError(errCode);
                    bus.publish(event);
                    return;
                }
                handleErrorCode((ErrorCodeList) errCode);
                event.setResults(new ArrayList<>(results.values()));
                ctx.stream().filter(inventory -> results.containsKey(inventory.getUuid()) && results.get(inventory.getUuid()).isSuccess())
                        .forEach(inventory -> {
                            new FireSnapShotCanonicalEvent()
                                    .fireSnapShotStatusChangedEvent(VolumeSnapshotStatus.valueOf(inventory.getStatus()), inventory);
                        });
                bus.publish(event);
            }
        }).start();
    }

    private class SnapshotAncestorStruct {
        private List<String> ancestors;
        private boolean child = false;

        public List<String> getAncestors() {
            return ancestors;
        }

        public void setAncestors(List<String> ancestors) {
            this.ancestors = ancestors;
        }

        public boolean isChild() {
            return child;
        }

        public void setChild(boolean child) {
            this.child = child;
        }
    }

    private Map<String, List<String>> getAncestorSnapshots(List<String> snapshotUuids) {
        Map<String, List<String>> result = new HashMap<>();
        Map<String, SnapshotAncestorStruct> snapshotAncestors = new HashMap();
        for (String snapshotUuid : snapshotUuids) {
            SnapshotAncestorStruct snapshotAncestor = new SnapshotAncestorStruct();
            List<String> t = getSnapshotAncestors(snapshotUuid);
            if (t.size() == 1) {
                snapshotAncestor.setAncestors(new ArrayList<>());
            } else {
                t.remove(snapshotUuid);
                snapshotAncestor.setAncestors(t);
            }

            logger.debug(String.format("got ancestors %s of snapshot[uuid: %s]", snapshotAncestor.getAncestors(), snapshotUuid));
            snapshotAncestors.put(snapshotUuid, snapshotAncestor);
        }

        for (String snapshotUuid : snapshotUuids) {
            if (snapshotUuids.parallelStream().anyMatch(s -> snapshotAncestors.get(snapshotUuid).getAncestors().contains(s))) {
                snapshotAncestors.get(snapshotUuid).setChild(true);
            }
        }

        Integer count = 0;
        for (String root: snapshotAncestors.entrySet().parallelStream().filter(e -> !e.getValue().isChild()).map(e -> e.getKey()).collect(Collectors.toList())) {
            List<String> children = snapshotAncestors.entrySet().stream().filter(e -> e.getValue().getAncestors().contains(root)).map(e -> e.getKey()).collect(Collectors.toList());
            logger.debug(String.format("get root snapshot: %s and its children: %s", root, children));
            result.put(root, children);
            count += 1 + children.size();
        }
        DebugUtils.Assert(count == snapshotUuids.size(), String.format("count of snapshotAncestors[%s] not equals to snapshots[%s]", count, snapshotUuids.size()));
        return result;
    }

    private List<String> getSnapshotAncestors(String snapshotUuid) {
        String parentUuid = Q.New(VolumeSnapshotVO.class)
                .select(VolumeSnapshotVO_.parentUuid).eq(VolumeSnapshotVO_.uuid, snapshotUuid).findValue();
        if (parentUuid == null || parentUuid.length() == 0) {
            return Arrays.asList(snapshotUuid);
        }  else {
            List<String> r = new ArrayList<>();
            r.add(snapshotUuid);
            r.addAll(getSnapshotAncestors(parentUuid));
            return r;
        }
    }

    private void handle(GetVolumeSnapshotTreeRootNodeMsg msg) {
        List<Tuple> ts = SQL.New("select snapshot.primaryStorageInstallPath, tree.current from VolumeSnapshotTreeVO tree, VolumeSnapshotVO snapshot" +
                " where tree.volumeUuid = :volUuid" +
                " and snapshot.treeUuid = tree.uuid" +
                " and snapshot.parentUuid is null", Tuple.class)
                .param("volUuid", msg.getVolumeUuid())
                .list();
        GetVolumeSnapshotTreeRootNodeReply reply = new GetVolumeSnapshotTreeRootNodeReply();
        for (Tuple t : ts) {
            if (t.get(1, Boolean.class)) {
                reply.setCurrentRootInstallPath(t.get(0, String.class));
            } else {
                reply.addPreviousRootInstallPath(t.get(0, String.class));
            }
        }
        bus.reply(msg, reply);
    }

    private void handle(APICheckVolumeSnapshotGroupAvailabilityMsg msg) {
        APICheckVolumeSnapshotGroupAvailabilityReply reply = new APICheckVolumeSnapshotGroupAvailabilityReply();
        reply.setResults(VolumeSnapshotGroupChecker.getAvailability(msg.getUuids()));
        bus.reply(msg, reply);
    }

/*
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
*/

    @Transactional
    private VolumeSnapshotStruct newChain(VolumeSnapshotVO vo, boolean fullsnapshot) {
        new SQLBatch() {
            @Override
            protected void scripts() {
                VolumeSnapshotTreeVO chain = new VolumeSnapshotTreeVO();
                chain.setCurrent(false);
                chain.setVolumeUuid(vo.getVolumeUuid());
                chain.setUuid(Platform.getUuid());
                chain.setStatus(VolumeSnapshotTreeStatus.Creating);
                chain = dbf.getEntityManager().merge(chain);

                logger.debug(String.format("created new volume snapshot tree[tree uuid:%s, volume uuid:%s, full snapshot uuid:%s]",
                        chain.getUuid(), vo.getVolumeUuid(), vo.getUuid()));

                vo.setTreeUuid(chain.getUuid());
                vo.setDistance(fullsnapshot ? 0 : 1);
                vo.setParentUuid(null);
                vo.setLatest(true);
                vo.setFullSnapshot(fullsnapshot);
                dbf.getEntityManager().persist(vo);
                dbf.getEntityManager().flush();
                dbf.getEntityManager().refresh(vo);
            }
        }.execute();


        VolumeSnapshotStruct struct = new VolumeSnapshotStruct();
        struct.setCurrent(VolumeSnapshotInventory.valueOf(vo));
        struct.setFullSnapshot(fullsnapshot);

        return struct;
    }

    public static VolumeSnapshotTreeInventory getCurrentTree(String volumeUuid) {
        VolumeSnapshotTreeVO vo = Q.New(VolumeSnapshotTreeVO.class).eq(VolumeSnapshotTreeVO_.volumeUuid, volumeUuid).eq(VolumeSnapshotTreeVO_.current, true).find();
        if (vo != null) {
            return VolumeSnapshotTreeInventory.valueOf(vo);
        } else {
            return null;
        }
    }

    public static void markSnapshotTreeCompleted(VolumeSnapshotInventory snapshot) {
        SQL.New("update VolumeSnapshotTreeVO tree" +
                " set tree.current = false" +
                " where tree.current = true" +
                " and tree.volumeUuid = :volUuid").param("volUuid", snapshot.getVolumeUuid()).execute();

        VolumeSnapshotTreeVO chain = Q.New(VolumeSnapshotTreeVO.class).eq(VolumeSnapshotTreeVO_.uuid, snapshot.getTreeUuid()).find();
        DebugUtils.Assert(chain != null, "why not found volumeSnapshotTree here?");

        SQL.New("update VolumeSnapshotTreeVO tree" +
                " set tree.current = true, tree.status = :status" +
                " where tree.uuid = :uuid").param("uuid", snapshot.getTreeUuid()).
                param("status", VolumeSnapshotTreeStatus.Completed).execute();

        logger.debug(String.format("mark new volume snapshot tree[tree uuid:%s, snapshot: %s] to Completed",
                chain.getUuid(), snapshot.getUuid()));
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
        final Integer maxIncrementalSnapshotNum = getMaxIncrementalSnapshotNum(vo.getVolumeUuid());
        if (!CoreGlobalProperty.UNIT_TEST_ON) {
            if (maxIncrementalSnapshotNum <= 1) {
                throw new OperationFailureException(operr("Unsupported maximum snapshot number (%d) for volume [uuid:%s]",
                        maxIncrementalSnapshotNum, vo.getVolumeUuid()
                ));
            }
        }

        if (chain == null) {
            return newChain(vo, maxIncrementalSnapshotNum == 0);
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

            if (latest.getDistance() >= maxIncrementalSnapshotNum) {
                dbf.getEntityManager().merge(chain);
                return newChain(vo, true);
            }

            latest.setLatest(false);
            latest = dbf.getEntityManager().merge(latest);

            vo.setTreeUuid(latest.getTreeUuid());
            vo.setLatest(true);
            vo.setParentUuid(latest.getUuid());
            vo.setDistance(latest.getDistance() + 1);
            dbf.getEntityManager().persist(vo);
            dbf.getEntityManager().flush();
            dbf.getEntityManager().refresh(vo);

            VolumeSnapshotStruct struct = new VolumeSnapshotStruct();
            struct.setParent(VolumeSnapshotInventory.valueOf(latest));
            struct.setCurrent(VolumeSnapshotInventory.valueOf(vo));
            return struct;
        }
    }

    private Integer getMaxIncrementalSnapshotNum(String volumeUuid) {
        String systemTagValue = VolumeSystemTags.VOLUME_MAX_INCREMENTAL_SNAPSHOT_NUM.getTokenByResourceUuid(volumeUuid,
                VolumeSystemTags.VOLUME_MAX_INCREMENTAL_SNAPSHOT_NUM_TOKEN);
        return systemTagValue != null ? Integer.valueOf(systemTagValue) : VolumeSnapshotGlobalConfig.MAX_INCREMENTAL_SNAPSHOT_NUM.value(Integer.class);
    }

    @Transactional
    private VolumeSnapshotStruct saveIndividualTypeSnapshot(VolumeSnapshotVO vo) {
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

    private void handle(final AskVolumeSnapshotStructMsg msg) {
        AskVolumeSnapshotStructReply reply = new AskVolumeSnapshotStructReply();
        CreateVolumeSnapshotMsg cmsg = new CreateVolumeSnapshotMsg();
        cmsg.setResourceUuid(msg.getResourceUuid());
        cmsg.setVolumeUuid(msg.getVolumeUuid());
        cmsg.setDescription(msg.getDescription());
        cmsg.setName(msg.getName());
        cmsg.setAccountUuid(msg.getAccountUuid());

        reply.setStruct(getVolumeSnapshotStruct(cmsg));
        bus.reply(msg, reply);
    }

    private VolumeSnapshotStruct getVolumeSnapshotStruct(CreateVolumeSnapshotMsg msg) {
        final VolumeVO vol = dbf.findByUuid(msg.getVolumeUuid(), VolumeVO.class);
        final String primaryStorageUuid = vol.getPrimaryStorageUuid();

        AskVolumeSnapshotCapabilityMsg askMsg = new AskVolumeSnapshotCapabilityMsg();
        askMsg.setPrimaryStorageUuid(primaryStorageUuid);
        askMsg.setVolume(VolumeInventory.valueOf(vol));
        bus.makeTargetServiceIdByResourceUuid(askMsg, PrimaryStorageConstant.SERVICE_ID, primaryStorageUuid);
        MessageReply reply = bus.call(askMsg);
        if (!reply.isSuccess()) {
            throw new OperationFailureException(operr("cannot ask primary storage[uuid:%s] for volume snapshot capability, see detail [%s]", vol.getUuid(),reply.getError()));
        }

        AskVolumeSnapshotCapabilityReply areply = reply.castReply();
        VolumeSnapshotCapability capability = areply.getCapability();
        if (!capability.isSupport()) {
            throw new OperationFailureException(operr("primary storage[uuid:%s] doesn't support volume snapshot;" +
                    " cannot create snapshot for volume[uuid:%s]", primaryStorageUuid, vol.getUuid()));
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
        vo.setAccountUuid(msg.getAccountUuid());

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

                return s;
            }
        }.execute();

        return struct;
    }

    private void handle(final CreateVolumeSnapshotMsg msg) {
        final CreateVolumeSnapshotReply ret = new CreateVolumeSnapshotReply();
        final VolumeVO vol = dbf.findByUuid(msg.getVolumeUuid(), VolumeVO.class);
        final String primaryStorageUuid = vol.getPrimaryStorageUuid();


        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("take-volume-snapshot-for-volume-%s", msg.getVolumeUuid()));
        chain.then(new ShareFlow() {
            VolumeSnapshotInventory snapshot;
            String volumeNewInstallPath;
            VolumeSnapshotStruct struct;
            long volumeSize;

            VolumeVO volumeVO = dbf.findByUuid(msg.getVolumeUuid(), VolumeVO.class);
            PrimaryStorageVO storageVO = dbf.findByUuid(volumeVO.getPrimaryStorageUuid(), PrimaryStorageVO.class);

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "create-new-tree-if-needed";
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        struct = getVolumeSnapshotStruct(msg);
                        trigger.next();
                    }
                });

                if (volumeVO.getType().equals(VolumeType.Memory)) {
                    volumeSize = Q.New(VmInstanceVO.class)
                            .select(VmInstanceVO_.memorySize)
                            .eq(VmInstanceVO_.uuid, volumeVO.getVmInstanceUuid())
                            .findValue();
                } else {
                    flow(new NoRollbackFlow() {
                        String __name__ = "sync-root-volume-size";
                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            SyncVolumeSizeOnPrimaryStorageMsg smsg = new SyncVolumeSizeOnPrimaryStorageMsg();

                            smsg.setPrimaryStorageUuid(volumeVO.getPrimaryStorageUuid());
                            smsg.setVolumeUuid(volumeVO.getUuid());
                            smsg.setInstallPath(volumeVO.getInstallPath());
                            bus.makeTargetServiceIdByResourceUuid(smsg, PrimaryStorageConstant.SERVICE_ID, volumeVO.getPrimaryStorageUuid());
                            bus.send(smsg, new CloudBusCallBack(trigger) {
                                @Override
                                public void run(MessageReply reply) {
                                    if (!reply.isSuccess()) {
                                        trigger.fail(reply.getError());
                                        return;
                                    }

                                    SyncVolumeSizeOnPrimaryStorageReply r = reply.castReply();
                                    volumeSize = r.getActualSize();

                                    trigger.next();
                                }

                            });
                        }

                        @Override
                        public boolean skip(Map data) {
                            return !PrimaryStorageType.valueOf(storageVO.getType()).isSupportCreateVolumeSnapshotCheckCapacity();
                        }
                    });
                }

                flow(new NoRollbackFlow() {
                    String __name__ = "check-primary-storage-capacity";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        boolean capacityChecked = PrimaryStorageCapacityChecker.New(volumeVO.getPrimaryStorageUuid()).checkRequiredSize(volumeSize);
                        if (!capacityChecked) {
                            trigger.fail(operr("after subtracting reserved capacity[%s], there is no primary storage having required size[%s bytes], may be the threshold of primary storage physical capacity setting is lower",
                                    PrimaryStorageGlobalConfig.RESERVED_CAPACITY.value(), volumeSize));
                            return;
                        }

                        trigger.next();
                    }

                    @Override
                    public boolean skip(Map data) {
                        return !PrimaryStorageType.valueOf(storageVO.getType()).isSupportCreateVolumeSnapshotCheckCapacity();
                    }
                });


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
                    String __name__ = "save-volume-snapshot-integrity";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<AfterCreateVolumeSnapshotExtensionPoint> extensionList = pluginRgty.getExtensionList(AfterCreateVolumeSnapshotExtensionPoint.class);

                        if (extensionList.isEmpty()) {
                            trigger.next();
                            return;
                        }

                        extensionList.forEach(exp -> exp.afterCreateVolumeSnapshot(snapshot, new Completion(trigger) {
                            @Override
                            public void success() {
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        }));
                    }
                });

                flow(new Flow() {

                    String __name__ = "reserve-snapshot-size-on-primary-storage";

                    boolean success;
                    String allocatedInstall;

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        AllocatePrimaryStorageSpaceMsg amsg = new AllocatePrimaryStorageSpaceMsg();
                        amsg.setRequiredPrimaryStorageUuid(vol.getPrimaryStorageUuid());
                        amsg.setSize(snapshot.getSize());
                        amsg.setRequiredInstallUri(String.format("volume://%s", snapshot.getVolumeUuid()));
                        amsg.setForce(true);
                        amsg.setNoOverProvisioning(true);

                        bus.makeTargetServiceIdByResourceUuid(amsg, PrimaryStorageConstant.SERVICE_ID, vol.getPrimaryStorageUuid());
                        bus.send(amsg, new CloudBusCallBack(trigger) {
                            @Override
                            public void run(MessageReply reply) {
                                if (!reply.isSuccess()) {
                                    trigger.next();
                                    return;
                                }
                                AllocatePrimaryStorageSpaceReply ar = (AllocatePrimaryStorageSpaceReply) reply;
                                allocatedInstall = ar.getAllocatedInstallUrl();
                                success = true;
                                trigger.next();
                            }
                        });
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (success) {
                            ReleasePrimaryStorageSpaceMsg rmsg = new ReleasePrimaryStorageSpaceMsg();
                            rmsg.setPrimaryStorageUuid(vol.getPrimaryStorageUuid());
                            rmsg.setDiskSize(snapshot.getSize());
                            rmsg.setAllocatedInstallUrl(allocatedInstall);
                            bus.makeTargetServiceIdByResourceUuid(rmsg, PrimaryStorageConstant.SERVICE_ID, vol.getPrimaryStorageUuid());
                            bus.send(rmsg);
                        }
                        trigger.rollback();
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        markSnapshotTreeCompleted(snapshot);
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
                                        VolumeSnapshotInventory.valueOf(svo));

                        VolumeSnapshotInventory sp = svo.toInventory();
                        callExtensionPoints(sp);

                        ret.setInventory(sp);
                        bus.reply(msg, ret);
                    }

                    @ExceptionSafe
                    private void callExtensionPoints(VolumeSnapshotInventory sp) {
                        VolumeInventory volinv = vol.toInventory();
                        pluginRgty.getExtensionList(VolumeSnapshotAfterCreateExtensionPoint.class).forEach(ext -> ext.volumeSnapshotAfterCreate(volinv, sp));
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        if (struct != null) {
                            rollbackSnapshot(struct.getCurrent().getUuid());
                        }
                        ret.setError(errCode);
                        bus.reply(msg, ret);
                    }
                });
            }
        }).start();
    }

    private void handle(MarkRootVolumeAsSnapshotMsg msg) {
        final MarkRootVolumeAsSnapshotReply ret = new MarkRootVolumeAsSnapshotReply();
        VolumeInventory vol = msg.getVolume();

        String psType = Q.New(PrimaryStorageVO.class).select(PrimaryStorageVO_.type).eq(PrimaryStorageVO_.uuid, vol.getPrimaryStorageUuid()).findValue();
        if (psType == null) {
            ret.setError(operr("cannot find type for primaryStorage [%s]", vol.getPrimaryStorageUuid()));
            bus.reply(msg, ret);
            return;
        }

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("mark-rootVolume-%s-as-snapshot", vol.getUuid()));
        chain.then(new ShareFlow() {
            VolumeSnapshotVO vo = new VolumeSnapshotVO();
            VolumeSnapshotCapability capability;
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "ask-volume-snapshot-capability";
                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        final String primaryStorageUuid = msg.getVolume().getPrimaryStorageUuid();
                        AskVolumeSnapshotCapabilityMsg askMsg = new AskVolumeSnapshotCapabilityMsg();
                        askMsg.setPrimaryStorageUuid(primaryStorageUuid);
                        askMsg.setVolume(vol);
                        bus.makeTargetServiceIdByResourceUuid(askMsg, PrimaryStorageConstant.SERVICE_ID, primaryStorageUuid);
                        MessageReply reply = bus.call(askMsg);
                        if (!reply.isSuccess()) {
                            ret.setError(operr(reply.getError(),
                                    "cannot ask primary storage[uuid:%s] for volume snapshot capability",
                                    vol.getUuid()));
                            bus.reply(msg, ret);
                            trigger.fail(ret.getError());
                            return;
                        }

                        AskVolumeSnapshotCapabilityReply areply = reply.castReply();
                        capability = areply.getCapability();
                        if (!capability.isSupport()) {
                            ret.setError(operr("primary storage[uuid:%s] doesn't support volume snapshot;" +
                                    " cannot create snapshot for volume[uuid:%s]", primaryStorageUuid, vol.getUuid()));
                            bus.reply(msg, ret);
                            trigger.fail(ret.getError());
                            return;
                        }
                        trigger.next();
                    }
                });

                // mark volume installPath as snapshot installPath, or other extentions...
                List<MarkRootVolumeAsSnapshotExtension> extensions = pluginRgty.getExtensionList(MarkRootVolumeAsSnapshotExtension.class);
                List<Flow> customizedFlows = null;
                for(MarkRootVolumeAsSnapshotExtension extension : extensions){
                    if (psType.equals(extension.getExtensionPrimaryStorageType())) {
                        customizedFlows = extension.markRootVolumeAsSnapshot(vol, vo, msg.getAccountUuid());
                        break;
                    }
                }

                if (customizedFlows != null) {
                    customizedFlows.forEach(this::flow);
                } else {
                    flow(new NoRollbackFlow() {
                        String __name__ = "mark-rootVolume-as-snapshot";
                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            vo.setUuid(Platform.getUuid());
                            vo.setName(vol.getName());
                            vo.setDescription(vol.getDescription());
                            vo.setVolumeUuid(vol.getUuid());
                            vo.setFormat(vol.getFormat());
                            vo.setVolumeType(vol.getType());
                            if (vo.getType() == null) {
                                vo.setType(VolumeSnapshotConstant.HYPERVISOR_SNAPSHOT_TYPE.toString());
                            }
                            vo.setPrimaryStorageUuid(vol.getPrimaryStorageUuid());
                            vo.setSize(vol.getSize());
                            vo.setState(VolumeSnapshotState.Enabled);
                            vo.setStatus(VolumeSnapshotStatus.Creating);
                            vo.setAccountUuid(msg.getAccountUuid());

                            if (VolumeSnapshotArrangementType.CHAIN == capability.getArrangementType()) {
                                saveChainTypeSnapshot(vo);
                            } else if (VolumeSnapshotArrangementType.INDIVIDUAL == capability.getArrangementType()) {
                                saveIndividualTypeSnapshot(vo);
                            } else {
                                DebugUtils.Assert(false, "should not be here");
                            }

                            trigger.next();
                        }
                    });
                }

                flow(new NoRollbackFlow() {
                    String __name__ = "post-mark-rootVolume-as-snapshot";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        String uuid = vo.getUuid();
                        vo = dbf.findByUuid(uuid, VolumeSnapshotVO.class);
                        if (vo == null) {
                            trigger.fail(operr("cannot find snapshot: %s", uuid));
                            return;
                        }
                        List<PostMarkRootVolumeAsSnapshotExtension> extensions = pluginRgty.getExtensionList(PostMarkRootVolumeAsSnapshotExtension.class);
                        for(PostMarkRootVolumeAsSnapshotExtension extension : extensions){
                            extension.afterMarkRootVolumeAsSnapshot(VolumeSnapshotInventory.valueOf(vo));
                        }
                        trigger.next();
                    }
                });


                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        markSnapshotTreeCompleted(VolumeSnapshotInventory.valueOf(vo));
                        VolumeSnapshotVO svo = dbf.findByUuid(vo.getUuid(), VolumeSnapshotVO.class);
                        if (svo.getPrimaryStorageInstallPath() == null) {
                            svo.setPrimaryStorageInstallPath(vol.getInstallPath());
                        }
                        svo.setStatus(VolumeSnapshotStatus.Ready);
                        if (vol.getFormat() != null) {
                            svo.setFormat(vol.getFormat());
                        }
                        svo = dbf.updateAndRefresh(svo);
                        tagMgr.createNonInherentSystemTag(svo.getUuid(),
                                VolumeSnapshotSystemTags.VOLUMESNAPSHOT_CREATED_BY_SYSTEM.getTagFormat(),
                                VolumeSnapshotVO.class.getSimpleName());
                        new FireSnapShotCanonicalEvent().
                                fireSnapShotStatusChangedEvent(svo.getStatus(), VolumeSnapshotInventory.valueOf(svo));
                        ret.setInventory(VolumeSnapshotInventory.valueOf(svo));
                        bus.reply(msg, ret);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        rollbackSnapshot(vo.getUuid());
                        ret.setError(errCode);
                        bus.reply(msg, ret);
                    }
                });
            }
        }).start();
    }

    private void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIBatchDeleteVolumeSnapshotMsg) {
            handle((APIBatchDeleteVolumeSnapshotMsg) msg);
        } else if (msg instanceof APICheckVolumeSnapshotGroupAvailabilityMsg) {
            handle((APICheckVolumeSnapshotGroupAvailabilityMsg) msg);
        } else if (msg instanceof APIGetMemorySnapshotGroupReferenceMsg) {
            handle((APIGetMemorySnapshotGroupReferenceMsg) msg);
        } else  {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIGetMemorySnapshotGroupReferenceMsg msg) {
        APIGetMemorySnapshotGroupReferenceReply reply = new APIGetMemorySnapshotGroupReferenceReply();

        if (!L3NetworkVO.class.getSimpleName().equals(msg.getResourceType())) {
            bus.reply(msg, reply);
            return;
        }

        List<String> quotedArchiveGroupList = Q.New(VmInstanceDeviceAddressArchiveVO.class)
                .select(VmInstanceDeviceAddressArchiveVO_.addressGroupUuid)
                .eq(VmInstanceDeviceAddressArchiveVO_.metadataClass, ArchiveVmNicType.class.getCanonicalName())
                .like(VmInstanceDeviceAddressArchiveVO_.metadata, String.format("%%\"l3NetworkUuid\":\"%s\"%%", msg.getResourceUuid())).listValues();

        if (quotedArchiveGroupList.isEmpty()){
            bus.reply(msg, reply);
            return;
        }

        String sql = "select snapshotGroup from VolumeSnapshotGroupVO snapshotGroup, VmInstanceDeviceAddressGroupVO deviceAddressGroup where snapshotGroup.uuid = deviceAddressGroup.resourceUuid and deviceAddressGroup.uuid in :addressGroupUuids";
        TypedQuery<VolumeSnapshotGroupVO> q = dbf.getEntityManager().createQuery(sql, VolumeSnapshotGroupVO.class);
        q.setParameter("addressGroupUuids", quotedArchiveGroupList);
        List<VolumeSnapshotGroupVO> result = q.getResultList();
        reply.setInventories(VolumeSnapshotGroupInventory.valueOf(result));
        bus.reply(msg, reply);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(VolumeSnapshotConstant.SERVICE_ID);
    }

    @Override
    public boolean start() {
        pluginRgty.saveExtensionAsMap(CreateTemplateFromVolumeSnapshotExtensionPoint.class,
                (Function<Object, CreateTemplateFromVolumeSnapshotExtensionPoint>) arg -> arg.createTemplateFromVolumeSnapshotPrimaryStorageType());

        bus.installBeforeDeliveryMessageInterceptor(new AbstractBeforeDeliveryMessageInterceptor() {
            @Override
            public void beforeDeliveryMessage(Message msg) {
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
        }, VolumeCreateSnapshotMsg.class, CreateVolumeSnapshotMsg.class);
        handleVolumeDeletionEvent();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public List<Class> getReplyMessageClassForMarshalExtensionPoint() {
        List<Class> ret = new ArrayList<>();
        ret.add(APIQueryVolumeSnapshotTreeReply.class);
        return ret;
    }

    @Override
    public void marshalReplyMessageBeforeSending(Message replyOrEvent, NeedReplyMessage msg) {
        if (replyOrEvent instanceof APIQueryVolumeSnapshotTreeReply) {
            marshal(((APIMessage) msg).getSession(), (APIQueryVolumeSnapshotTreeReply) replyOrEvent);
        }
    }

    private void marshal(SessionInventory session, APIQueryVolumeSnapshotTreeReply reply) {
        if (reply.getInventories() == null) {
            // this is for count
            return;
        }

        for (VolumeSnapshotTreeInventory inv : reply.getInventories()) {
            SimpleQuery<VolumeSnapshotVO> sq = dbf.createQuery(VolumeSnapshotVO.class);
            sq.add(VolumeSnapshotVO_.treeUuid, Op.EQ, inv.getUuid());
            List<VolumeSnapshotVO> vos = sq.list();
            VolumeSnapshotTree tree = VolumeSnapshotTree.fromVOs(vos);
            inv.setTree(tree.getRoot().toLeafInventory(querySnapshotUuids(inv.getUuid(), session)));
        }
    }

    @SuppressWarnings("unchecked")
    private Set<String> querySnapshotUuids(String treeUuid, SessionInventory session) {
        String zql = String.format("query volumesnapshot.uuid where treeUuid = '%s'", treeUuid);
        List<Object> invs = ZQL.fromString(zql).getSingleResultWithSession(session).inventories;
        return invs != null ? invs.stream().map(it -> ((VolumeSnapshotInventory) it).getUuid()).collect(Collectors.toSet())
                : Collections.emptySet();
    }

    @Override
    @Transactional(readOnly = true)
    public void volumePreExpunge(VolumeInventory volume) {
        List<String> snapUuids = Q.New(VolumeSnapshotVO.class).select(VolumeSnapshotVO_.uuid)
                .eq(VolumeSnapshotVO_.volumeUuid, volume.getUuid())
                .listValues();

        if (snapUuids.isEmpty()) {
            return;
        }

        String tagFmt = getBackingVolumeTag("%");
        List<String> protectedSnapUuids = Q.New(SystemTagVO.class).select(SystemTagVO_.resourceUuid)
                .eq(SystemTagVO_.resourceType, VolumeSnapshotVO.class.getSimpleName())
                .in(SystemTagVO_.resourceUuid, snapUuids)
                .like(SystemTagVO_.tag, tagFmt)
                .listValues();
        if (!protectedSnapUuids.isEmpty()) {
            throw new OperationFailureException(operr("volume snapshot[uuids:%s] is protected, " +
                    "do not allow to delete volume.", new HashSet<>(protectedSnapUuids).toString()));
        }
    }

    @Override
    public void volumeBeforeExpunge(VolumeInventory volume, Completion completion) {
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

        new While<>(msgs).all((msg, c) -> {
            bus.send(msg, new CloudBusCallBack(c) {
                @Override
                public void run(MessageReply reply) {
                    if (!reply.isSuccess()) {
                        c.addError(reply.getError());
                    }

                    c.done();
                }
            });
        }).run(new WhileDoneCompletion(completion) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errorCodeList.getCauses().isEmpty()) {
                    completion.fail(errorCodeList.getCauses().get(0));
                    return;
                }

                completion.success();
            }
        });
    }

    private void handleVolumeDeletionEvent() {
        evtf.onLocal(VolumeCanonicalEvents.VOLUME_STATUS_CHANGED_PATH, new EventCallback() {
            @Override
            protected void run(Map tokens, Object data) {
                VolumeCanonicalEvents.VolumeStatusChangedData d = (VolumeCanonicalEvents.VolumeStatusChangedData) data;
                if (Q.New(VolumeVO.class).eq(VolumeVO_.uuid, d.getVolumeUuid()).isExists()) {
                    return;
                }

                String toDeleteTag = getBackingVolumeTag(d.getVolumeUuid());
                SQL.New(SystemTagVO.class).eq(SystemTagVO_.resourceType, VolumeSnapshotVO.class.getSimpleName())
                        .eq(SystemTagVO_.tag, toDeleteTag).delete();
            }
        });
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
                    } else if (msg instanceof APICreateVolumeSnapshotGroupMsg) {
                        check((APICreateVolumeSnapshotGroupMsg) msg, pairs);
                    } else if (msg instanceof APIChangeResourceOwnerMsg) {
                        check((APIChangeResourceOwnerMsg) msg, pairs);
                    } else if (msg instanceof APIRecoverDataVolumeMsg) {
                        check((APIRecoverDataVolumeMsg) msg, pairs);
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
                    } else if (msg instanceof CreateVolumeSnapshotMsg) {
                        check((CreateVolumeSnapshotMsg) msg, pairs);
                    }
                }
            }

            @Override
            public List<Quota.QuotaUsage> getQuotaUsageByAccount(String accountUuid) {
                List<Quota.QuotaUsage> usages = new ArrayList<>();

                Quota.QuotaUsage usage;

                usage = new Quota.QuotaUsage();
                usage.setName(VolumeSnapshotQuotaConstant.VOLUME_SNAPSHOT_NUM);
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

            private void check(CreateVolumeSnapshotMsg msg, Map<String, Quota.QuotaPair> pairs) {
                checkVolumeSnapshotNumQuota(msg.getAccountUuid(), msg.getAccountUuid(), 1, pairs);
            }

            private void check(CreateVolumesSnapshotMsg msg, Map<String, Quota.QuotaPair> pairs) {
                checkVolumeSnapshotNumQuota(msg.getAccountUuid(), msg.getAccountUuid(), msg.getVolumeSnapshotJobs().size(), pairs);
            }

            private void check(APICreateVolumeSnapshotMsg msg, Map<String, Quota.QuotaPair> pairs) {
                String resourceTargetOwnerUuid = new QuotaUtil().getResourceOwnerAccountUuid(msg.getVolumeUuid());
                checkVolumeSnapshotNumQuota(msg.getSession().getAccountUuid(), resourceTargetOwnerUuid, 1, pairs);
            }

            private void check(APICreateVolumeSnapshotGroupMsg msg, Map<String, Quota.QuotaPair> pairs) {
                String resourceTargetOwnerUuid = new QuotaUtil().getResourceOwnerAccountUuid(msg.getVolumeUuid());
                long snapCount = SQL.New("select count(vol) from VolumeVO vol" +
                        " where vol.vmInstanceUuid =" +
                        " (select v.vmInstanceUuid from VolumeVO v where v.uuid = :uuid)", Long.class)
                        .param("uuid", msg.getRootVolumeUuid())
                        .find();
                checkVolumeSnapshotNumQuota(msg.getSession().getAccountUuid(), resourceTargetOwnerUuid, snapCount, pairs);
            }

            private void check(APIRecoverDataVolumeMsg msg, Map<String, Quota.QuotaPair> pairs) {
                String resourceTargetOwnerUuid = new QuotaUtil().getResourceOwnerAccountUuid(msg.getVolumeUuid());
                long cnt = Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.volumeUuid, msg.getVolumeUuid()).count();
                checkVolumeSnapshotNumQuota(msg.getSession().getAccountUuid(), resourceTargetOwnerUuid, cnt, pairs);
            }

            private void checkVolumeSnapshotNumQuota(String currentAccountUuid,
                                                     String resourceTargetOwnerAccountUuid,
                                                     long volumeSnapshotNumAsked,
                                                     Map<String, Quota.QuotaPair> pairs) {
                long volumeSnapshotNumQuota = pairs.get(VolumeSnapshotQuotaConstant.VOLUME_SNAPSHOT_NUM).getValue();
                long volumeSnapshotNumUsed = getUsedVolumeSnapshotNum(resourceTargetOwnerAccountUuid);
                {
                    QuotaUtil.QuotaCompareInfo quotaCompareInfo;
                    quotaCompareInfo = new QuotaUtil.QuotaCompareInfo();
                    quotaCompareInfo.currentAccountUuid = currentAccountUuid;
                    quotaCompareInfo.resourceTargetOwnerAccountUuid = resourceTargetOwnerAccountUuid;
                    quotaCompareInfo.quotaName = VolumeSnapshotQuotaConstant.VOLUME_SNAPSHOT_NUM;
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
        p.setName(VolumeSnapshotQuotaConstant.VOLUME_SNAPSHOT_NUM);
        p.setValue(VolumeSnapshotQuotaGlobalConfig.VOLUME_SNAPSHOT_NUM.defaultValue(Long.class));
        quota.addPair(p);

        quota.addMessageNeedValidation(APICreateVolumeSnapshotMsg.class);
        quota.addMessageNeedValidation(APICreateVolumeSnapshotGroupMsg.class);
        quota.addMessageNeedValidation(APIChangeResourceOwnerMsg.class);
        quota.addMessageNeedValidation(APIRecoverDataVolumeMsg.class);
        quota.addMessageNeedValidation(VolumeCreateSnapshotMsg.class);
        quota.addMessageNeedValidation(CreateVolumeSnapshotMsg.class);
        quota.addMessageNeedValidation(CreateVolumesSnapshotMsg.class);
        quota.setOperator(checker);

        return list(quota);
    }

    @Override
    public void afterReimageVmInstance(VolumeInventory inventory) {
        removeVolumeFromOldSnapshotTreeInDb(inventory.getUuid());
    }

    @Override
    public void afterOverwriteVolume(VolumeInventory volume, VolumeInventory transientVolume) {
        removeVolumeFromOldSnapshotTreeInDb(volume.getUuid());
    }

    @Transactional
    private void removeVolumeFromOldSnapshotTreeInDb(String volumeUuid) {
        String sql = "update VolumeSnapshotVO s" +
                " set s.latest = false" +
                " where s.latest = true" +
                " and s.volumeUuid = :volumeUuid";
        Query q = dbf.getEntityManager().createQuery(sql);
        q.setParameter("volumeUuid", volumeUuid);
        q.executeUpdate();

        sql = "update VolumeSnapshotTreeVO tree" +
                " set tree.current = false" +
                " where tree.current = true" +
                " and tree.volumeUuid = :volUuid";
        q = dbf.getEntityManager().createQuery(sql);
        q.setParameter("volUuid", volumeUuid);
        q.executeUpdate();
    }

    @Override
    public void vmJustBeforeDeleteFromDb(VmInstanceInventory inv) {
        deleteStaleSnapshotRecords(inv.getRootVolumeUuid(), VolumeType.Root, inv.getUuid());
    }

    private void deleteStaleSnapshotRecords(String volumeUuid, VolumeType volumeType, String vmUuid) {
        new SQLBatch() {
            @Override
            protected void scripts() {
                if (!q(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.volumeUuid, volumeUuid).isExists()) {
                    return;
                }

                List<String> treeUuids = q(VolumeSnapshotVO.class).select(VolumeSnapshotVO_.treeUuid).eq(VolumeSnapshotVO_.volumeUuid, volumeUuid).listValues();

                sql(VolumeSnapshotTreeVO.class).in(VolumeSnapshotTreeVO_.uuid, treeUuids).delete();

                sql(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.volumeUuid, volumeUuid).delete();

                if (volumeType == VolumeType.Root) {
                    sql(VolumeSnapshotGroupVO.class).eq(VolumeSnapshotGroupVO_.vmInstanceUuid, vmUuid).delete();
                } else {
                    sql(VolumeSnapshotGroupRefVO.class).eq(VolumeSnapshotGroupRefVO_.volumeUuid, volumeUuid)
                            .set(VolumeSnapshotGroupRefVO_.snapshotDeleted, true)
                            .update();
                }
            }
        }.execute();
    }

    @Override
    public void volumeJustBeforeDeleteFromDb(VolumeInventory inv) {
        deleteStaleSnapshotRecords(inv.getUuid(), VolumeType.valueOf(inv.getType()), inv.getVmInstanceUuid());
    }
}
