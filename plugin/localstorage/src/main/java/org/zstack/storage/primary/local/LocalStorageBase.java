package org.zstack.storage.primary.local;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.core.workflow.*;
import org.zstack.header.cluster.ClusterInventory;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.cluster.ClusterVO_;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.core.workflow.*;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.image.ImageVO;
import org.zstack.header.message.Message;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.primary.CreateTemplateFromVolumeSnapshotOnPrimaryStorageMsg.SnapshotDownloadInfo;
import org.zstack.header.storage.primary.VolumeSnapshotCapability.VolumeSnapshotArrangementType;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.vm.VmInstanceState;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.volume.VolumeType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.storage.primary.PrimaryStorageBase;
import org.zstack.utils.Utils;
import org.zstack.utils.data.SizeUnit;
import org.zstack.utils.logging.CLogger;

import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.rmi.CORBA.Util;
import java.util.*;

/**
 * Created by frank on 6/30/2015.
 */
public class LocalStorageBase extends PrimaryStorageBase {
    private static final CLogger logger = Utils.getLogger(LocalStorageBase.class);

    @Autowired
    private PluginRegistry pluginRgty;

    static class FactoryCluster {
        LocalStorageHypervisorFactory factory;
        List<ClusterInventory> clusters;
    }

    public LocalStorageBase(PrimaryStorageVO self) {
        super(self);
    }

    @Override
    protected void handleLocalMessage(Message msg) {
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
        } else if (msg instanceof CreateTemplateFromVolumeSnapshotOnPrimaryStorageMsg) {
            handle((CreateTemplateFromVolumeSnapshotOnPrimaryStorageMsg) msg);
        } else if (msg instanceof CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg) {
            handle((CreateVolumeFromVolumeSnapshotOnPrimaryStorageMsg) msg);
        } else if (msg instanceof MergeVolumeSnapshotOnPrimaryStorageMsg) {
            handle((MergeVolumeSnapshotOnPrimaryStorageMsg) msg);
        } else {
            super.handleLocalMessage(msg);
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
        final VolumeSnapshotInventory sinv = msg.getSnapshots().get(0).getSnapshot();
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
                flow(new Flow() {
                    String __name__ = "reserve-capacity-on-host";

                    long size;

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        if (sinv.getVolumeUuid() != null) {
                            SimpleQuery<VolumeVO> q = dbf.createQuery(VolumeVO.class);
                            q.select(VolumeVO_.size);
                            q.add(VolumeVO_.uuid, Op.EQ, sinv.getVolumeUuid());
                            size = q.findValue();
                        } else {
                            for (SnapshotDownloadInfo sp : msg.getSnapshots()) {
                                size += sp.getSnapshot().getSize();
                            }
                        }

                        reserveCapacityOnHost(hostUuid, size);
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowTrigger trigger, Map data) {
                        returnCapacityToHost(hostUuid,size);
                        trigger.rollback();
                    }
                });

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

    private void handle(final CreateTemplateFromVolumeSnapshotOnPrimaryStorageMsg msg) {
        final List<SnapshotDownloadInfo> infos = msg.getSnapshotsDownloadInfo();
        final VolumeSnapshotInventory sinv = infos.get(0).getSnapshot();
        String hostUuid = getHostUuidByResourceUuid(sinv.getUuid());

        if (hostUuid == null) {
            throw new OperationFailureException(errf.stringToInternalError(
                    String.format("the volume snapshot[uuid:%s] is not on the local primary storage[uuid: %s]; the local primary storage" +
                            " doesn't support the manner of downloading snapshots and creating the volume", sinv.getUuid(), self.getUuid())
            ));
        }

        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByHostUuid(hostUuid);
        LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, hostUuid, new ReturnValueCompletion<CreateTemplateFromVolumeSnapshotOnPrimaryStorageReply>(msg) {
            @Override
            public void success(CreateTemplateFromVolumeSnapshotOnPrimaryStorageReply returnValue) {
                bus.reply(msg, returnValue);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                CreateTemplateFromVolumeSnapshotOnPrimaryStorageReply reply = new CreateTemplateFromVolumeSnapshotOnPrimaryStorageReply();
                reply.setError(errorCode);
                bus.reply(msg, reply);
            }
        });
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

        if (msg.getVolume().getVmInstanceUuid() != null) {
            SimpleQuery<VmInstanceVO> q = dbf.createQuery(VmInstanceVO.class);
            q.select(VmInstanceVO_.state);
            q.add(VmInstanceVO_.uuid, Op.EQ, msg.getVolume().getVmInstanceUuid());
            VmInstanceState state = q.findValue();
            if (state != VmInstanceState.Stopped) {
                reply.setError(errf.stringToOperationError(
                        String.format("unable to revert volume[uuid:%s] to snapshot[uuid:%s], the vm[uuid:%s] volume attached to is not in Stopped state, current state is %s",
                                msg.getVolume().getUuid(), msg.getSnapshot().getUuid(), msg.getVolume().getVmInstanceUuid(), state)
                ));

                bus.reply(msg, reply);
                return;
            }
        }

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

    private String getHostUuidByResourceUuid(String resUuid) {
        SimpleQuery<LocalStorageResourceRefVO> q = dbf.createQuery(LocalStorageResourceRefVO.class);
        q.select(LocalStorageResourceRefVO_.hostUuid);
        q.add(LocalStorageResourceRefVO_.resourceUuid, Op.EQ, resUuid);
        return q.findValue();
    }

    private void handle(final DeleteSnapshotOnPrimaryStorageMsg msg) {
        String hostUuid = getHostUuidByResourceUuid(msg.getSnapshot().getUuid());
        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByHostUuid(hostUuid);
        LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, hostUuid, new ReturnValueCompletion<DeleteSnapshotOnPrimaryStorageReply>(msg) {
            @Override
            public void success(DeleteSnapshotOnPrimaryStorageReply returnValue) {
                deleteResourceRefVO(msg.getSnapshot().getUuid());
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

    private void handle(RemoveHostFromLocalStorageMsg msg) {
        LocalStorageHostRefVO ref = dbf.findByUuid(msg.getHostUuid(), LocalStorageHostRefVO.class);
        // on remove, substract the total capacity from every capacity
        decreaseCapacity(ref.getTotalCapacity(), ref.getTotalCapacity(), ref.getTotalCapacity(), ref.getTotalCapacity());
        dbf.remove(ref);
        bus.reply(msg, new RemoveHostFromLocalStorageReply());
    }

    protected void handle(final InitPrimaryStorageOnHostConnectedMsg msg) {
        final InitPrimaryStorageOnHostConnectedReply reply = new InitPrimaryStorageOnHostConnectedReply();
        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByHostUuid(msg.getHostUuid());
        final LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.handle(msg, new ReturnValueCompletion<PhysicalCapacityUsage>(msg) {
            @Override
            public void success(PhysicalCapacityUsage c) {
                SimpleQuery<LocalStorageHostRefVO> q = dbf.createQuery(LocalStorageHostRefVO.class);
                q.add(LocalStorageHostRefVO_.hostUuid, Op.EQ, msg.getHostUuid());
                q.add(LocalStorageHostRefVO_.primaryStorageUuid, Op.EQ, self.getUuid());
                LocalStorageHostRefVO ref = q.find();
                if (ref == null) {
                    ref = new LocalStorageHostRefVO();
                    ref.setTotalCapacity(c.totalPhysicalSize);
                    ref.setAvailableCapacity(c.availablePhysicalSize);
                    ref.setTotalPhysicalCapacity(c.totalPhysicalSize);
                    ref.setAvailablePhysicalCapacity(c.availablePhysicalSize);
                    ref.setHostUuid(msg.getHostUuid());
                    ref.setPrimaryStorageUuid(self.getUuid());
                    dbf.persist(ref);

                    increaseCapacity(c.totalPhysicalSize, c.availablePhysicalSize, c.totalPhysicalSize, c.availablePhysicalSize);
                } else {
                    ref.setAvailablePhysicalCapacity(c.availablePhysicalSize);
                    dbf.update(ref);
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
            throw new CloudRuntimeException(String.format("cannot find host[uuid: %s] of local primary storage[uuid: %s]", hostUuid, self.getUuid()));
        }

        LocalStorageHostRefVO ref = refs.get(0);
        long avail = ref.getAvailableCapacity() - size;
        if (avail < 0) {
            throw new OperationFailureException(errf.stringToOperationError(
                    String.format("host[uuid: %s] of local primary storage[uuid: %s] doesn't have enough capacity[current: %s bytes, needed: %s]",
                            hostUuid, self.getUuid(), size, ref.getAvailableCapacity())
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
            throw new CloudRuntimeException(String.format("cannot find host[uuid: %s] of local primary storage[uuid: %s]", hostUuid, self.getUuid()));
        }

        LocalStorageHostRefVO ref = refs.get(0);
        ref.setAvailableCapacity(ref.getAvailableCapacity() + size);
        dbf.getEntityManager().merge(ref);
    }

    @Transactional
    protected void returnCapacityToHostByResourceUuid(String resUuid) {
        String sql = "select href, ref from LocalStorageHostRefVO href, LocalStorageResourceRefVO ref where href.hostUuid = ref.hostUuid and ref.resourceUuid = :resUuid and ref.primaryStorageUuid = :puuid";
        TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
        q.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        q.setParameter("resUuid", resUuid);
        q.setParameter("puuid", self.getUuid());
        Tuple ref = q.getSingleResult();

        LocalStorageHostRefVO href = ref.get(0, LocalStorageHostRefVO.class);
        LocalStorageResourceRefVO rref = ref.get(1, LocalStorageResourceRefVO.class);

        href.setAvailableCapacity(href.getAvailableCapacity() + rref.getSize());
        dbf.getEntityManager().merge(href);
    }

    @Override
    protected void handle(final InstantiateVolumeMsg msg) {
        String hostUuid = msg.getDestHost().getUuid();
        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByHostUuid(hostUuid);
        final LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("instantiate-volume-%s-local-primary-storage-%s", msg.getVolume().getUuid(), self.getUuid()));
        final String finalHostUuid = hostUuid;
        chain.then(new ShareFlow() {
            InstantiateVolumeReply reply;

            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "allocate-capacity-on-host";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        reserveCapacityOnHost(finalHostUuid, msg.getVolume().getSize());
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowTrigger trigger, Map data) {
                        returnCapacityToHost(finalHostUuid, msg.getVolume().getSize());
                        trigger.rollback();
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "instantiate-volume-on-host";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        bkd.handle(msg, new ReturnValueCompletion<InstantiateVolumeReply>(msg) {
                            @Override
                            public void success(InstantiateVolumeReply returnValue) {
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
                        InstantiateVolumeReply reply = new InstantiateVolumeReply();
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

            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "allocate-capacity-on-host";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        reserveCapacityOnHost(msg.getHostUuid(), msg.getImage().getSize());
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowTrigger trigger, Map data) {
                        returnCapacityToHost(msg.getHostUuid(), msg.getImage().getSize());
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
                        reserveCapacityOnHost(msg.getDestHostUuid(), msg.getIsoSpec().getInventory().getSize());
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowTrigger trigger, Map data) {
                        returnCapacityToHost(msg.getDestHostUuid(), msg.getIsoSpec().getInventory().getSize());
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
                        createResourceRefVO(msg.getIsoSpec().getInventory().getUuid(), ImageVO.class.getSimpleName(),
                                msg.getIsoSpec().getInventory().getSize(), msg.getDestHostUuid());
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
                        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByResourceUuid(msg.getIsoSpec().getInventory().getUuid(), ImageVO.class.getSimpleName());
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

    @Transactional
    protected void setCapacity(Long total, Long avail, Long totalPhysical, Long availPhysical) {
        PrimaryStorageCapacityVO cvo = dbf.getEntityManager().find(PrimaryStorageCapacityVO.class, self.getUuid(), LockModeType.PESSIMISTIC_WRITE);
        if (total != null) {
            cvo.setTotalCapacity(total);
        }
        if (avail != null) {
            cvo.setAvailableCapacity(avail);
        }
        if (totalPhysical != null) {
            cvo.setTotalPhysicalCapacity(totalPhysical);
        }
        if (availPhysical != null) {
            cvo.setAvailablePhysicalCapacity(availPhysical);
        }
        dbf.getEntityManager().merge(cvo);
    }

    @Transactional
    protected void increaseCapacity(Long total, Long avail, Long totalPhysical, Long availPhysical) {
        PrimaryStorageCapacityVO cvo = dbf.getEntityManager().find(PrimaryStorageCapacityVO.class, self.getUuid(), LockModeType.PESSIMISTIC_WRITE);
        if (total != null) {
            cvo.setTotalCapacity(cvo.getTotalCapacity() + total);
        }
        if (avail != null) {
            cvo.setAvailableCapacity(cvo.getAvailableCapacity() + avail);
        }
        if (totalPhysical != null) {
            cvo.setTotalPhysicalCapacity(cvo.getTotalPhysicalCapacity() + totalPhysical);
        }
        if (availPhysical != null) {
            cvo.setAvailablePhysicalCapacity(cvo.getAvailablePhysicalCapacity() + availPhysical);
        }
        dbf.getEntityManager().merge(cvo);
    }

    @Transactional
    protected void decreaseCapacity(Long total, Long avail, Long totalPhysical, Long availPhysical) {
        PrimaryStorageCapacityVO cvo = dbf.getEntityManager().find(PrimaryStorageCapacityVO.class, self.getUuid(), LockModeType.PESSIMISTIC_WRITE);
        if (total != null) {
            long t = cvo.getTotalCapacity() - total;
            cvo.setTotalCapacity(t < 0 ? 0 : t);
        }
        if (avail != null) {
            long a = cvo.getAvailableCapacity() - avail;
            cvo.setAvailableCapacity(a < 0 ? 0 : a);
        }
        if (totalPhysical != null) {
            long tp = cvo.getTotalPhysicalCapacity() - totalPhysical;
            cvo.setTotalPhysicalCapacity(tp < 0 ? 0 : tp);
        }
        if (availPhysical != null) {
            long ap = cvo.getAvailablePhysicalCapacity() - availPhysical;
            cvo.setAvailablePhysicalCapacity(ap < 0 ? 0 : ap);
        }
        dbf.getEntityManager().merge(cvo);
    }


    @Transactional(readOnly = true)
    protected List<FactoryCluster> getAllFactoriesForAttachedClusters() {
        String sql = "select cluster from ClusterVO cluster, PrimaryStorageClusterRefVO ref where ref.clusterUuid = cluster.uuid and ref.primaryStorageUuid = :uuid";
        TypedQuery<ClusterVO> q = dbf.getEntityManager().createQuery(sql, ClusterVO.class);
        q.setParameter("uuid", self.getUuid());
        List<ClusterVO> clusters = q.getResultList();

        if (clusters.isEmpty()) {
            return new ArrayList<FactoryCluster>();
        }

        Map<String, FactoryCluster> m = new HashMap<String, FactoryCluster>();
        for (ClusterVO c : clusters) {
            FactoryCluster fc = m.get(c.getHypervisorType());
            if (fc == null) {
                fc = new FactoryCluster();
                fc.factory = getHypervisorBackendFactory(c.getHypervisorType());
                fc.clusters = new ArrayList<ClusterInventory>();
                m.put(c.getHypervisorType(), fc);
            }

            fc.clusters.add(ClusterInventory.valueOf(c));
        }

        List<FactoryCluster> fcs = new ArrayList<FactoryCluster>();
        fcs.addAll(m.values());
        return fcs;
    }

    @Override
    protected void connectHook(final ConnectPrimaryStorageMsg msg, final Completion completion) {
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

    private LocalStorageHypervisorFactory getHypervisorBackendFactoryByHostUuid(String hostUuid) {
        SimpleQuery<HostVO> q = dbf.createQuery(HostVO.class);
        q.select(HostVO_.hypervisorType);
        q.add(HostVO_.uuid, Op.EQ, hostUuid);
        String hvType = q.findValue();
        return getHypervisorBackendFactory(hvType);
    }

    @Transactional(readOnly = true)
    private LocalStorageHypervisorFactory getHypervisorBackendFactoryByResourceUuid(String resUuid, String resourceType) {
        String sql = "select host.hypervisorType from HostVO host, LocalStorageResourceRefVO ref where ref.hostUuid = host.uuid and ref.resourceUuid = :resUuid and ref.primaryStorageUuid = :puuid";
        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("resUuid", resUuid);
        q.setParameter("puuid", self.getUuid());
        List<String> ret = q.getResultList();
        if (ret.isEmpty()) {
            throw new CloudRuntimeException(String.format("resource[uuid:%s, type: %s] is not on the local primary storage[uuid:%s]", resUuid, resourceType, self.getUuid()));
        }
        if (ret.size() != 1) {
            throw new CloudRuntimeException(String.format("resource[uuid:%s, type: %s] on the local primary storage[uuid:%s] maps to multiple hypervisor%s", resUuid, resourceType, self.getUuid(), ret));
        }

        String hvType = ret.get(0);
        return getHypervisorBackendFactory(hvType);
    }

    private LocalStorageHypervisorFactory getHypervisorBackendFactory(String hvType) {
        for (LocalStorageHypervisorFactory f : pluginRgty.getExtensionList(LocalStorageHypervisorFactory.class)) {
            if (hvType.equals(f.getHypervisorType())) {
                return f;
            }
        }

        throw new CloudRuntimeException(String.format("cannot find LocalStorageHypervisorFactory with hypervisorType[%s]", hvType));
    }

    @Override
    public void attachHook(String clusterUuid, Completion completion) {
        SimpleQuery<ClusterVO> q = dbf.createQuery(ClusterVO.class);
        q.select(ClusterVO_.hypervisorType);
        q.add(ClusterVO_.uuid, Op.EQ, clusterUuid);
        String hvType = q.findValue();

        LocalStorageHypervisorFactory f = getHypervisorBackendFactory(hvType);
        LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);
        bkd.attachHook(clusterUuid, completion);
    }
}
