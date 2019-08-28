package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.allocator.AbstractHostAllocatorFlow;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostVO;
import org.zstack.header.storage.primary.*;
import org.zstack.header.vm.VmInstanceConstant.VmOperation;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.*;
import java.util.stream.Collectors;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class HostPrimaryStorageAllocatorFlow extends AbstractHostAllocatorFlow {
    private final static CLogger logger = Utils.getLogger(HostPrimaryStorageAllocatorFlow.class);
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PrimaryStorageOverProvisioningManager ratioMgr;

    @Transactional(readOnly = true)
    private List<HostVO> allocateFromCandidates() {
        List<String> huuids = getHostUuidsFromCandidates();
        Set<String> requiredPsUuids = spec.getRequiredPrimaryStorageUuids();
        if (!VmOperation.NewCreate.toString().equals(spec.getVmOperation())) {
            String sqlappend = "";
            if (!requiredPsUuids.isEmpty()) {
                sqlappend = requiredPsUuids.size() == 1 ?
                        String.format(" and ps.uuid = '%s'", requiredPsUuids.iterator().next()) :
                        String.format(" and ps.uuid in ('%s')" +
                        " group by ref.clusterUuid" +
                        " having count(distinct ref.primaryStorageUuid) = %d",
                        String.join("','", requiredPsUuids), requiredPsUuids.size());
            }

            String sql = "select h" +
                    " from HostVO h" +
                    " where h.uuid in :uuids" +
                    " and h.clusterUuid in" +
                    " (" +
                    " select ref.clusterUuid" +
                    " from PrimaryStorageClusterRefVO ref, PrimaryStorageVO ps" +
                    " where ref.primaryStorageUuid = ps.uuid" +
                    " and (ps.state = :state or ps.state =:state1)" +
                    " and ps.status = :status" +
                    sqlappend +
                    " )";

            TypedQuery<HostVO> query = dbf.getEntityManager().createQuery(sql, HostVO.class);
            query.setParameter("uuids", huuids);
            query.setParameter("state", PrimaryStorageState.Enabled);
            query.setParameter("state1", PrimaryStorageState.Disabled);
            query.setParameter("status", PrimaryStorageStatus.Connected);
            List<HostVO> hosts = query.getResultList();

            // in case no host is connected
            if (hosts.isEmpty()) {
                return new ArrayList<>();
            }

            List<String> hostUuids = hosts.stream().map(HostVO::getUuid).collect(Collectors.toList());
            List<String> disconnectHostUuids = requiredPsUuids.isEmpty() ? Collections.emptyList() :
                    Q.New(PrimaryStorageHostRefVO.class).select(PrimaryStorageHostRefVO_.hostUuid)
                            .in(PrimaryStorageHostRefVO_.primaryStorageUuid, requiredPsUuids)
                            .eq(PrimaryStorageHostRefVO_.status, PrimaryStorageHostStatus.Disconnected)
                            .in(PrimaryStorageHostRefVO_.hostUuid, hostUuids)
                            .listValues();

            if (!disconnectHostUuids.isEmpty()) {
                logger.trace(String.format("There are some disconnection between primary storage[uuids:%s]" +
                        " and host[uuids:%s], remove these hosts", requiredPsUuids, disconnectHostUuids));
                Set<String> discHostSet = new HashSet<>(disconnectHostUuids);
                hosts.removeIf(it -> discHostSet.contains(it.getUuid()));
            }

            return hosts;
        }

        // for new created vm
        String sql = "select ps.uuid, cap.availableCapacity" +
                " from PrimaryStorageClusterRefVO ref, PrimaryStorageVO ps, HostVO h, PrimaryStorageCapacityVO cap" +
                " where ref.primaryStorageUuid = ps.uuid" +
                " and cap.uuid = ps.uuid" +
                " and ps.state = :state" +
                " and ps.status = :status" +
                " and ref.clusterUuid = h.clusterUuid" +
                " and h.uuid in (:huuids)";

        TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
        q.setParameter("state", PrimaryStorageState.Enabled);
        q.setParameter("status", PrimaryStorageStatus.Connected);
        q.setParameter("huuids", huuids);
        List<Tuple> ts = q.getResultList();
        if (ts.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> psUuids = new ArrayList<>();
        for (Tuple t : ts) {
            psUuids.add(t.get(0, String.class));
        }

        if (!requiredPsUuids.isEmpty()) {
            if (psUuids.containsAll(requiredPsUuids)) {
                psUuids.clear();
                psUuids.addAll(requiredPsUuids);
                huuids = SQL.New("select h.uuid from HostVO h" +
                        " where h.uuid in :huuids" +
                        " and h.uuid not in (" +
                        " select ref.hostUuid from PrimaryStorageHostRefVO ref" +
                        " where ref.primaryStorageUuid in :psUuids" +
                        " and ref.status = :phStatus" +
                        " )", String.class)
                        .param("huuids", huuids)
                        .param("psUuids", requiredPsUuids)
                        .param("phStatus", PrimaryStorageHostStatus.Disconnected)
                        .list();
                if (huuids.isEmpty()) {
                    return new ArrayList<>();
                }
            } else {
                return new ArrayList<>();
            }
        }

        sql = "select i.primaryStorageUuid from ImageCacheVO i where i.primaryStorageUuid in (:psUuids) and i.imageUuid = :iuuid";
        TypedQuery<String> iq = dbf.getEntityManager().createQuery(sql, String.class);
        iq.setParameter("psUuids", psUuids);
        iq.setParameter("iuuid", spec.getImage().getUuid());
        List<String> hasImagePrimaryStorage = iq.getResultList();

        List<String> hostCandidates = new ArrayList<>();
        sql = "select h.uuid, ref.clusterUuid" +
                " from PrimaryStorageClusterRefVO ref, HostVO h" +
                " where ref.clusterUuid = h.clusterUuid" +
                " and h.uuid in (:hostUuids)" +
                " and ref.primaryStorageUuid in (:psUuids)";
        TypedQuery<Tuple> hostCluster = dbf.getEntityManager().createQuery(sql, Tuple.class);
        hostCluster.setParameter("hostUuids", huuids);
        hostCluster.setParameter("psUuids", psUuids);
        List<Tuple> result = hostCluster.getResultList();
        for (Tuple t : result) {
            String hostUuid = t.get(0, String.class);
            String clusterUuid = t.get(1, String.class);

            List<String> clusterPsUuids = Q.New(PrimaryStorageClusterRefVO.class)
                    .select(PrimaryStorageClusterRefVO_.primaryStorageUuid)
                    .eq(PrimaryStorageClusterRefVO_.clusterUuid, clusterUuid)
                    .in(PrimaryStorageClusterRefVO_.primaryStorageUuid, psUuids)
                    .listValues();

            if(clusterPsUuids.isEmpty()){
                break;
            }

            // background: http://dev.zstack.io/browse/ZSTAC-4852
            // only fix problem one
            long cap = 0L;
            if(clusterPsUuids.size() == 1){
                String psUuid = clusterPsUuids.get(0);
                Long psAvaCap = Q.New(PrimaryStorageCapacityVO.class).select(PrimaryStorageCapacityVO_.availableCapacity)
                        .eq(PrimaryStorageCapacityVO_.uuid, psUuid).findValue();

                if (hasImagePrimaryStorage.contains(psUuid)) {
                    cap = ratioMgr.calculatePrimaryStorageAvailableCapacityByRatio(psUuid, psAvaCap );
                } else {
                    // the primary storage doesn't have the image in cache
                    // so we need to add the image size
                    cap = ratioMgr.calculatePrimaryStorageAvailableCapacityByRatio(psUuid, psAvaCap) - spec.getImage().getActualSize();
                }
            }else{
                // multi ps, don't consider image cache, because of cannot determine the primary storage associated with the root disk
                for(String psUuid : clusterPsUuids){
                    Long psAvaCap = Q.New(PrimaryStorageCapacityVO.class).select(PrimaryStorageCapacityVO_.availableCapacity)
                            .eq(PrimaryStorageCapacityVO_.uuid, psUuid).findValue();
                    cap = cap + ratioMgr.calculatePrimaryStorageAvailableCapacityByRatio(psUuid, psAvaCap);
                }
            }

            if (cap >= spec.getDiskSize()) {
                hostCandidates.add(hostUuid);
            }
        }

        if (hostCandidates.isEmpty()) {
            return new ArrayList<>();
        }

        sql = "select h from HostVO h where h.uuid in (:huuids)";
        TypedQuery<HostVO> hq = dbf.getEntityManager().createQuery(sql, HostVO.class);
        hq.setParameter("huuids", hostCandidates);
        return hq.getResultList();
    }

    @Override
    public void allocate() {
        if (amITheFirstFlow()) {
            throw new CloudRuntimeException("HostPrimaryStorageAllocatorFlow cannot be the first flow in the chain");
        }

        if (spec.getImage() == null) {
            next(candidates);
            return;
        }

        candidates = allocateFromCandidates();

        if (candidates.isEmpty()) {
            if (spec.getVmOperation().equals(VmOperation.NewCreate.toString())) {
                fail(Platform.operr("cannot find available primary storage[state: %s, status: %s, available capacity %s bytes]." +
                        " Check the state/status of primary storage and make sure they have been attached to clusters",
                        PrimaryStorageState.Enabled, PrimaryStorageStatus.Connected, spec.getDiskSize()));
            } else {
                fail(Platform.operr("cannot find available primary storage[state: %s or %s, status: %s]." +
                        " Check the state/status of primary storage and make sure they have been attached to clusters",
                        PrimaryStorageState.Enabled, PrimaryStorageState.Disabled, PrimaryStorageStatus.Connected));
            }

        } else {
            next(candidates);
        }
    }
}
