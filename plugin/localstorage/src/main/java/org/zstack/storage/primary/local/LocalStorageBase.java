package org.zstack.storage.primary.local;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.db.UpdateQuery;
import org.zstack.core.thread.AsyncThread;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.core.workflow.ShareFlow;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.core.AsyncLatch;
import org.zstack.header.core.Completion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.primary.VolumeSnapshotCapability.VolumeSnapshotArrangementType;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_;
import org.zstack.header.volume.VolumeConstant;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.storage.primary.PrimaryStorageBase;
import org.zstack.storage.primary.PrimaryStorageCapacityUpdater;
import org.zstack.storage.primary.PrimaryStoragePhysicalCapacityManager;
import org.zstack.storage.primary.local.APIGetLocalStorageHostDiskCapacityReply.HostDiskCapacity;
import org.zstack.storage.primary.local.MigrateBitsStruct.ResourceInfo;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.function.Function;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.*;
import java.util.concurrent.Callable;

import static org.zstack.utils.CollectionDSL.list;

/**
 * Created by frank on 6/30/2015.
 */
public class LocalStorageBase extends PrimaryStorageBase {
    private static final CLogger logger = Utils.getLogger(LocalStorageBase.class);

    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    protected PrimaryStorageOverProvisioningManager ratioMgr;
    @Autowired
    protected PrimaryStoragePhysicalCapacityManager physicalCapacityMgr;
    @Autowired
    private LocalStorageImageCleaner imageCacheCleaner;

    static class FactoryCluster {
        LocalStorageHypervisorFactory factory;
        List<ClusterInventory> clusters;
    }

    public LocalStorageBase(PrimaryStorageVO self) {
        super(self);
    }

    @Override
    public void detachHook(String clusterUuid, Completion completion) {
        SimpleQuery<ClusterVO> q = dbf.createQuery(ClusterVO.class);
        q.select(ClusterVO_.hypervisorType);
        q.add(ClusterVO_.uuid, Op.EQ, clusterUuid);
        String hvType = q.findValue();

        LocalStorageHypervisorFactory f = getHypervisorBackendFactory(hvType);
        final LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.detachHook(clusterUuid, new Completion(completion) {
            @Override
            public void success() {
                syncPhysicalCapacity(new ReturnValueCompletion<PhysicalCapacityUsage>() {
                    @Override
                    public void success(PhysicalCapacityUsage returnValue) {
                        setCapacity(null, null, returnValue.totalPhysicalSize, returnValue.availablePhysicalSize);
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        logger.warn(String.format("failed to sync the physical capacity on the local primary storage[uuid:%s], %s",
                                self.getUuid(), errorCode));
                    }
                });

                completion.success();
            }

            @Override
            public void fail(ErrorCode errorCode) {

            }
        });
    }

    @Override
    public void handleApiMessage(APIMessage msg) {
        if (msg instanceof APIGetLocalStorageHostDiskCapacityMsg) {
            handle((APIGetLocalStorageHostDiskCapacityMsg) msg);
        } else if (msg instanceof APILocalStorageMigrateVolumeMsg) {
            handle((APILocalStorageMigrateVolumeMsg) msg);
        } else if (msg instanceof APILocalStorageGetVolumeMigratableHostsMsg) {
            handle((APILocalStorageGetVolumeMigratableHostsMsg) msg);
        } else {
            super.handleApiMessage(msg);
        }
    }

    @Transactional(readOnly = true)
    private void handle(APILocalStorageGetVolumeMigratableHostsMsg msg) {
        // this API does the best it can to find migratable hosts.
        // it doesn't count the base image size because the image may have
        // been deleted, and ZStack has to consult the host for the image size

        APILocalStorageGetVolumeMigratableReply reply = new APILocalStorageGetVolumeMigratableReply();
        String sql = "select vol.size from VolumeVO vol where vol.uuid = :uuid";
        TypedQuery<Long> vq = dbf.getEntityManager().createQuery(sql, Long.class);
        vq.setParameter("uuid", msg.getVolumeUuid());
        long size = vq.getSingleResult();
        size = ratioMgr.calculateByRatio(self.getUuid(), size);

        sql = "select sum(sp.size) from VolumeSnapshotVO sp where sp.volumeUuid = :volUuid";
        TypedQuery<Long> sq = dbf.getEntityManager().createQuery(sql, Long.class);
        sq.setParameter("volUuid", msg.getVolumeUuid());
        Long snapshotSize = sq.getSingleResult();

        if (snapshotSize != null) {
            size += snapshotSize;
        }

        sql = "select href.hostUuid" +
                " from LocalStorageHostRefVO href" +
                " where href.hostUuid !=" +
                " (" +
                " select rref.hostUuid" +
                " from LocalStorageResourceRefVO rref" +
                " where rref.resourceUuid = :volUuid" +
                " and rref.resourceType = :rtype" +
                " )" +
                " and (href.totalPhysicalCapacity * (1 - :thres)) <= href.availablePhysicalCapacity" +
                " and href.availablePhysicalCapacity != 0" +
                " and href.availableCapacity >= :size" +
                " and href.primaryStorageUuid = :psUuid" +
                " group by href.hostUuid";

        double physicalThreshold = physicalCapacityMgr.getRatio(self.getUuid());

        //TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        Query q = dbf.getEntityManager().createNativeQuery(sql);
        q.setParameter("volUuid", msg.getVolumeUuid());
        q.setParameter("rtype", VolumeVO.class.getSimpleName());
        q.setParameter("thres", physicalThreshold);
        q.setParameter("size", size);
        q.setParameter("psUuid", self.getUuid());
        List<String> hostUuids = q.getResultList();

        if (hostUuids.isEmpty()) {
            reply.setInventories(new ArrayList<HostInventory>());
            bus.reply(msg, reply);
            return;
        }

        sql = "select h from HostVO h where h.uuid in (:uuids) and h.status = :hstatus";
        TypedQuery<HostVO> hq = dbf.getEntityManager().createQuery(sql, HostVO.class);
        hq.setParameter("uuids", hostUuids);
        hq.setParameter("hstatus", HostStatus.Connected);
        List<HostVO> hosts = hq.getResultList();
        reply.setInventories(HostInventory.valueOf(hosts));
        bus.reply(msg, reply);
    }

    private void handle(final APILocalStorageMigrateVolumeMsg msg) {
        final APILocalStorageMigrateVolumeEvent evt = new APILocalStorageMigrateVolumeEvent(msg.getId());
        MigrateVolumeOnLocalStorageMsg mmsg = new MigrateVolumeOnLocalStorageMsg();
        mmsg.setPrimaryStorageUuid(msg.getPrimaryStorageUuid());
        mmsg.setDestHostUuid(msg.getDestHostUuid());
        mmsg.setVolumeUuid(msg.getVolumeUuid());
        bus.makeTargetServiceIdByResourceUuid(mmsg, PrimaryStorageConstant.SERVICE_ID, self.getUuid());

        MigrateVolumeOverlayMsg omsg = new MigrateVolumeOverlayMsg();
        omsg.setMessage(mmsg);
        omsg.setVolumeUuid(msg.getVolumeUuid());
        bus.makeTargetServiceIdByResourceUuid(omsg, VolumeConstant.SERVICE_ID, msg.getVolumeUuid());

        bus.send(omsg, new CloudBusCallBack(msg) {
            @Override
            public void run(MessageReply reply) {
                if (!reply.isSuccess()) {
                    evt.setErrorCode(reply.getError());
                    bus.publish(evt);
                    return;
                }

                MigrateVolumeOnLocalStorageReply mr = reply.castReply();
                evt.setInventory(mr.getInventory());
                bus.publish(evt);
            }
        });
    }

    private void handle(final MigrateVolumeOnLocalStorageMsg msg) {
        thdf.chainSubmit(new ChainTask(msg) {
            @Override
            public String getSyncSignature() {
                return String.format("migrate-volume-%s", msg.getVolumeUuid());
            }

            @Override
            public void run(SyncTaskChain chain) {
                migrateVolume(msg, new NoErrorCompletion(msg, chain) {
                    @Override
                    public void done() {
                        chain.next();
                    }
                });
            }

            @Override
            public String getName() {
                return getSyncSignature();
            }
        });
    }

    private void migrateVolume(MigrateVolumeOnLocalStorageMsg msg, NoErrorCompletion completion) {
        MigrateVolumeOnLocalStorageReply reply = new MigrateVolumeOnLocalStorageReply();

        SimpleQuery<LocalStorageResourceRefVO> refq = dbf.createQuery(LocalStorageResourceRefVO.class);
        refq.add(LocalStorageResourceRefVO_.resourceUuid, Op.EQ, msg.getVolumeUuid());
        refq.add(LocalStorageResourceRefVO_.resourceType, Op.EQ, VolumeVO.class.getSimpleName());
        LocalStorageResourceRefVO ref = refq.find();
        if (ref == null) {
            reply.setError(errf.stringToOperationError(String.format("volume[uuid:%s] is not on the local storage anymore," +
                    "it may have been deleted", msg.getVolumeUuid())));
            bus.reply(msg, reply);
            completion.done();
            return;
        }

        if (ref.getHostUuid().equals(msg.getDestHostUuid())) {
            logger.debug(String.format("the volume[uuid:%s] is already on the host[uuid:%s], no need to migrate",
                    msg.getVolumeUuid(), msg.getDestHostUuid()));
            bus.reply(msg, reply);
            completion.done();
            return;
        }

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("migrate-volume-%s-local-storage-%s-to-host-%s",
                msg.getVolumeUuid(), msg.getPrimaryStorageUuid(), msg.getDestHostUuid()));
        chain.then(new ShareFlow() {
            LocalStorageResourceRefVO volumeRefVO;
            List<LocalStorageResourceRefVO> snapshotRefVOS;
            LocalStorageResourceRefInventory ref;
            long requiredSize;
            List<VolumeSnapshotVO> snapshots;
            VolumeVO volume;
            MigrateBitsStruct struct = new MigrateBitsStruct();
            LocalStorageHypervisorBackend bkd;

            {
                SimpleQuery<LocalStorageResourceRefVO> q = dbf.createQuery(LocalStorageResourceRefVO.class);
                q.add(LocalStorageResourceRefVO_.resourceType, Op.EQ, VolumeVO.class.getSimpleName());
                q.add(LocalStorageResourceRefVO_.resourceUuid, Op.EQ, msg.getVolumeUuid());
                volumeRefVO = q.find();
                ref = LocalStorageResourceRefInventory.valueOf(volumeRefVO);

                SimpleQuery<VolumeSnapshotVO> sq = dbf.createQuery(VolumeSnapshotVO.class);
                sq.add(VolumeSnapshotVO_.volumeUuid, Op.EQ, ref.getResourceUuid());
                snapshots = sq.list();

                volume = dbf.findByUuid(ref.getResourceUuid(), VolumeVO.class);

                requiredSize = ratioMgr.calculateByRatio(self.getUuid(), ref.getSize());

                ResourceInfo info = new ResourceInfo();
                info.setResourceRef(ref);
                info.setPath(volume.getInstallPath());

                struct = new MigrateBitsStruct();
                struct.getInfos().add(info);
                struct.setDestHostUuid(msg.getDestHostUuid());
                struct.setSrcHostUuid(ref.getHostUuid());
                struct.setVolume(VolumeInventory.valueOf(volume));

                if (!snapshots.isEmpty()) {
                    List<String> spUuids = CollectionUtils.transformToList(snapshots, new Function<String, VolumeSnapshotVO>() {
                        @Override
                        public String call(VolumeSnapshotVO arg) {
                            return arg.getUuid();
                        }
                    });

                    SimpleQuery<LocalStorageResourceRefVO> rq = dbf.createQuery(LocalStorageResourceRefVO.class);
                    rq.add(LocalStorageResourceRefVO_.resourceType, Op.EQ, VolumeSnapshotVO.class.getSimpleName());
                    rq.add(LocalStorageResourceRefVO_.resourceUuid, Op.IN, spUuids);
                    snapshotRefVOS = rq.list();

                    for (final VolumeSnapshotVO vo : snapshots) {
                        info = new ResourceInfo();
                        info.setPath(vo.getPrimaryStorageInstallPath());
                        info.setResourceRef(CollectionUtils.find(snapshotRefVOS, new Function<LocalStorageResourceRefInventory, LocalStorageResourceRefVO>() {
                            @Override
                            public LocalStorageResourceRefInventory call(LocalStorageResourceRefVO arg) {
                                return arg.getResourceUuid().equals(vo.getUuid()) ? LocalStorageResourceRefInventory.valueOf(arg) : null;
                            }
                        }));

                        if (info.getResourceRef() == null) {
                            throw new CloudRuntimeException(
                                    String.format("cannot find reference of snapshot[uuid:%s, name:%s] on the local storage[uuid:%s, name:%s]",
                                            vo.getUuid(), vo.getName(), self.getUuid(), self.getName()));
                        }

                        struct.getInfos().add(info);

                        requiredSize += vo.getSize();
                    }
                }

                LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByHostUuid(msg.getDestHostUuid());
                bkd = f.getHypervisorBackend(self);
            }

            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "reserve-capacity-on-dest-host";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        reserveCapacityOnHost(msg.getDestHostUuid(), requiredSize);
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        returnCapacityToHost(msg.getDestHostUuid(), requiredSize);
                        trigger.rollback();
                    }
                });

                List<Flow> flows = bkd.createMigrateBitsFlow(struct);
                for (Flow fl : flows) {
                    flow(fl);
                }

                flow(new NoRollbackFlow() {
                    String __name__ = "change-reference-to-dst-host";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<LocalStorageResourceRefVO> refs = new ArrayList<>();
                        volumeRefVO.setHostUuid(msg.getDestHostUuid());
                        refs.add(volumeRefVO);

                        if (snapshotRefVOS != null) {
                            for (LocalStorageResourceRefVO r : snapshotRefVOS) {
                                r.setHostUuid(msg.getDestHostUuid());
                                refs.add(r);
                            }
                        }

                        dbf.updateCollection(refs);
                        trigger.next();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "return-capacity-to-src-host";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        returnCapacityToHost(ref.getHostUuid(), requiredSize);
                        trigger.next();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "delete-bits-on-the-src-host";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        List<String> paths = new ArrayList<>();
                        paths.add(volume.getInstallPath());
                        for (VolumeSnapshotVO sp : snapshots) {
                            paths.add(sp.getPrimaryStorageInstallPath());
                        }

                        final Iterator<String> it = paths.iterator();
                        new Runnable() {
                            @Override
                            @AsyncThread
                            public void run() {
                                if (!it.hasNext()) {
                                    return;
                                }

                                final String path = it.next();
                                bkd.deleteBits(path, struct.getSrcHostUuid(), new Completion() {
                                    @Override
                                    public void success() {
                                        run();
                                    }

                                    @Override
                                    public void fail(ErrorCode errorCode) {
                                        //TODO
                                        logger.warn(String.format("failed to delete %s on the host[uuid:%s], %s",
                                                path, struct.getSrcHostUuid(), errorCode));
                                        run();
                                    }
                                });
                            }
                        }.run();

                        trigger.next();
                    }
                });

                done(new FlowDoneHandler(msg, completion) {
                    @Override
                    public void handle(Map data) {
                        reply.setInventory(LocalStorageResourceRefInventory.valueOf(dbf.reload(volumeRefVO)));
                        bus.reply(msg, reply);
                    }
                });

                error(new FlowErrorHandler(msg, completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                    }
                });

                Finally(new FlowFinallyHandler(msg, completion) {
                    @Override
                    public void Finally() {
                        completion.done();
                    }
                });
            }
        }).start();
    }

    @Override
    public void handleLocalMessage(Message msg) {
        if (msg instanceof InitPrimaryStorageOnHostConnectedMsg) {
            handle((InitPrimaryStorageOnHostConnectedMsg) msg);
        } else if (msg instanceof RemoveHostFromLocalStorageMsg) {
            handle((RemoveHostFromLocalStorageMsg) msg);
        } else if (msg instanceof TakeSnapshotMsg) {
            handle((TakeSnapshotMsg) msg);
        } else if (msg instanceof DeleteSnapshotOnPrimaryStorageMsg) {
            handle((DeleteSnapshotOnPrimaryStorageMsg) msg);
        } else if (msg instanceof RevertVolumeFromSnapshotOnPrimaryStorageMsg) {
            handle((RevertVolumeFromSnapshotOnPrimaryStorageMsg) msg);
        } else if (msg instanceof BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg) {
            handle((BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg) msg);
        } else if (msg instanceof CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg) {
            handle((CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg) msg);
        } else if (msg instanceof MergeVolumeSnapshotOnPrimaryStorageMsg) {
            handle((MergeVolumeSnapshotOnPrimaryStorageMsg) msg);
        } else if (msg instanceof DownloadImageToPrimaryStorageCacheMsg) {
            handle((DownloadImageToPrimaryStorageCacheMsg) msg);
        } else if (msg instanceof LocalStorageCreateEmptyVolumeMsg) {
            handle((LocalStorageCreateEmptyVolumeMsg) msg);
        } else if (msg instanceof LocalStorageDirectlyDeleteBitsMsg) {
            handle((LocalStorageDirectlyDeleteBitsMsg) msg);
        } else if (msg instanceof LocalStorageReserveHostCapacityMsg) {
            handle((LocalStorageReserveHostCapacityMsg) msg);
        } else if (msg instanceof LocalStorageReturnHostCapacityMsg) {
            handle((LocalStorageReturnHostCapacityMsg) msg);
        } else if (msg instanceof LocalStorageHypervisorSpecificMessage) {
            handle((LocalStorageHypervisorSpecificMessage) msg);
        } else if (msg instanceof CreateTemporaryVolumeFromSnapshotMsg) {
            handle((CreateTemporaryVolumeFromSnapshotMsg) msg);
        } else if (msg instanceof UploadBitsFromLocalStorageToBackupStorageMsg) {
            handle((UploadBitsFromLocalStorageToBackupStorageMsg) msg);
        } else if (msg instanceof GetVolumeRootImageUuidFromPrimaryStorageMsg) {
            handle((GetVolumeRootImageUuidFromPrimaryStorageMsg) msg);
        } else if (msg instanceof LocalStorageDeleteImageCacheOnPrimaryStorageMsg) {
            handle((LocalStorageDeleteImageCacheOnPrimaryStorageMsg) msg);
        } else if (msg instanceof MigrateVolumeOnLocalStorageMsg) {
            handle((MigrateVolumeOnLocalStorageMsg) msg);
        } else if (msg instanceof ReInitRootVolumeFromTemplateOnPrimaryStorageMsg) {
            handle((ReInitRootVolumeFromTemplateOnPrimaryStorageMsg) msg);
        } else {
            super.handleLocalMessage(msg);
        }
    }

    @Override
    protected void handle(APICleanUpImageCacheOnPrimaryStorageMsg msg) {
        APICleanUpImageCacheOnPrimaryStorageEvent evt = new APICleanUpImageCacheOnPrimaryStorageEvent(msg.getId());
        imageCacheCleaner.cleanup(msg.getUuid());
        bus.publish(evt);
    }


    private void handle(final LocalStorageDeleteImageCacheOnPrimaryStorageMsg msg) {
        LocalStorageHypervisorBackend bkd = getHypervisorBackendFactoryByHostUuid(msg.getHostUuid()).getHypervisorBackend(self);
        bkd.handle(msg, msg.getHostUuid(), new ReturnValueCompletion<DeleteImageCacheOnPrimaryStorageReply>(msg) {
            @Override
            public void success(DeleteImageCacheOnPrimaryStorageReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                DeleteImageCacheOnPrimaryStorageReply reply = new DeleteImageCacheOnPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final GetVolumeRootImageUuidFromPrimaryStorageMsg msg) {
        String hostUuid = getHostUuidByResourceUuid(msg.getVolume().getUuid());
        LocalStorageHypervisorBackend bkd = getHypervisorBackendFactoryByHostUuid(hostUuid).getHypervisorBackend(self);
        bkd.handle(msg, hostUuid, new ReturnValueCompletion<GetVolumeRootImageUuidFromPrimaryStorageReply>(msg) {
            @Override
            public void success(GetVolumeRootImageUuidFromPrimaryStorageReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                GetVolumeRootImageUuidFromPrimaryStorageReply reply = new GetVolumeRootImageUuidFromPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final UploadBitsFromLocalStorageToBackupStorageMsg msg) {
        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByHostUuid(msg.getHostUuid());
        LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, msg.getHostUuid(), new ReturnValueCompletion<UploadBitsFromLocalStorageToBackupStorageReply>(msg) {
            @Override
            public void success(UploadBitsFromLocalStorageToBackupStorageReply returnValue) {
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                UploadBitsFromLocalStorageToBackupStorageReply reply = new UploadBitsFromLocalStorageToBackupStorageReply();
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final CreateTemporaryVolumeFromSnapshotMsg msg) {
        String hostUuid = getHostUuidByResourceUuid(msg.getSnapshot().getUuid());
        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByHostUuid(hostUuid);
        LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, hostUuid, new ReturnValueCompletion<CreateTemporaryVolumeFromSnapshotReply>() {
            @Override
            public void success(CreateTemporaryVolumeFromSnapshotReply returnValue) {
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                CreateTemporaryVolumeFromSnapshotReply reply = new CreateTemporaryVolumeFromSnapshotReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(LocalStorageHypervisorSpecificMessage msg) {
        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByHostUuid(msg.getHostUuid());
        LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handleHypervisorSpecificMessage(msg);
    }

    private void handle(LocalStorageReturnHostCapacityMsg msg) {
        LocalStorageReturnHostCapacityReply reply = new LocalStorageReturnHostCapacityReply();
        long size = msg.isNoOverProvisioning() ? msg.getSize() : ratioMgr.calculateByRatio(self.getUuid(), msg.getSize());
        returnCapacityToHost(msg.getHostUuid(), size);
        bus.reply(msg, reply);
    }

    private void handle(LocalStorageReserveHostCapacityMsg msg) {
        LocalStorageReserveHostCapacityReply reply = new LocalStorageReserveHostCapacityReply();
        long size = msg.isNoOverProvisioning() ? msg.getSize() : ratioMgr.calculateByRatio(self.getUuid(), msg.getSize());
        reserveCapacityOnHost(msg.getHostUuid(), size);
        bus.reply(msg, reply);
    }

    private void handle(final LocalStorageDirectlyDeleteBitsMsg msg) {
        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByHostUuid(msg.getHostUuid());
        LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, msg.getHostUuid(), new ReturnValueCompletion<LocalStorageDirectlyDeleteBitsReply>(msg) {
            @Override
            public void success(LocalStorageDirectlyDeleteBitsReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                LocalStorageDirectlyDeleteBitsReply reply = new LocalStorageDirectlyDeleteBitsReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final LocalStorageCreateEmptyVolumeMsg msg) {
        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByHostUuid(msg.getHostUuid());
        LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<LocalStorageCreateEmptyVolumeReply>() {
            @Override
            public void success(LocalStorageCreateEmptyVolumeReply returnValue) {
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                LocalStorageCreateEmptyVolumeReply reply = new LocalStorageCreateEmptyVolumeReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(APIGetLocalStorageHostDiskCapacityMsg msg) {
        APIGetLocalStorageHostDiskCapacityReply reply = new APIGetLocalStorageHostDiskCapacityReply();
        if (msg.getHostUuid() != null) {
            SimpleQuery<LocalStorageHostRefVO> q = dbf.createQuery(LocalStorageHostRefVO.class);
            q.add(LocalStorageHostRefVO_.primaryStorageUuid, Op.EQ, msg.getPrimaryStorageUuid());
            q.add(LocalStorageHostRefVO_.hostUuid, Op.EQ, msg.getHostUuid());
            LocalStorageHostRefVO ref = q.find();
            if (ref == null) {
                reply.setError(errf.instantiateErrorCode(SysErrors.RESOURCE_NOT_FOUND, String.format("local primary storage[uuid:%s] doesn't have the host[uuid:%s]", self.getUuid(), msg.getHostUuid())));
                bus.reply(msg, reply);
                return;
            }

            HostDiskCapacity c = new HostDiskCapacity();
            c.setHostUuid(msg.getHostUuid());
            c.setTotalCapacity(ref.getTotalCapacity());
            c.setAvailableCapacity(ref.getAvailableCapacity());
            c.setAvailablePhysicalCapacity(ref.getAvailablePhysicalCapacity());
            c.setTotalPhysicalCapacity(ref.getTotalPhysicalCapacity());
            reply.setInventories(list(c));
        } else {
            SimpleQuery<LocalStorageHostRefVO> q = dbf.createQuery(LocalStorageHostRefVO.class);
            q.add(LocalStorageHostRefVO_.primaryStorageUuid, Op.EQ, msg.getPrimaryStorageUuid());
            List<LocalStorageHostRefVO> refs = q.list();

            List<HostDiskCapacity> cs = CollectionUtils.transformToList(refs, new Function<HostDiskCapacity, LocalStorageHostRefVO>() {
                @Override
                public HostDiskCapacity call(LocalStorageHostRefVO ref) {
                    HostDiskCapacity c = new HostDiskCapacity();
                    c.setHostUuid(ref.getHostUuid());
                    c.setTotalCapacity(ref.getTotalCapacity());
                    c.setAvailableCapacity(ref.getAvailableCapacity());
                    c.setAvailablePhysicalCapacity(ref.getAvailablePhysicalCapacity());
                    c.setTotalPhysicalCapacity(ref.getTotalPhysicalCapacity());
                    return c;
                }
            });

            reply.setInventories(cs);
        }

        bus.reply(msg, reply);
    }

    private void handle(final DownloadImageToPrimaryStorageCacheMsg msg) {
        final DownloadImageToPrimaryStorageCacheReply reply = new DownloadImageToPrimaryStorageCacheReply();
        final List<String> hostUuids;
        if (msg.getHostUuid() == null) {
            hostUuids = new Callable<List<String>>() {
                @Override
                @Transactional(readOnly = true)
                public List<String> call() {
                    String sql = "select h.hostUuid" +
                            " from LocalStorageHostRefVO h, HostVO host" +
                            " where h.primaryStorageUuid = :puuid" +
                            " and h.hostUuid = host.uuid" +
                            " and host.status = :hstatus";
                    TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
                    q.setParameter("puuid", self.getUuid());
                    q.setParameter("hstatus", HostStatus.Connected);
                    return q.getResultList();
                }
            }.call();
        } else {
            hostUuids = list(msg.getHostUuid());
        }

        if (hostUuids.isEmpty()) {
            bus.reply(msg, reply);
            return;
        }

        class HostError {
            ErrorCode errorCode;
            String hostUuid;
        }

        class Ret {
            List<HostError> errorCodes = new ArrayList<HostError>();
            String installPath;

            synchronized void addError(HostError err) {
                errorCodes.add(err);
            }
        }

        final Ret ret = new Ret();
        final AsyncLatch latch = new AsyncLatch(hostUuids.size(), new NoErrorCompletion(msg) {
            @Override
            public void done() {
                if (ret.errorCodes.size() == hostUuids.size()) {
                    reply.setError(errf.stringToOperationError(
                            String.format("failed to download image[uuid:%s] to all hosts in the local storage[uuid:%s]" +
                                    ". %s", msg.getImage().getUuid(), self.getUuid(), JSONObjectUtil.toJsonString(ret.errorCodes))
                    ));
                } else if (!ret.errorCodes.isEmpty()) {
                    for (HostError err : ret.errorCodes) {
                        logger.warn(String.format("failed to download image [uuid:%s] to the host[uuid:%s] in the local" +
                                " storage[uuid:%s]. %s", msg.getImage().getUuid(), err.hostUuid, self.getUuid(), err.errorCode));
                    }
                }

                reply.setInstallPath(ret.installPath);
                bus.reply(msg, reply);
            }
        });

        for (final String hostUuid : hostUuids) {
            FlowChain chain = FlowChainBuilder.newShareFlowChain();
            chain.setName(String.format("download-image-%s-to-local-storage-%s-host-%s", msg.getImage().getUuid(), self.getUuid(), hostUuid));
            chain.then(new ShareFlow() {
                @Override
                public void setup() {

                    flow(new NoRollbackFlow() {
                        String __name__ = "download-to-host";

                        @Override
                        public void run(final FlowTrigger trigger, Map data) {
                            LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByHostUuid(hostUuid);
                            LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
                            bkd.downloadImageToCache(msg.getImage(), hostUuid, new ReturnValueCompletion<String>(trigger) {
                                @Override
                                public void success(String returnValue) {
                                    ret.installPath = returnValue;
                                    trigger.next();
                                }

                                @Override
                                public void fail(ErrorCode errorCode) {
                                    trigger.fail(errorCode);
                                }
                            });
                        }
                    });

                    done(new FlowDoneHandler(latch) {
                        @Override
                        public void handle(Map data) {
                            latch.ack();
                        }
                    });

                    error(new FlowErrorHandler(latch) {
                        @Override
                        public void handle(ErrorCode errCode, Map data) {
                            HostError herr = new HostError();
                            herr.errorCode = errCode;
                            herr.hostUuid = hostUuid;
                            ret.addError(herr);
                            latch.ack();
                        }
                    });
                }
            }).start();
        }
    }

    private void handle(final MergeVolumeSnapshotOnPrimaryStorageMsg msg) {
        final String hostUuid = getHostUuidByResourceUuid(msg.getTo().getUuid());
        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByHostUuid(hostUuid);
        LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, hostUuid, new ReturnValueCompletion<MergeVolumeSnapshotOnPrimaryStorageReply>(msg) {
            @Override
            public void success(MergeVolumeSnapshotOnPrimaryStorageReply returnValue) {
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                MergeVolumeSnapshotOnPrimaryStorageReply r = new MergeVolumeSnapshotOnPrimaryStorageReply();
                r.setError(errorCode);
                bus.reply(msg, r);
            }
        });
    }

    private void handle(final CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg msg) {
        final VolumeSnapshotInventory sinv = msg.getSnapshot();
        final String hostUuid = getHostUuidByResourceUuid(sinv.getUuid());
        if (hostUuid == null) {
            throw new OperationFailureException(errf.stringToInternalError(
                    String.format("the volume snapshot[uuid:%s] is not on the local primary storage[uuid: %s]; the local primary storage" +
                            " doesn't support the manner of downloading snapshots and creating the volume", sinv.getUuid(), self.getUuid())
            ));
        }

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("create-volume-%s-from-snapshots", msg.getVolumeUuid()));
        chain.then(new ShareFlow() {
            CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply reply;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "create-volume";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByHostUuid(hostUuid);
                        LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
                        bkd.handle(msg, hostUuid, new ReturnValueCompletion<CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply>(msg) {
                            @Override
                            public void success(CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply returnValue) {
                                reply = returnValue;
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                flow(new Flow() {
                    String __name__ = "reserve-capacity-on-host";

                    Long size;

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        size = reply.getSize();
                        reserveCapacityOnHost(hostUuid, size);
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (size != null) {
                            returnCapacityToHost(hostUuid, size);
                        }

                        trigger.rollback();
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        createResourceRefVO(msg.getVolumeUuid(), VolumeVO.class.getSimpleName(), reply.getSize(), hostUuid);
                        bus.reply(msg, reply);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply reply = new CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply();
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                    }
                });
            }
        }).start();
    }

    private void handle(final BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg msg) {
        String hostUuid = getHostUuidByResourceUuid(msg.getSnapshot().getUuid());
        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByHostUuid(hostUuid);
        LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, hostUuid, new ReturnValueCompletion<BackupVolumeSnapshotFromPrimaryStorageToBackupStorageReply>(msg) {
            @Override
            public void success(BackupVolumeSnapshotFromPrimaryStorageToBackupStorageReply returnValue) {
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                BackupVolumeSnapshotFromPrimaryStorageToBackupStorageReply reply = new BackupVolumeSnapshotFromPrimaryStorageToBackupStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final RevertVolumeFromSnapshotOnPrimaryStorageMsg msg) {
        final RevertVolumeFromSnapshotOnPrimaryStorageReply reply = new RevertVolumeFromSnapshotOnPrimaryStorageReply();

        String hostUuid = getHostUuidByResourceUuid(msg.getSnapshot().getUuid());
        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByHostUuid(hostUuid);
        LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, hostUuid, new ReturnValueCompletion<RevertVolumeFromSnapshotOnPrimaryStorageReply>(msg) {
            @Override
            public void success(RevertVolumeFromSnapshotOnPrimaryStorageReply returnValue) {
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final ReInitRootVolumeFromTemplateOnPrimaryStorageMsg msg) {
        final ReInitRootVolumeFromTemplateOnPrimaryStorageReply reply = new ReInitRootVolumeFromTemplateOnPrimaryStorageReply();

        String hostUuid = getHostUuidByResourceUuid(msg.getVolume().getUuid());
        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByHostUuid(hostUuid);
        LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, hostUuid, new ReturnValueCompletion<ReInitRootVolumeFromTemplateOnPrimaryStorageReply>(msg) {
            @Override
            public void success(ReInitRootVolumeFromTemplateOnPrimaryStorageReply returnValue) {
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    protected String getHostUuidByResourceUuid(String resUuid) {
        SimpleQuery<LocalStorageResourceRefVO> q = dbf.createQuery(LocalStorageResourceRefVO.class);
        q.select(LocalStorageResourceRefVO_.hostUuid);
        q.add(LocalStorageResourceRefVO_.resourceUuid, Op.EQ, resUuid);
        return q.findValue();
    }

    private void handle(final DeleteSnapshotOnPrimaryStorageMsg msg) {
        final String hostUuid = getHostUuidByResourceUuid(msg.getSnapshot().getUuid());

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("delete-snapshot-%s-on-local-storage-%s", msg.getSnapshot().getUuid(), self.getUuid()));
        chain.then(new ShareFlow() {
            DeleteSnapshotOnPrimaryStorageReply reply;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "delete-snapshot-on-host";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByHostUuid(hostUuid);
                        LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
                        bkd.handle(msg, hostUuid, new ReturnValueCompletion<DeleteSnapshotOnPrimaryStorageReply>(trigger) {
                            @Override
                            public void success(DeleteSnapshotOnPrimaryStorageReply returnValue) {
                                reply = returnValue;
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "return-capacity-to-host";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        returnCapacityToHost(hostUuid, msg.getSnapshot().getSize());
                        trigger.next();
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        deleteResourceRefVO(msg.getSnapshot().getUuid());
                        bus.reply(msg, reply);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        DeleteSnapshotOnPrimaryStorageReply reply = new DeleteSnapshotOnPrimaryStorageReply();
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                    }
                });
            }
        }).start();
    }

    private void handle(final TakeSnapshotMsg msg) {
        final VolumeSnapshotInventory sp = msg.getStruct().getCurrent();
        final String hostUuid = getHostUuidByResourceUuid(sp.getVolumeUuid());

        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByHostUuid(hostUuid);
        LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, hostUuid, new ReturnValueCompletion<TakeSnapshotReply>(msg) {
            @Override
            public void success(TakeSnapshotReply returnValue) {
                createResourceRefVO(sp.getUuid(), VolumeSnapshotVO.class.getSimpleName(), returnValue.getInventory().getSize(), hostUuid);
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                TakeSnapshotReply reply = new TakeSnapshotReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Transactional
    private void handle(RemoveHostFromLocalStorageMsg msg) {
        String sqlLocalStorageHostRefVO = "select ref" +
                " from LocalStorageHostRefVO ref" +
                " where hostUuid = :hostUuid" +
                " and primaryStorageUuid = :primaryStorageUuid";
        TypedQuery<LocalStorageHostRefVO> query = dbf.getEntityManager().
                createQuery(sqlLocalStorageHostRefVO, LocalStorageHostRefVO.class);
        query.setParameter("hostUuid", msg.getHostUuid());
        query.setParameter("primaryStorageUuid", msg.getPrimaryStorageUuid());
        LocalStorageHostRefVO ref = query.getSingleResult();
        if (ref == null) {
            return;
        }
        dbf.remove(ref);

        deleteResourceRef(msg.getHostUuid());

        // on remove, subtract the capacity from every capacity
        decreaseCapacity(ref.getTotalCapacity(),
                ref.getAvailableCapacity(),
                ref.getTotalPhysicalCapacity(),
                ref.getAvailablePhysicalCapacity(),
                ref.getSystemUsedCapacity());
        bus.reply(msg, new RemoveHostFromLocalStorageReply());
    }

    void deleteResourceRef(String hostUuid) {
        SimpleQuery<LocalStorageResourceRefVO> rq = dbf.createQuery(LocalStorageResourceRefVO.class);
        rq.add(LocalStorageResourceRefVO_.hostUuid, Op.EQ, hostUuid);
        List<LocalStorageResourceRefVO> refs = rq.list();
        if (refs.isEmpty()) {
            return;
        }

        List<String> volumes = new ArrayList<>();
        List<String> snapshots = new ArrayList<>();
        for (LocalStorageResourceRefVO ref : refs) {
            if (VolumeVO.class.getSimpleName().equals(ref.getResourceType())) {
                volumes.add(ref.getResourceUuid());
            } else if (VolumeSnapshotVO.class.getSimpleName().equals(ref.getResourceType())) {
                snapshots.add(ref.getResourceUuid());
            }
        }

        // delete items in image cache
        UpdateQuery uq = UpdateQuery.New();
        uq.entity(ImageCacheVO.class);
        uq.condAnd(ImageCacheVO_.primaryStorageUuid, Op.EQ, self.getUuid());
        uq.condAnd(ImageCacheVO_.installUrl, Op.LIKE, String.format("%%%s%%", hostUuid));
        uq.delete();

        // delete volumes
        if (!volumes.isEmpty()) {
            uq = UpdateQuery.New();
            uq.entity(VolumeVO.class);
            uq.condAnd(VolumeVO_.uuid, Op.IN, volumes);
            uq.delete();
            logger.debug(String.format("delete volumes%s because the host[uuid:%s] is removed from" +
                    " the local storage[name:%s, uuid:%s]", volumes, hostUuid, self.getName(), self.getUuid()));
        }

        // delete snapshots
        if (!snapshots.isEmpty()) {
            uq = UpdateQuery.New();
            uq.entity(VolumeSnapshotVO.class);
            uq.condAnd(VolumeSnapshotVO_.uuid, Op.IN, snapshots);
            uq.delete();
            logger.debug(String.format("delete volume snapshots%s because the host[uuid:%s] is removed from" +
                    " the local storage[name:%s, uuid:%s]", snapshots, hostUuid, self.getName(), self.getUuid()));
        }

        dbf.removeCollection(refs, LocalStorageResourceRefVO.class);
    }

    protected void handle(final InitPrimaryStorageOnHostConnectedMsg msg) {
        final InitPrimaryStorageOnHostConnectedReply reply = new InitPrimaryStorageOnHostConnectedReply();
        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByHostUuid(msg.getHostUuid());
        final LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<PhysicalCapacityUsage>(msg) {
            @Override
            @Transactional
            public void success(PhysicalCapacityUsage c) {
                String sqlLocalStorageHostRefVO = "select ref" +
                        " from LocalStorageHostRefVO ref" +
                        " where hostUuid = :hostUuid" +
                        " and primaryStorageUuid = :primaryStorageUuid";
                TypedQuery<LocalStorageHostRefVO> query = dbf.getEntityManager().
                        createQuery(sqlLocalStorageHostRefVO, LocalStorageHostRefVO.class);
                query.setParameter("hostUuid", msg.getHostUuid());
                query.setParameter("primaryStorageUuid", msg.getPrimaryStorageUuid());
                List<LocalStorageHostRefVO> refs = query.getResultList();
                LocalStorageHostRefVO ref;
                if (refs == null || refs.isEmpty()) {
                    ref = new LocalStorageHostRefVO();
                    ref.setTotalCapacity(c.totalPhysicalSize);
                    ref.setAvailableCapacity(c.availablePhysicalSize);
                    ref.setTotalPhysicalCapacity(c.totalPhysicalSize);
                    ref.setAvailablePhysicalCapacity(c.availablePhysicalSize);
                    ref.setHostUuid(msg.getHostUuid());
                    ref.setPrimaryStorageUuid(self.getUuid());
                    ref.setSystemUsedCapacity(c.totalPhysicalSize - c.availablePhysicalSize);
                    dbf.persist(ref);

                    increaseCapacity(
                            c.totalPhysicalSize,
                            c.availablePhysicalSize,
                            c.totalPhysicalSize,
                            c.availablePhysicalSize,
                            ref.getSystemUsedCapacity());
                } else {
                    ref = refs.get(0);
                    boolean totalCapacityChanged = false;
                    if (ref.getTotalCapacity() != c.totalPhysicalSize) {
                        totalCapacityChanged = true;
                    }

                    ref.setAvailablePhysicalCapacity(c.availablePhysicalSize);
                    ref.setTotalPhysicalCapacity(c.totalPhysicalSize);
                    ref.setTotalCapacity(c.totalPhysicalSize);
                    dbf.update(ref);

                    if (totalCapacityChanged) {
                        // the host's local storage capacity changed
                        // need to recalculate the capacity in the database
                        RecalculatePrimaryStorageCapacityMsg rmsg = new RecalculatePrimaryStorageCapacityMsg();
                        rmsg.setPrimaryStorageUuid(self.getUuid());
                        bus.makeTargetServiceIdByResourceUuid(rmsg, PrimaryStorageConstant.SERVICE_ID, self.getUuid());
                        bus.send(rmsg);
                    } else {
                        new LocalStorageCapacityRecalculator().calculateByHostUuids(self.getUuid(), list(msg.getHostUuid()));
                    }
                }

                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Transactional
    protected void reserveCapacityOnHost(String hostUuid, long size) {
        String sql = "select ref from LocalStorageHostRefVO ref where ref.hostUuid = :huuid";
        TypedQuery<LocalStorageHostRefVO> q = dbf.getEntityManager().createQuery(sql, LocalStorageHostRefVO.class);
        q.setParameter("huuid", hostUuid);
        q.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        List<LocalStorageHostRefVO> refs = q.getResultList();

        if (refs.isEmpty()) {
            throw new CloudRuntimeException(String.format("cannot find host[uuid: %s] of local primary storage[uuid: %s]",
                    hostUuid, self.getUuid()));
        }


        LocalStorageHostRefVO ref = refs.get(0);

        physicalCapacityMgr.checkCapacityByRatio(
                self.getUuid(),
                ref.getTotalPhysicalCapacity(),
                ref.getAvailablePhysicalCapacity()
        );

        LocalStorageHostCapacityStruct s = new LocalStorageHostCapacityStruct();
        s.setLocalStorage(getSelfInventory());
        s.setHostUuid(ref.getHostUuid());
        s.setSizeBeforeOverProvisioning(size);
        s.setSize(size);

        for (LocalStorageReserveHostCapacityExtensionPoint ext : pluginRgty.getExtensionList(
                LocalStorageReserveHostCapacityExtensionPoint.class)) {
            ext.beforeReserveLocalStorageCapacityOnHost(s);
        }

        long avail = ref.getAvailableCapacity() - s.getSize();
        if (avail < 0) {
            throw new OperationFailureException(errf.stringToOperationError(
                    String.format("host[uuid: %s] of local primary storage[uuid: %s] doesn't have enough capacity" +
                                    "[current: %s bytes, needed: %s]",
                            hostUuid, self.getUuid(), ref.getAvailableCapacity(), size)
            ));
        }

        ref.setAvailableCapacity(avail);
        dbf.getEntityManager().merge(ref);
    }

    @Transactional
    protected void returnCapacityToHost(String hostUuid, long size) {
        String sql = "select ref from LocalStorageHostRefVO ref where ref.hostUuid = :huuid";
        TypedQuery<LocalStorageHostRefVO> q = dbf.getEntityManager().createQuery(sql, LocalStorageHostRefVO.class);
        q.setParameter("huuid", hostUuid);
        q.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        List<LocalStorageHostRefVO> refs = q.getResultList();

        if (refs.isEmpty()) {
            throw new CloudRuntimeException(String.format("cannot find host[uuid: %s] of local primary storage[uuid: %s]",
                    hostUuid, self.getUuid()));
        }

        LocalStorageHostRefVO ref = refs.get(0);

        LocalStorageHostCapacityStruct s = new LocalStorageHostCapacityStruct();
        s.setSizeBeforeOverProvisioning(size);
        s.setHostUuid(hostUuid);
        s.setLocalStorage(getSelfInventory());
        s.setSize(size);

        for (LocalStorageReturnHostCapacityExtensionPoint ext : pluginRgty.getExtensionList(
                LocalStorageReturnHostCapacityExtensionPoint.class)) {
            ext.beforeReturnLocalStorageCapacityOnHost(s);
        }

        ref.setAvailableCapacity(ref.getAvailableCapacity() + s.getSize());
        dbf.getEntityManager().merge(ref);
    }

    @Transactional
    protected void returnCapacityToHostByResourceUuid(String resUuid) {
        String sql = "select href, rref" +
                " from LocalStorageHostRefVO href, LocalStorageResourceRefVO rref" +
                " where href.hostUuid = rref.hostUuid" +
                " and href.primaryStorageUuid = rref.primaryStorageUuid" +
                " and rref.resourceUuid = :resUuid" +
                " and rref.primaryStorageUuid = :puuid";
        TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
        q.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        q.setParameter("resUuid", resUuid);
        q.setParameter("puuid", self.getUuid());
        Tuple twoRefs = q.getSingleResult();

        LocalStorageHostRefVO href = twoRefs.get(0, LocalStorageHostRefVO.class);
        LocalStorageResourceRefVO rref = twoRefs.get(1, LocalStorageResourceRefVO.class);

        long requiredSize = rref.getSize();
        if (VolumeVO.class.getSimpleName().equals(rref.getResourceType())) {
            requiredSize = ratioMgr.calculateByRatio(self.getUuid(), requiredSize);
        }

        LocalStorageHostCapacityStruct s = new LocalStorageHostCapacityStruct();
        s.setSizeBeforeOverProvisioning(rref.getSize());
        s.setHostUuid(href.getHostUuid());
        s.setLocalStorage(getSelfInventory());
        s.setSize(requiredSize);
        for (LocalStorageReturnHostCapacityExtensionPoint ext : pluginRgty.getExtensionList(
                LocalStorageReturnHostCapacityExtensionPoint.class)) {
            ext.beforeReturnLocalStorageCapacityOnHost(s);
        }

        href.setAvailableCapacity(href.getAvailableCapacity() + s.getSize());
        dbf.getEntityManager().merge(href);
    }

    @Override
    protected void handle(final InstantiateVolumeOnPrimaryStorageMsg msg) {
        String hostUuid = msg.getDestHost().getUuid();
        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByHostUuid(hostUuid);
        final LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("instantiate-volume-%s-local-primary-storage-%s", msg.getVolume().getUuid(), self.getUuid()));
        final String finalHostUuid = hostUuid;
        chain.then(new ShareFlow() {
            InstantiateVolumeOnPrimaryStorageReply reply;

            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "allocate-capacity-on-host";

                    long requiredSize = ratioMgr.calculateByRatio(self.getUuid(), msg.getVolume().getSize());
                    long reservedSize;

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        reserveCapacityOnHost(finalHostUuid, requiredSize);
                        reservedSize = requiredSize;
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        if (reservedSize != 0) {
                            returnCapacityToHost(finalHostUuid, reservedSize);
                        }
                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "instantiate-volume-on-host";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        bkd.handle(msg, new ReturnValueCompletion<InstantiateVolumeOnPrimaryStorageReply>(msg) {
                            @Override
                            public void success(InstantiateVolumeOnPrimaryStorageReply returnValue) {
                                reply = returnValue;
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        createResourceRefVO(msg.getVolume().getUuid(), VolumeVO.class.getSimpleName(),
                                msg.getVolume().getSize(), finalHostUuid);
                        bus.reply(msg, reply);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        InstantiateVolumeOnPrimaryStorageReply reply = new InstantiateVolumeOnPrimaryStorageReply();
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                    }
                });
            }
        }).start();
    }

    private void deleteResourceRefVO(String resourceUuid) {
        SimpleQuery<LocalStorageResourceRefVO> q = dbf.createQuery(LocalStorageResourceRefVO.class);
        q.add(LocalStorageResourceRefVO_.primaryStorageUuid, Op.EQ, self.getUuid());
        q.add(LocalStorageResourceRefVO_.resourceUuid, Op.EQ, resourceUuid);
        LocalStorageResourceRefVO ref = q.find();
        dbf.remove(ref);
    }

    private void createResourceRefVO(String resUuid, String resType, long size, String hostUuid) {
        LocalStorageResourceRefVO ref = new LocalStorageResourceRefVO();
        ref.setPrimaryStorageUuid(self.getUuid());
        ref.setSize(size);
        ref.setResourceType(resType);
        ref.setResourceUuid(resUuid);
        ref.setHostUuid(hostUuid);
        dbf.persist(ref);
    }

    @Override
    protected void handle(final DeleteVolumeOnPrimaryStorageMsg msg) {
        SimpleQuery<LocalStorageResourceRefVO> q = dbf.createQuery(LocalStorageResourceRefVO.class);
        q.add(LocalStorageResourceRefVO_.resourceUuid, Op.EQ, msg.getVolume().getUuid());
        q.add(LocalStorageResourceRefVO_.resourceType, Op.EQ, VolumeVO.class.getSimpleName());
        if (!q.isExists()) {
            logger.debug(String.format("volume[uuid:%s] is not on the local storage[uuid:%s, name:%s]," +
                            "the host the volume is on may have been deleted",
                    msg.getVolume().getUuid(), self.getUuid(), self.getName()));

            DeleteVolumeOnPrimaryStorageReply reply = new DeleteVolumeOnPrimaryStorageReply();
            bus.reply(msg, reply);
            return;
        }

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("delete-volume-%s-local-primary-storage-%s", msg.getVolume().getUuid(), self.getUuid()));
        chain.then(new ShareFlow() {
            DeleteVolumeOnPrimaryStorageReply reply;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "delete-volume-on-host";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByResourceUuid(msg.getVolume().getUuid(), VolumeVO.class.getSimpleName());
                        LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
                        bkd.handle(msg, new ReturnValueCompletion<DeleteVolumeOnPrimaryStorageReply>(msg) {
                            @Override
                            public void success(DeleteVolumeOnPrimaryStorageReply returnValue) {
                                reply = returnValue;
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "return-capacity-to-host";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        returnCapacityToHostByResourceUuid(msg.getVolume().getUuid());
                        trigger.next();
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        deleteResourceRefVO(msg.getVolume().getUuid());
                        bus.reply(msg, reply);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        DeleteVolumeOnPrimaryStorageReply reply = new DeleteVolumeOnPrimaryStorageReply();
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                    }
                });
            }
        }).start();
    }

    @Override
    protected void handle(CreateTemplateFromVolumeOnPrimaryStorageMsg msg) {
        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByResourceUuid(msg.getVolumeInventory().getUuid(), VolumeVO.class.getSimpleName());
        LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg);
    }

    @Override
    protected void handle(final DownloadDataVolumeToPrimaryStorageMsg msg) {
        if (msg.getHostUuid() == null) {
            throw new OperationFailureException(errf.stringToOperationError(
                    String.format("unable to create the data volume[uuid: %s] on a local primary storage[uuid:%s], because the hostUuid is not specified.",
                            msg.getVolumeUuid(), self.getUuid())
            ));
        }

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("download-data-volume-%s-to-local-storage-%s", msg.getVolumeUuid(), self.getUuid()));
        chain.then(new ShareFlow() {
            DownloadDataVolumeToPrimaryStorageReply reply;

            long requiredSize = ratioMgr.calculateByRatio(self.getUuid(), msg.getImage().getSize());

            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "allocate-capacity-on-host";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        reserveCapacityOnHost(msg.getHostUuid(), requiredSize);
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        returnCapacityToHost(msg.getHostUuid(), requiredSize);
                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "download-the-data-volume-to-host";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByHostUuid(msg.getHostUuid());
                        LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
                        bkd.handle(msg, new ReturnValueCompletion<DownloadDataVolumeToPrimaryStorageReply>(trigger) {
                            @Override
                            public void success(DownloadDataVolumeToPrimaryStorageReply returnValue) {
                                reply = returnValue;
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        createResourceRefVO(msg.getVolumeUuid(), VolumeVO.class.getSimpleName(), msg.getImage().getSize(), msg.getHostUuid());
                        bus.reply(msg, reply);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        DownloadDataVolumeToPrimaryStorageReply reply = new DownloadDataVolumeToPrimaryStorageReply();
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                    }
                });
            }
        }).start();
    }

    @Override
    protected void handle(final DeleteBitsOnPrimaryStorageMsg msg) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("delete-bits-on-local-primary-storage-%s", self.getUuid()));
        chain.then(new ShareFlow() {
            DeleteBitsOnPrimaryStorageReply reply;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "delete-bits-on-host";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByResourceUuid(msg.getBitsUuid(), msg.getBitsType());
                        LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
                        bkd.handle(msg, new ReturnValueCompletion<DeleteBitsOnPrimaryStorageReply>(msg) {
                            @Override
                            public void success(DeleteBitsOnPrimaryStorageReply returnValue) {
                                reply = returnValue;
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "return-capacity-to-host";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        returnCapacityToHostByResourceUuid(msg.getBitsUuid());
                        trigger.next();
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        deleteResourceRefVO(msg.getBitsUuid());
                        bus.reply(msg, reply);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        DeleteBitsOnPrimaryStorageReply reply = new DeleteBitsOnPrimaryStorageReply();
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                    }
                });
            }
        }).start();

    }

    @Override
    protected void handle(final DownloadIsoToPrimaryStorageMsg msg) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("download-iso-%s-local-primary-storage-%s", msg.getIsoSpec().getInventory().getUuid(), self.getUuid()));
        chain.then(new ShareFlow() {
            DownloadIsoToPrimaryStorageReply reply;

            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "reserve-capacity-on-host";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        reserveCapacityOnHost(msg.getDestHostUuid(), msg.getIsoSpec().getInventory().getActualSize());
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowRollback trigger, Map data) {
                        returnCapacityToHost(msg.getDestHostUuid(), msg.getIsoSpec().getInventory().getActualSize());
                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "download-iso-to-host";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByHostUuid(msg.getDestHostUuid());
                        LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
                        bkd.handle(msg, new ReturnValueCompletion<DownloadIsoToPrimaryStorageReply>(msg) {
                            @Override
                            public void success(DownloadIsoToPrimaryStorageReply returnValue) {
                                reply = returnValue;
                                trigger.next();

                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        SimpleQuery<LocalStorageResourceRefVO> q = dbf.createQuery(LocalStorageResourceRefVO.class);
                        q.add(LocalStorageResourceRefVO_.resourceUuid, Op.EQ, msg.getIsoSpec().getInventory().getUuid());
                        q.add(LocalStorageResourceRefVO_.primaryStorageUuid, Op.EQ, self.getUuid());
                        q.add(LocalStorageResourceRefVO_.resourceType, Op.EQ, ImageVO.class.getSimpleName());
                        if (!q.isExists()) {
                            createResourceRefVO(msg.getIsoSpec().getInventory().getUuid(), ImageVO.class.getSimpleName(),
                                    msg.getIsoSpec().getInventory().getActualSize(), msg.getDestHostUuid());
                        }

                        bus.reply(msg, reply);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        DownloadIsoToPrimaryStorageReply reply = new DownloadIsoToPrimaryStorageReply();
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                    }
                });
            }
        }).start();
    }

    @Override
    protected void handle(final DeleteIsoFromPrimaryStorageMsg msg) {
        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("delete-iso-local-primary-storage-%s", msg.getIsoSpec().getInventory().getUuid()));
        chain.then(new ShareFlow() {
            DeleteIsoFromPrimaryStorageReply reply;

            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "delete-iso-on-host";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByResourceUuid(
                                msg.getIsoSpec().getInventory().getUuid(), ImageVO.class.getSimpleName());
                        LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
                        bkd.handle(msg, new ReturnValueCompletion<DeleteIsoFromPrimaryStorageReply>(msg) {
                            @Override
                            public void success(DeleteIsoFromPrimaryStorageReply returnValue) {
                                reply = returnValue;
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);

                            }
                        });
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "return-capacity-to-host";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        returnCapacityToHostByResourceUuid(msg.getIsoSpec().getInventory().getUuid());
                        trigger.next();
                    }
                });

                done(new FlowDoneHandler(msg) {
                    @Override
                    public void handle(Map data) {
                        deleteResourceRefVO(msg.getIsoSpec().getInventory().getUuid());
                        bus.reply(msg, reply);
                    }
                });

                error(new FlowErrorHandler(msg) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        DeleteIsoFromPrimaryStorageReply reply = new DeleteIsoFromPrimaryStorageReply();
                        reply.setError(errCode);
                        bus.reply(msg, reply);
                    }
                });
            }
        }).start();
    }

    @Override
    protected void handle(AskVolumeSnapshotCapabilityMsg msg) {
        AskVolumeSnapshotCapabilityReply reply = new AskVolumeSnapshotCapabilityReply();
        VolumeSnapshotCapability capability = new VolumeSnapshotCapability();
        capability.setSupport(true);
        capability.setArrangementType(VolumeSnapshotArrangementType.CHAIN);
        reply.setCapability(capability);
        bus.reply(msg, reply);
    }

    @Override
    protected void handle(final SyncVolumeSizeOnPrimaryStorageMsg msg) {
        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByResourceUuid(msg.getVolumeUuid(), VolumeVO.class.getSimpleName());
        LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
        String huuid = getHostUuidByResourceUuid(msg.getVolumeUuid());
        bkd.handle(msg, huuid, new ReturnValueCompletion<SyncVolumeSizeOnPrimaryStorageReply>(msg) {
            @Override
            public void success(SyncVolumeSizeOnPrimaryStorageReply returnValue) {
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                SyncVolumeSizeOnPrimaryStorageReply reply = new SyncVolumeSizeOnPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    protected void setCapacity(Long total, Long avail, Long totalPhysical, Long availPhysical) {
        PrimaryStorageCapacityUpdater updater = new PrimaryStorageCapacityUpdater(self.getUuid());
        updater.update(total, avail, totalPhysical, availPhysical);
    }

    void increaseCapacity(final Long total,
                          final Long avail,
                          final Long totalPhysical,
                          final Long availPhysical,
                          final Long sysmtemUsed) {
        PrimaryStorageCapacityUpdater updater = new PrimaryStorageCapacityUpdater(self.getUuid());
        updater.run(new PrimaryStorageCapacityUpdaterRunnable() {
            @Override
            public PrimaryStorageCapacityVO call(PrimaryStorageCapacityVO cap) {
                if (total != null) {
                    cap.setTotalCapacity(cap.getTotalCapacity() + total);
                }
                if (avail != null) {
                    cap.setAvailableCapacity(cap.getAvailableCapacity() + avail);
                }
                if (totalPhysical != null) {
                    cap.setTotalPhysicalCapacity(cap.getTotalPhysicalCapacity() + totalPhysical);
                }
                if (availPhysical != null) {
                    cap.setAvailablePhysicalCapacity(cap.getAvailablePhysicalCapacity() + availPhysical);
                }
                if (sysmtemUsed != null) {
                    if (cap.getSystemUsedCapacity() == null) {
                        cap.setSystemUsedCapacity(0L);
                    }

                    cap.setSystemUsedCapacity(cap.getSystemUsedCapacity() + sysmtemUsed);
                }
                return cap;
            }
        });
    }

    void decreaseCapacity(final Long total,
                          final Long avail,
                          final Long totalPhysical,
                          final Long availPhysical,
                          final Long systemUsed) {
        PrimaryStorageCapacityUpdater updater = new PrimaryStorageCapacityUpdater(self.getUuid());
        updater.run(new PrimaryStorageCapacityUpdaterRunnable() {
            @Override
            public PrimaryStorageCapacityVO call(PrimaryStorageCapacityVO cap) {
                if (total != null) {
                    long t = cap.getTotalCapacity() - total;
                    cap.setTotalCapacity(t < 0 ? 0 : t);
                }
                if (avail != null) {
                    // for over-provisioning scenarios, minus value of available capacity is permitted
                    long a = cap.getAvailableCapacity() - avail;
                    cap.setAvailableCapacity(a);
                }
                if (totalPhysical != null) {
                    long tp = cap.getTotalPhysicalCapacity() - totalPhysical;
                    cap.setTotalPhysicalCapacity(tp < 0 ? 0 : tp);
                }
                if (availPhysical != null) {
                    long ap = cap.getAvailablePhysicalCapacity() - availPhysical;
                    cap.setAvailablePhysicalCapacity(ap < 0 ? 0 : ap);
                }
                if (systemUsed != null) {
                    long su = cap.getSystemUsedCapacity() - systemUsed;
                    cap.setSystemUsedCapacity(su < 0 ? 0 : su);
                }
                return cap;
            }
        });
    }


    @Transactional(readOnly = true)
    protected List<FactoryCluster> getAllFactoriesForAttachedClusters() {
        String sql = "select cluster" +
                " from ClusterVO cluster, PrimaryStorageClusterRefVO ref" +
                " where ref.clusterUuid = cluster.uuid" +
                " and ref.primaryStorageUuid = :uuid";
        TypedQuery<ClusterVO> q = dbf.getEntityManager().createQuery(sql, ClusterVO.class);
        q.setParameter("uuid", self.getUuid());
        List<ClusterVO> clusters = q.getResultList();

        if (clusters.isEmpty()) {
            return new ArrayList<>();
        }

        Map<String, FactoryCluster> m = new HashMap<>();
        for (ClusterVO c : clusters) {
            FactoryCluster fc = m.get(c.getHypervisorType());
            if (fc == null) {
                fc = new FactoryCluster();
                fc.factory = getHypervisorBackendFactory(c.getHypervisorType());
                fc.clusters = new ArrayList<>();
                m.put(c.getHypervisorType(), fc);
            }

            fc.clusters.add(ClusterInventory.valueOf(c));
        }

        List<FactoryCluster> fcs = new ArrayList<>();
        fcs.addAll(m.values());
        return fcs;
    }

    @Override
    protected void connectHook(final ConnectParam param, final Completion completion) {
        RecalculatePrimaryStorageCapacityMsg rmsg = new RecalculatePrimaryStorageCapacityMsg();
        rmsg.setPrimaryStorageUuid(self.getUuid());
        bus.makeLocalServiceId(rmsg, PrimaryStorageConstant.SERVICE_ID);
        bus.send(rmsg);
        completion.success();
    }

    @Override
    protected void pingHook(Completion completion) {
        completion.success();
    }

    @Override
    final protected void syncPhysicalCapacity(final ReturnValueCompletion<PhysicalCapacityUsage> completion) {
        final List<FactoryCluster> fs = getAllFactoriesForAttachedClusters();
        class Sync {
            long total = 0;
            long avail = 0;
            Iterator<FactoryCluster> it = fs.iterator();

            void sync() {
                if (!it.hasNext()) {
                    PhysicalCapacityUsage ret = new PhysicalCapacityUsage();
                    ret.totalPhysicalSize = total;
                    ret.availablePhysicalSize = avail;
                    completion.success(ret);
                    return;
                }

                FactoryCluster fc = it.next();
                LocalStorageHypervisorBackend bkd = fc.factory.getHypervisorBackend(self);
                bkd.syncPhysicalCapacityInCluster(fc.clusters, new ReturnValueCompletion<PhysicalCapacityUsage>(completion) {
                    @Override
                    public void success(PhysicalCapacityUsage returnValue) {
                        total += returnValue.totalPhysicalSize;
                        avail += returnValue.availablePhysicalSize;
                        sync();
                    }

                    @Override
                    public void fail(ErrorCode errorCode) {
                        completion.fail(errorCode);
                    }
                });
            }
        }

        new Sync().sync();
    }

    protected LocalStorageHypervisorFactory getHypervisorBackendFactoryByHostUuid(String hostUuid) {
        SimpleQuery<HostVO> q = dbf.createQuery(HostVO.class);
        q.select(HostVO_.hypervisorType);
        q.add(HostVO_.uuid, Op.EQ, hostUuid);
        String hvType = q.findValue();
        return getHypervisorBackendFactory(hvType);
    }

    @Transactional(readOnly = true)
    private LocalStorageHypervisorFactory getHypervisorBackendFactoryByResourceUuid(String resUuid, String resourceType) {
        String sql = "select host.hypervisorType" +
                " from HostVO host, LocalStorageResourceRefVO ref" +
                " where ref.hostUuid = host.uuid" +
                " and ref.resourceUuid = :resUuid" +
                " and ref.primaryStorageUuid = :puuid";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("resUuid", resUuid);
        q.setParameter("puuid", self.getUuid());
        List<String> ret = q.getResultList();
        if (ret.isEmpty()) {
            throw new CloudRuntimeException(
                    String.format("resource[uuid:%s, type: %s] is not on the local primary storage[uuid:%s]",
                            resUuid, resourceType, self.getUuid()));
        }
        if (ret.size() != 1) {
            throw new CloudRuntimeException(
                    String.format("resource[uuid:%s, type: %s] on the local primary storage[uuid:%s] maps to multiple hypervisor%s",
                            resUuid, resourceType, self.getUuid(), ret));
        }

        String hvType = ret.get(0);
        return getHypervisorBackendFactory(hvType);
    }

    private LocalStorageHypervisorFactory getHypervisorBackendFactory(String hvType) {
        if (hvType == null) {
            throw new CloudRuntimeException("hvType is null!!!");
        }
        for (LocalStorageHypervisorFactory f : pluginRgty.getExtensionList(LocalStorageHypervisorFactory.class)) {
            if (hvType.equals(f.getHypervisorType())) {
                return f;
            }
        }

        throw new CloudRuntimeException(String.format("cannot find LocalStorageHypervisorFactory with hypervisorType[%s]", hvType));
    }

    @Override
    public void attachHook(final String clusterUuid, Completion completion) {
        SimpleQuery<ClusterVO> q = dbf.createQuery(ClusterVO.class);
        q.select(ClusterVO_.hypervisorType);
        q.add(ClusterVO_.uuid, Op.EQ, clusterUuid);
        String hvType = q.findValue();

        LocalStorageHypervisorFactory f = getHypervisorBackendFactory(hvType);
        LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.attachHook(clusterUuid, completion);
    }
}
