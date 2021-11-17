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

    private List<HostVO> allocateIfNotNewCreate(List<String> huuids, Set<String> requiredPsUuids) {
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

    @Transactional(readOnly = true)
    private List<HostVO> allocateFromCandidates() {
        List<String> huuids = getHostUuidsFromCandidates();
        Set<String> requiredPsUuids = spec.getRequiredPrimaryStorageUuids();
        if (!VmOperation.NewCreate.toString().equals(spec.getVmOperation())) {
            return allocateIfNotNewCreate(huuids, requiredPsUuids);
        }

        

        // for new created vm
        String sql = "select ps.uuid" +
                " from PrimaryStorageClusterRefVO ref, PrimaryStorageVO ps, HostVO h" +
                " where ref.primaryStorageUuid = ps.uuid" +
                " and ps.state = :state" +
                " and ps.status = :status" +
                " and ref.clusterUuid = h.clusterUuid" +
                " and h.uuid in (:huuids)";

        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
        q.setParameter("state", PrimaryStorageState.Enabled);
        q.setParameter("status", PrimaryStorageStatus.Connected);
        q.setParameter("huuids", huuids);
        List<String> psUuids = q.getResultList();
        if (psUuids.isEmpty()) {
            return new ArrayList<>();
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
                        " and ref.status != :phStatus" +
                        " )", String.class)
                        .param("huuids", huuids)
                        .param("psUuids", requiredPsUuids)
                        .param("phStatus", PrimaryStorageHostStatus.Connected)
                        .list();
                if (huuids.isEmpty()) {
                    return new ArrayList<>();
                }
            } else {
                return new ArrayList<>();
            }
        }

        String sqlappend = psUuids.size() == 1 ?
                String.format(" and ref.primaryStorageUuid = '%s'", psUuids.get(0)) :
                String.format(" and ref.primaryStorageUuid in ('%s')", String.join("','", psUuids));

        sql = "select h.uuid, ref.primaryStorageUuid, cap.availableCapacity" +
                " from PrimaryStorageClusterRefVO ref, HostVO h, PrimaryStorageCapacityVO cap" +
                " where ref.clusterUuid = h.clusterUuid" +
                " and cap.uuid = ref.primaryStorageUuid" +
                " and h.uuid in (:hostUuids)" +
                sqlappend;
        TypedQuery<Tuple> hostCluster = dbf.getEntityManager().createQuery(sql, Tuple.class);
        hostCluster.setParameter("hostUuids", huuids);
        List<Tuple> result = hostCluster.getResultList();
        Map<String, Long> psCap = new HashMap<>();           // psUuid -> available capacities (with ratio).
        Map<String, Long> availableHostPs = new HashMap<>(); // hostUuid -> available capacities (with ratio).
        Map<String, String> hostPsDict = new HashMap<>();    // host -> ps mapping
        for (Tuple t : result) {
            String hostUuid = t.get(0, String.class);
            String psUuid = t.get(1, String.class);
            Long psAvaCap = t.get(2, Long.class);

            if (!psCap.containsKey(psUuid)) {
                psCap.put(psUuid, ratioMgr.calculatePrimaryStorageAvailableCapacityByRatio(psUuid, psAvaCap));
            }

            Long tmpCap = availableHostPs.get(hostUuid);
            if (tmpCap == null) {
                availableHostPs.put(hostUuid, psCap.get(psUuid));
                hostPsDict.put(hostUuid, psUuid);
            } else {
                availableHostPs.put(hostUuid, tmpCap + psCap.get(psUuid));
            }
        }

        Set<String> hostCandidates = new HashSet<>();
        Map<String, Boolean> psImageCacheDict = new HashMap<>();
        for (String hostUuid: availableHostPs.keySet()) {
            if (hasCapablePS(hostPsDict.get(hostUuid), availableHostPs.get(hostUuid), psImageCacheDict)) {
                hostCandidates.add(hostUuid);
            }
        }

        if (hostCandidates.isEmpty()) {
            return new ArrayList<>();
        }

        return candidates.stream()
                .filter(h -> hostCandidates.contains(h.getUuid()))
                .collect(Collectors.toList());
    }

    private Boolean psHasImageCache(String psUuid, Map<String, Boolean> psImageCacheDict) {
        Boolean b = psImageCacheDict.get(psUuid);
        if (b != null) {
            return b;
        }

        b = Q.New(ImageCacheVO.class)
                .eq(ImageCacheVO_.primaryStorageUuid, psUuid)
                .eq(ImageCacheVO_.imageUuid, spec.getImage().getUuid())
                .isExists();
        psImageCacheDict.put(psUuid, b);
        return b;
    }

    private boolean hasCapablePS(String psUuid, Long cap, Map<String, Boolean> psImageCacheDict) {
        // background: http://dev.zstack.io/browse/ZSTAC-4852
        // only fix problem one
        long requiredSize = spec.getDiskSize();

        if (spec.getImage() != null) {
            if (!psHasImageCache(psUuid, psImageCacheDict)) {
                // the primary storage doesn't have the image in cache
                // so we need to add the image size
                requiredSize +=  spec.getImage().getActualSize();
            }
        }

        // if multi ps, don't consider image cache, because of cannot determine the primary storage associated with the root disk
        return cap >= requiredSize;
    }

    @Override
    public void allocate() {
        if (amITheFirstFlow()) {
            throw new CloudRuntimeException("HostPrimaryStorageAllocatorFlow cannot be the first flow in the chain");
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
