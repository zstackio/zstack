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
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.primary.VolumeSnapshotCapability.VolumeSnapshotArrangementType;
import org.zstack.header.volume.VolumeVO;
import org.zstack.storage.primary.PrimaryStorageBase;
import org.zstack.utils.data.SizeUnit;

import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.*;

/**
 * Created by frank on 6/30/2015.
 */
public class LocalStorageBase extends PrimaryStorageBase {
    @Autowired
    private PluginRegistry pluginRgty;

    static class FactoryCluster {
        LocalStorageHypervisorFactory factory;
        List<ClusterInventory> clusters;
    }

    public LocalStorageBase(PrimaryStorageVO self) {
        super(self);
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
        LocalStorageHypervisorFactory f = getHypervisorBackendFactoryByHostUuid(msg.getDestHost().getUuid());
        final LocalStorageHypervisorBackend bkd = f.getHypervisorBackend(self);

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("instantiate-volume-%s-local-primary-storage-%s", msg.getVolume().getUuid(), self.getUuid()));
        chain.then(new ShareFlow() {
            InstantiateVolumeReply reply;

            @Override
            public void setup() {
                flow(new Flow() {
                    String __name__ = "allocate-capacity-on-host";

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        reserveCapacityOnHost(msg.getDestHost().getUuid(), msg.getVolume().getSize());
                        trigger.next();
                    }

                    @Override
                    public void rollback(FlowTrigger trigger, Map data) {
                        returnCapacityToHost(msg.getDestHost().getUuid(), msg.getVolume().getSize());
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
                                msg.getVolume().getSize(), msg.getDestHost().getUuid());
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
    protected void handle(DownloadDataVolumeToPrimaryStorageMsg msg) {
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
        final List<FactoryCluster> fs = getAllFactoriesForAttachedClusters();

        FlowChain chain = FlowChainBuilder.newShareFlowChain();
        chain.setName(String.format("connect-local-primary-storage-%s", self.getUuid()));
        chain.then(new ShareFlow() {
            @Override
            public void setup() {
                flow(new NoRollbackFlow() {
                    String __name__ = "connect-all-backend";
                    final Iterator<FactoryCluster> it = fs.iterator();

                    private void connect(final FlowTrigger trigger) {
                        if (!it.hasNext()) {
                            trigger.next();
                            return;
                        }

                        FactoryCluster fc = it.next();
                        LocalStorageHypervisorBackend bkd = fc.factory.getHypervisorBackend(self);
                        bkd.connectHook(msg, new Completion(trigger) {
                            @Override
                            public void success() {
                                connect(trigger);
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }

                    @Override
                    public void run(FlowTrigger trigger, Map data) {
                        connect(trigger);
                    }
                });

                flow(new NoRollbackFlow() {
                    String __name__ = "sync-physical-capacity";

                    @Override
                    public void run(final FlowTrigger trigger, Map data) {
                        syncPhysicalCapacity(new ReturnValueCompletion<PhysicalCapacityUsage>(trigger) {
                            @Override
                            public void success(PhysicalCapacityUsage returnValue) {
                                setCapacity(null, null, returnValue.totalPhysicalSize, returnValue.availablePhysicalSize);
                                trigger.next();
                            }

                            @Override
                            public void fail(ErrorCode errorCode) {
                                trigger.fail(errorCode);
                            }
                        });
                    }
                });

                done(new FlowDoneHandler(completion) {
                    @Override
                    public void handle(Map data) {
                        completion.success();
                    }
                });

                error(new FlowErrorHandler(completion) {
                    @Override
                    public void handle(ErrorCode errCode, Map data) {
                        completion.fail(errCode);
                    }
                });
            }
        }).start();
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
