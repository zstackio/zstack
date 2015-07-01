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
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.storage.primary.*;
import org.zstack.storage.primary.PrimaryStorageBase;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;

import javax.persistence.LockModeType;
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

    @Override
    protected void handle(InstantiateVolumeMsg msg) {

    }

    @Override
    protected void handle(DeleteVolumeOnPrimaryStorageMsg msg) {

    }

    @Override
    protected void handle(CreateTemplateFromVolumeOnPrimaryStorageMsg msg) {

    }

    @Override
    protected void handle(DownloadDataVolumeToPrimaryStorageMsg msg) {

    }

    @Override
    protected void handle(DeleteBitsOnPrimaryStorageMsg msg) {

    }

    @Override
    protected void handle(DownloadIsoToPrimaryStorageMsg msg) {

    }

    @Override
    protected void handle(DeleteIsoFromPrimaryStorageMsg msg) {

    }

    @Override
    protected void handle(AskVolumeSnapshotCapabilityMsg msg) {

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
                m.put(c.getHypervisorType(), fc)
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
