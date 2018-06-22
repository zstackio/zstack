package org.zstack.storage.primary.smp;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.asyncbatch.AsyncBatchRunner;
import org.zstack.core.asyncbatch.LoopAsyncBatch;
import org.zstack.core.cloudbus.AutoOffEventCallback;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.notification.N;
import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.cluster.ClusterConnectionStatus;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.core.Completion;
import org.zstack.header.core.FutureCompletion;
import org.zstack.header.core.NoErrorCompletion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.*;
import org.zstack.header.message.Message;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.primary.VolumeSnapshotCapability.VolumeSnapshotArrangementType;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.volume.VolumeFormat;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.kvm.KVMConstant;
import org.zstack.storage.primary.PrimaryStorageBase;
import org.zstack.storage.primary.PrimaryStorageCapacityUpdater;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.TypedQuery;
import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.operr;

/**
 * Created by xing5 on 2016/3/26.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class SMPPrimaryStorageBase extends PrimaryStorageBase {
    private static final CLogger logger = Utils.getLogger(SMPPrimaryStorageBase.class);

    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private SMPPrimaryStorageImageCacheCleaner imageCacheCleaner;

    public SMPPrimaryStorageBase() {
    }

    public SMPPrimaryStorageBase(PrimaryStorageVO self) {
        super(self);
    }

    private HypervisorFactory getHypervisorFactoryByHypervisorType(String hvType) {
        for (HypervisorFactory f : pluginRgty.getExtensionList(HypervisorFactory.class)) {
            if (hvType.equals(f.getHypervisorType())) {
                return f;
            }
        }

        throw new CloudRuntimeException(String.format("cannot find HypervisorFactory[type = %s]", hvType));
    }

    protected HypervisorFactory getHypervisorFactoryByHostUuid(String huuid) {
        SimpleQuery<HostVO> q = dbf.createQuery(HostVO.class);
        q.select(HostVO_.hypervisorType);
        q.add(HostVO_.uuid, Op.EQ, huuid);
        String hvType = q.findValue();
        return getHypervisorFactoryByHypervisorType(hvType);
    }

    protected HypervisorFactory getHypervisorFactoryByClusterUuid(String cuuid) {
        SimpleQuery<ClusterVO> q = dbf.createQuery(ClusterVO.class);
        q.select(ClusterVO_.hypervisorType);
        q.add(ClusterVO_.uuid, Op.EQ, cuuid);
        String hvType = q.findValue();

        return getHypervisorFactoryByHypervisorType(hvType);
    }

    @Override
    public void attachHook(String clusterUuid, final Completion completion) {
        HypervisorBackend bkd = getHypervisorFactoryByClusterUuid(clusterUuid).getHypervisorBackend(self);
        bkd.attachHook(clusterUuid, completion);
    }

    @Override
    protected void handle(GetPrimaryStorageFolderListMsg msg) {
        GetPrimaryStorageFolderListReply reply = new GetPrimaryStorageFolderListReply();
        String hostUuid = getAvailableHostUuidForOperation();
        if (hostUuid == null) {
            bus.reply(msg, reply);
            return;
        }
        String type = Q.New(HostVO.class).eq(HostVO_.uuid, hostUuid).select(HostVO_.hypervisorType).findValue();
        HypervisorFactory f = getHypervisorFactoryByHypervisorType(type);
        final HypervisorBackend bkd = f.getHypervisorBackend(self);

        bkd.handle(msg, new ReturnValueCompletion<GetPrimaryStorageFolderListReply>(msg) {
            @Override
            public void success(GetPrimaryStorageFolderListReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(final InstantiateVolumeOnPrimaryStorageMsg msg) {
        if (msg.getDestHost() == null) {
            String hostUuid = getAvailableHostUuidForOperation();
            if (hostUuid == null) {
                throw new OperationFailureException(operr("the shared mount point primary storage[uuid:%s, name:%s] cannot find any " +
                                "available host in attached clusters for instantiating the volume", self.getUuid(), self.getName()));
            }

            msg.setDestHost(HostInventory.valueOf(dbf.findByUuid(hostUuid, HostVO.class)));
        }

        HypervisorFactory f = getHypervisorFactoryByHostUuid(msg.getDestHost().getUuid());
        HypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<InstantiateVolumeOnPrimaryStorageReply>(msg) {
            @Override
            public void success(InstantiateVolumeOnPrimaryStorageReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                InstantiateVolumeOnPrimaryStorageReply reply = new InstantiateVolumeOnPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(final DeleteVolumeOnPrimaryStorageMsg msg) {
        HypervisorType type = VolumeFormat.getMasterHypervisorTypeByVolumeFormat(msg.getVolume().getFormat());
        HypervisorFactory f = getHypervisorFactoryByHypervisorType(type.toString());
        final HypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<DeleteVolumeOnPrimaryStorageReply>(msg) {
            @Override
            public void success(DeleteVolumeOnPrimaryStorageReply reply) {
                logger.debug( String.format("successfully delete volume[uuid:%s]", msg.getVolume().getUuid()));
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                logger.debug( String.format("can't delete volume[uuid:%s] right now, add a GC job", msg.getVolume().getUuid()));
                SMPDeleteVolumeGC gc = new SMPDeleteVolumeGC();
                gc.NAME = String.format("gc-smp-%s-volume-%s", self.getUuid(), msg.getVolume());
                gc.primaryStorageUuid = self.getUuid();
                gc.hypervisorType = type.toString();
                gc.volume = msg.getVolume();
                gc.submit(SMPPrimaryStorageGlobalConfig.GC_INTERVAL.value(Long.class), TimeUnit.SECONDS);

                DeleteVolumeOnPrimaryStorageReply reply = new DeleteVolumeOnPrimaryStorageReply();
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(DeleteBitsOnPrimaryStorageMsg msg) {
        final DeleteBitsOnPrimaryStorageReply reply = new DeleteBitsOnPrimaryStorageReply();
        String hostUuid = getAvailableHostUuidForOperation();
        if (hostUuid == null) {
            bus.reply(msg, reply);
            return;
        }
        String type = Q.New(HostVO.class).eq(HostVO_.uuid, hostUuid).select(HostVO_.hypervisorType).findValue();
        HypervisorFactory f = getHypervisorFactoryByHypervisorType(type);
        final HypervisorBackend bkd = f.getHypervisorBackend(self);

        bkd.handle(msg, new ReturnValueCompletion<DeleteBitsOnPrimaryStorageReply>(msg) {
            @Override
            public void success(DeleteBitsOnPrimaryStorageReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(final CreateTemplateFromVolumeOnPrimaryStorageMsg msg) {
        HypervisorType type = VolumeFormat.getMasterHypervisorTypeByVolumeFormat(msg.getVolumeInventory().getFormat());
        HypervisorFactory f = getHypervisorFactoryByHypervisorType(type.toString());
        HypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<CreateTemplateFromVolumeOnPrimaryStorageReply>(msg) {
            @Override
            public void success(CreateTemplateFromVolumeOnPrimaryStorageReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                CreateTemplateFromVolumeOnPrimaryStorageReply reply = new CreateTemplateFromVolumeOnPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(final DownloadDataVolumeToPrimaryStorageMsg msg) {
        HypervisorType type = VolumeFormat.getMasterHypervisorTypeByVolumeFormat(msg.getImage().getFormat());
        HypervisorFactory f = getHypervisorFactoryByHypervisorType(type.toString());
        HypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<DownloadDataVolumeToPrimaryStorageReply>(msg) {
            @Override
            public void success(DownloadDataVolumeToPrimaryStorageReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                DownloadDataVolumeToPrimaryStorageReply reply = new DownloadDataVolumeToPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(GetInstallPathForDataVolumeDownloadMsg msg) {
        HypervisorType type = VolumeFormat.getMasterHypervisorTypeByVolumeFormat(msg.getImage().getFormat());
        HypervisorFactory f = getHypervisorFactoryByHypervisorType(type.toString());
        HypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<GetInstallPathForDataVolumeDownloadReply>(msg) {
            @Override
            public void success(GetInstallPathForDataVolumeDownloadReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                GetInstallPathForDataVolumeDownloadReply reply = new GetInstallPathForDataVolumeDownloadReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(final DeleteVolumeBitsOnPrimaryStorageMsg msg) {
        HypervisorFactory f = getHypervisorFactoryByHypervisorType(msg.getHypervisorType());
        HypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<DeleteVolumeBitsOnPrimaryStorageReply>(msg) {
            @Override
            public void success(DeleteVolumeBitsOnPrimaryStorageReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                DeleteVolumeBitsOnPrimaryStorageReply reply = new DeleteVolumeBitsOnPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(final DownloadIsoToPrimaryStorageMsg msg) {
        HypervisorFactory f = getHypervisorFactoryByHostUuid(msg.getDestHostUuid());
        HypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<DownloadIsoToPrimaryStorageReply>(msg) {
            @Override
            public void success(DownloadIsoToPrimaryStorageReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode error) {
                DownloadIsoToPrimaryStorageReply reply = new DownloadIsoToPrimaryStorageReply();
                reply.setError(error);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(final DeleteIsoFromPrimaryStorageMsg msg) {
        HypervisorType type = VolumeFormat.getMasterHypervisorTypeByVolumeFormat(msg.getIsoSpec().getInventory().getFormat());
        HypervisorFactory f = getHypervisorFactoryByHypervisorType(type.toString());
        HypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<DeleteIsoFromPrimaryStorageReply>(msg) {
            @Override
            public void success(DeleteIsoFromPrimaryStorageReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode error) {
                DeleteIsoFromPrimaryStorageReply reply = new DeleteIsoFromPrimaryStorageReply();
                reply.setError(error);
                bus.reply(msg, reply);
            }
        });
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
        SimpleQuery<VolumeVO> q = dbf.createQuery(VolumeVO.class);
        q.select(VolumeVO_.format);
        q.add(VolumeVO_.uuid, Op.EQ, msg.getVolumeUuid());
        String format = q.findValue();

        HypervisorType type = VolumeFormat.getMasterHypervisorTypeByVolumeFormat(format);
        HypervisorFactory f = getHypervisorFactoryByHypervisorType(type.toString());
        HypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<SyncVolumeSizeOnPrimaryStorageReply>(msg) {
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

    protected void hookToKVMHostConnectedEventToChangeStatusToConnected(){
        // hook on host connected event to reconnect the primary storage once there is
        // a host connected in attached clusters
        evtf.onLocal(HostCanonicalEvents.HOST_STATUS_CHANGED_PATH, new AutoOffEventCallback() {
            {
                uniqueIdentity = String.format("connect-smp-%s-when-host-connected", self.getUuid());
            }

            @Override
            protected boolean run(Map tokens, Object data) {
                HostCanonicalEvents.HostStatusChangedData d = (HostCanonicalEvents.HostStatusChangedData) data;
                if (!HostStatus.Connected.toString().equals(d.getNewStatus())) {
                    return false;
                }

                if (!KVMConstant.KVM_HYPERVISOR_TYPE.equals(d.getInventory().getHypervisorType())) {
                    return false;
                }

                self = dbf.reload(self);
                if (self.getStatus() == PrimaryStorageStatus.Connected) {
                    return true;
                }

                if (!self.getAttachedClusterRefs().stream()
                        .anyMatch(ref -> ref.getClusterUuid().equals(d.getInventory().getClusterUuid()))) {
                    return false;
                }

                FutureCompletion future = new FutureCompletion(null);

                ConnectParam p = new ConnectParam();
                p.setNewAdded(false);
                connectHook(p, future);

                future.await();

                if (!future.isSuccess()) {
                    N.New(PrimaryStorageVO.class, self.getUuid()).warn_("unable to reconnect the primary storage[uuid:%s, name:%s], %s",
                            self.getUuid(), self.getName(), future.getErrorCode());
                } else {
                    changeStatus(PrimaryStorageStatus.Connected);
                }

                return future.isSuccess();
            }
        });
    }

    @Override
    protected void connectHook(ConnectParam param, final Completion completion) {
        List<String> clusterUuids = self.getAttachedClusterRefs().stream()
                .map(PrimaryStorageClusterRefVO::getClusterUuid)
                .collect(Collectors.toList());

        if (!clusterUuids.isEmpty()) {
            clusterUuids = SQL.New("select cluster.uuid from ClusterVO cluster, HostVO host" +
                    " where cluster.uuid = host.clusterUuid" +
                    " and host.status = :hostStatus" +
                    " and cluster.uuid in (:cuuids)", String.class)
                    .param("hostStatus", HostStatus.Connected)
                    .param("cuuids", clusterUuids)
                    .list();
        }

        if (clusterUuids.isEmpty()){
            if (!param.isNewAdded()){
                hookToKVMHostConnectedEventToChangeStatusToConnected();
            }

            completion.fail(errf.instantiateErrorCode(PrimaryStorageErrors.DISCONNECTED,
                    String.format("the SMP primary storage[uuid:%s, name:%s] has not attached to any clusters, " +
                            "or no hosts in the attached clusters are connected", self.getUuid(), self.getName())
            ));
            return;
        }

        final List<String> finalClusterUuids = clusterUuids;
        new LoopAsyncBatch<String>(completion) {
            boolean success;

            @Override
            protected Collection<String> collect() {
                return finalClusterUuids;
            }

            @Override
            protected AsyncBatchRunner forEach(String item) {
                return new AsyncBatchRunner() {
                    @Override
                    public void run(NoErrorCompletion completion) {
                        HypervisorBackend bkd = getHypervisorFactoryByClusterUuid(item).getHypervisorBackend(self);
                        bkd.connectByClusterUuid(item, new ReturnValueCompletion<ClusterConnectionStatus>(completion) {
                            @Override
                            public void success(ClusterConnectionStatus clusterStatus) {
                                // isConnectedHostInCluster has been checked before
                                success = true;
                                completion.done();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                errors.add(errorCode);
                                completion.done();
                            }
                        });
                    }
                };
            }

            @Override
            protected void done() {
                if (success) {
                    completion.success();
                } else {
                    completion.fail(errf.stringToOperationError(
                            String.format("failed to connect to all clusters%s", finalClusterUuids), errors
                    ));
                }
            }
        }.start();
    }

    @Override
    protected void pingHook(Completion completion) {
        completion.success();
    }

    @Override
    protected void syncPhysicalCapacity(ReturnValueCompletion<PhysicalCapacityUsage> completion) {
        completion.fail(operr("not supported operation"));
    }

    @Override
    public void handleLocalMessage(Message msg) {
        if (msg instanceof TakeSnapshotMsg) {
            handle((TakeSnapshotMsg) msg);
        } else if (msg instanceof BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg) {
            handle((BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg) msg);
        } else if (msg instanceof CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg) {
            handle((CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg) msg);
        } else if (msg instanceof SMPPrimaryStorageHypervisorSpecificMessage) {
            handle((SMPPrimaryStorageHypervisorSpecificMessage) msg);
        } else if (msg instanceof UploadBitsToBackupStorageMsg) {
            handle((UploadBitsToBackupStorageMsg) msg);
        } else if (msg instanceof CreateTemporaryVolumeFromSnapshotMsg) {
            handle((CreateTemporaryVolumeFromSnapshotMsg) msg);
        } else if (msg instanceof SMPRecalculatePrimaryStorageCapacityMsg) {
            handle((SMPRecalculatePrimaryStorageCapacityMsg) msg);
        } else if (msg instanceof DeleteImageCacheOnPrimaryStorageMsg) {
            handle((DeleteImageCacheOnPrimaryStorageMsg) msg);
        } else {
            super.handleLocalMessage(msg);
        }
    }

    private void handle(final DeleteImageCacheOnPrimaryStorageMsg msg) {
        DeleteImageCacheOnPrimaryStorageReply sreply = new DeleteImageCacheOnPrimaryStorageReply();

        FlowChain chain = FlowChainBuilder.newSimpleFlowChain();
        chain.setName(String.format("delete-image-cache-on-smp-primary-storage-%s", msg.getPrimaryStorageUuid()));
        chain.then(new NoRollbackFlow() {
            String __name__ = "delete-volume-cache";
            @Override
            public void run(FlowTrigger trigger, Map data) {
                String hostUuid = getAvailableHostUuidForOperation();
                if (hostUuid == null) {
                    trigger.next();
                    return;
                }
                HostVO hvo = dbf.findByUuid(hostUuid, HostVO.class);
                DeleteVolumeBitsOnPrimaryStorageMsg dmsg = new DeleteVolumeBitsOnPrimaryStorageMsg();
                dmsg.setFolder(true);
                dmsg.setHypervisorType(hvo.getHypervisorType());
                dmsg.setInstallPath(new File(msg.getInstallPath()).getParent());
                dmsg.setPrimaryStorageUuid(msg.getPrimaryStorageUuid());
                bus.makeTargetServiceIdByResourceUuid(dmsg, PrimaryStorageConstant.SERVICE_ID, msg.getPrimaryStorageUuid());
                bus.send(dmsg, new CloudBusCallBack(trigger) {
                    @Override
                    public void run(MessageReply reply) {
                        if (reply.isSuccess()) {
                            trigger.next();
                        } else {
                            trigger.fail(reply.getError());
                        }
                    }
                });
            }
        }).done(new FlowDoneHandler(msg) {
            @Override
            public void handle(Map data) {
                bus.reply(msg, sreply);
            }
        }).error(new FlowErrorHandler(msg) {
            @Override
            public void handle(ErrorCode errCode, Map data) {
                sreply.setError(errCode);
                bus.reply(msg, sreply);
            }
        }).start();
    }

    @Override
    protected void handle(APICleanUpImageCacheOnPrimaryStorageMsg msg) {
        APICleanUpImageCacheOnPrimaryStorageEvent evt = new APICleanUpImageCacheOnPrimaryStorageEvent(msg.getId());
        imageCacheCleaner.cleanup(msg.getUuid());
        bus.publish(evt);
    }

    protected void handle(SMPRecalculatePrimaryStorageCapacityMsg msg) {
        if (msg.isRelease()) {
            doReleasePrimaryStorageCapacity();
        } else {
            RecalculatePrimaryStorageCapacityMsg rmsg = new RecalculatePrimaryStorageCapacityMsg();
            rmsg.setPrimaryStorageUuid(self.getUuid());
            bus.makeLocalServiceId(rmsg, PrimaryStorageConstant.SERVICE_ID);
            bus.send(rmsg);
        }
    }

    private void doReleasePrimaryStorageCapacity() {
        PrimaryStorageCapacityUpdater updater = new PrimaryStorageCapacityUpdater(self.getUuid());
        updater.run(new PrimaryStorageCapacityUpdaterRunnable() {
            @Override
            public PrimaryStorageCapacityVO call(PrimaryStorageCapacityVO cap) {
                cap.setAvailableCapacity(0L);
                cap.setTotalCapacity(0L);
                cap.setTotalPhysicalCapacity(0L);
                cap.setAvailablePhysicalCapacity(0L);
                cap.setSystemUsedCapacity(0L);
                return cap;
            }
        });
    }

    private void handle(final CreateTemporaryVolumeFromSnapshotMsg msg) {
        HypervisorFactory f = getHypervisorFactoryByHypervisorType(msg.getHypervisorType());
        HypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<CreateTemporaryVolumeFromSnapshotReply>(msg) {
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

    private void handle(final UploadBitsToBackupStorageMsg msg) {
        HypervisorFactory f = getHypervisorFactoryByHypervisorType(msg.getHypervisorType());
        HypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<UploadBitsToBackupStorageReply>(msg) {
            @Override
            public void success(UploadBitsToBackupStorageReply reply) {
                bus.reply(msg, reply);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                UploadBitsToBackupStorageReply reply = new UploadBitsToBackupStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(SMPPrimaryStorageHypervisorSpecificMessage msg) {
        HypervisorFactory f = getHypervisorFactoryByHypervisorType(msg.getHypervisorType());
        HypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handleHypervisorSpecificMessage(msg);
    }

    protected void handle(final MergeVolumeSnapshotOnPrimaryStorageMsg msg) {
        HypervisorBackend bkd = getHypervisorBackendByVolumeUuid(msg.getTo().getUuid());
        bkd.handle(msg, new ReturnValueCompletion<MergeVolumeSnapshotOnPrimaryStorageReply>(msg) {
            @Override
            public void success(MergeVolumeSnapshotOnPrimaryStorageReply returnValue) {
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                MergeVolumeSnapshotOnPrimaryStorageReply reply = new MergeVolumeSnapshotOnPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(final CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg msg) {
        HypervisorBackend bkd = getHypervisorBackendByVolumeUuid(msg.getSnapshot().getVolumeUuid());
        bkd.handle(msg, new ReturnValueCompletion<CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply>(msg) {
            @Override
            public void success(CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply returnValue) {
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply reply = new CreateVolumeFromVolumeSnapshotOnPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    private void handle(BackupVolumeSnapshotFromPrimaryStorageToBackupStorageMsg msg) {
        HypervisorBackend bkd = getHypervisorBackendByVolumeUuid(msg.getSnapshot().getVolumeUuid());
        bkd.handle(msg, new ReturnValueCompletion<BackupVolumeSnapshotFromPrimaryStorageToBackupStorageReply>(msg) {
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

    protected void handle(final RevertVolumeFromSnapshotOnPrimaryStorageMsg msg) {
        HypervisorBackend bkd = getHypervisorBackendByVolumeUuid(msg.getVolume().getUuid());
        bkd.handle(msg, new ReturnValueCompletion<RevertVolumeFromSnapshotOnPrimaryStorageReply>(msg) {
            @Override
            public void success(RevertVolumeFromSnapshotOnPrimaryStorageReply returnValue) {
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                RevertVolumeFromSnapshotOnPrimaryStorageReply reply = new RevertVolumeFromSnapshotOnPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    protected void handle(final ReInitRootVolumeFromTemplateOnPrimaryStorageMsg msg) {
        HypervisorBackend bkd = getHypervisorBackendByVolumeUuid(msg.getVolume().getUuid());
        bkd.handle(msg, new ReturnValueCompletion<ReInitRootVolumeFromTemplateOnPrimaryStorageReply>(msg) {
            @Override
            public void success(ReInitRootVolumeFromTemplateOnPrimaryStorageReply returnValue) {
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                ReInitRootVolumeFromTemplateOnPrimaryStorageReply reply = new ReInitRootVolumeFromTemplateOnPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    @Override
    protected void handle(final DeleteSnapshotOnPrimaryStorageMsg msg) {
        HypervisorBackend bkd = getHypervisorBackendByVolumeUuid(msg.getSnapshot().getVolumeUuid());
        bkd.handle(msg, new ReturnValueCompletion<DeleteSnapshotOnPrimaryStorageReply>(msg) {
            @Override
            public void success(DeleteSnapshotOnPrimaryStorageReply returnValue) {
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                DeleteSnapshotOnPrimaryStorageReply reply = new DeleteSnapshotOnPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }

    protected HypervisorBackend getHypervisorBackendByVolumeUuid(String volUuid) {
        SimpleQuery<VolumeVO> q = dbf.createQuery(VolumeVO.class);
        q.select(VolumeVO_.format);
        q.add(VolumeVO_.uuid, Op.EQ, volUuid);
        String format = q.findValue();

        if (format == null) {
            throw new CloudRuntimeException(String.format("cannot find the volume[uuid:%s]", volUuid));
        }

        HypervisorType type = VolumeFormat.getMasterHypervisorTypeByVolumeFormat(format);
        HypervisorFactory f = getHypervisorFactoryByHypervisorType(type.toString());
        return f.getHypervisorBackend(self);
    }

    private void handle(final TakeSnapshotMsg msg) {
        final VolumeSnapshotInventory sp = msg.getStruct().getCurrent();
        HypervisorBackend bkd = getHypervisorBackendByVolumeUuid(sp.getVolumeUuid());
        bkd.handle(msg, new ReturnValueCompletion<TakeSnapshotReply>(msg) {
            @Override
            public void success(TakeSnapshotReply returnValue) {
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

    @Transactional(readOnly = true)
    private String getAvailableHostUuidForOperation() {
        String sql = "select host.uuid from PrimaryStorageClusterRefVO ref, HostVO host where" +
                " ref.clusterUuid = host.clusterUuid and ref.primaryStorageUuid = :psUuid and host.status = :hstatus" +
                " and host.state = :hstate";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("psUuid", self.getUuid());
        q.setParameter("hstatus", HostStatus.Connected);
        q.setParameter("hstate", HostState.Enabled);
        List<String> hostUuids = q.getResultList();
        if (hostUuids.isEmpty()) {
            return null;
        }

        Collections.shuffle(hostUuids);
        return hostUuids.get(0);
    }

    @Override
    public void handle(AskInstallPathForNewSnapshotMsg msg) {
        HypervisorBackend bkd = getHypervisorBackendByVolumeUuid(msg.getVolumeInventory().getUuid());

        bkd.handle(msg, new ReturnValueCompletion<AskInstallPathForNewSnapshotReply>(msg) {
            @Override
            public void success(AskInstallPathForNewSnapshotReply returnValue) {
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                AskInstallPathForNewSnapshotReply reply = new AskInstallPathForNewSnapshotReply();
                reply.setSuccess(false);
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
    }
}
