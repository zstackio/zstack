package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.header.allocator.AbstractHostAllocatorFlow;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostVO;
import org.zstack.header.storage.primary.PrimaryStorageOverProvisioningManager;
import org.zstack.header.storage.primary.PrimaryStorageState;
import org.zstack.header.storage.primary.PrimaryStorageStatus;
import org.zstack.header.vm.VmInstanceConstant.VmOperation;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class HostPrimaryStorageAllocatorFlow extends AbstractHostAllocatorFlow {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PrimaryStorageOverProvisioningManager ratioMgr;

    @Transactional(readOnly = true)
    private List<HostVO> allocateFromCandidates() {
        List<String> huuids = getHostUuidsFromCandidates();

        if (!VmOperation.NewCreate.toString().equals(spec.getVmOperation())) {
            String sql = "select h" +
                    " from HostVO h" +
                    " where h.uuid in :uuids" +
                    " and h.clusterUuid in" +
                    " (" +
                    " select pr.clusterUuid" +
                    " from PrimaryStorageClusterRefVO pr, PrimaryStorageVO pri, PrimaryStorageCapacityVO cap" +
                    " where pr.primaryStorageUuid = pri.uuid" +
                    " and pri.uuid = cap.uuid" +
                    " and (pri.state = :state or pri.state =:state1)" +
                    " and pri.status = :status" +
                    " )";
            TypedQuery<HostVO> query = dbf.getEntityManager().createQuery(sql, HostVO.class);
            query.setParameter("uuids", huuids);
            query.setParameter("state", PrimaryStorageState.Enabled);
            query.setParameter("state1", PrimaryStorageState.Disabled);
            query.setParameter("status", PrimaryStorageStatus.Connected);
            return query.getResultList();
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

        if (spec.getRequiredPrimaryStorageUuid() != null) {
            if (psUuids.contains(spec.getRequiredPrimaryStorageUuid())) {
                psUuids.clear();
                psUuids.add(0, spec.getRequiredPrimaryStorageUuid());
            } else {
                return new ArrayList<>();
            }
        }

        sql = "select i.primaryStorageUuid from ImageCacheVO i where i.primaryStorageUuid in (:psUuids) and i.imageUuid = :iuuid";
        TypedQuery<String> iq = dbf.getEntityManager().createQuery(sql, String.class);
        iq.setParameter("psUuids", psUuids);
        iq.setParameter("iuuid", spec.getImage().getUuid());
        List<String> hasImagePrimaryStorage = iq.getResultList();

        List<String> psCandidates = new ArrayList<>();
        for (Tuple t : ts) {
            String psUuid = t.get(0, String.class);
            long cap = t.get(1, Long.class);

            if (hasImagePrimaryStorage.contains(psUuid)) {
                cap = ratioMgr.calculatePrimaryStorageAvailableCapacityByRatio(psUuid, cap);
            } else {
                // the primary storage doesn't have the image in cache
                // so we need to add the image size
                cap = ratioMgr.calculatePrimaryStorageAvailableCapacityByRatio(psUuid, cap) - spec.getImage().getActualSize();
            }

            if (cap > spec.getDiskSize()) {
                psCandidates.add(psUuid);
            }
        }

        if (psCandidates.isEmpty()) {
            return new ArrayList<>();
        }

        sql = "select h" +
                " from HostVO h, PrimaryStorageClusterRefVO ref" +
                " where ref.clusterUuid = h.clusterUuid" +
                " and ref.primaryStorageUuid in (:psUuids)" +
                " and h.uuid in (:huuids)";
        TypedQuery<HostVO> hq = dbf.getEntityManager().createQuery(sql, HostVO.class);
        hq.setParameter("psUuids", psCandidates);
        hq.setParameter("huuids", huuids);
        return hq.getResultList();
    }

    @Override
    public void allocate() {
        if (amITheFirstFlow()) {
            throw new CloudRuntimeException("HostPrimaryStorageAllocatorFlow cannot be the first flow in the chain");
        }

        candidates = allocateFromCandidates();

        if (candidates.isEmpty()) {
            String err = spec.getVmOperation().equals(VmOperation.NewCreate.toString()) ?
                    String.format("cannot find available primary storage[state: %s, status: %s, available capacity %s bytes]." +
                            " Check the state/status of primary storage and make sure they have been attached to clusters",
                            PrimaryStorageState.Enabled, PrimaryStorageStatus.Connected, spec.getDiskSize()) :
                    String.format("cannot find available primary storage[state: %s, status: %s]." +
                            " Check the state/status of primary storage and make sure they have been attached to clusters",
                            PrimaryStorageState.Enabled, PrimaryStorageStatus.Connected);
            fail(err);
        } else {
            next(candidates);
        }
    }
}
