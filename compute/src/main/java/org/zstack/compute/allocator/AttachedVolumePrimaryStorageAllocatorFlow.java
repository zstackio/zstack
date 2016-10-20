package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.allocator.AbstractHostAllocatorFlow;
import org.zstack.header.host.HostVO;
import org.zstack.header.storage.primary.PrimaryStorageClusterRefVO;
import org.zstack.header.storage.primary.PrimaryStorageClusterRefVO_;
import org.zstack.header.vm.VmInstanceConstant.VmOperation;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;

import java.util.*;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class AttachedVolumePrimaryStorageAllocatorFlow extends AbstractHostAllocatorFlow {
    @Autowired
    private DatabaseFacade dbf;

    @Override
    public void allocate() {
        throwExceptionIfIAmTheFirstFlow();

        if (VmOperation.NewCreate.toString().equals(spec.getVmOperation())) {
            next(candidates);
            return;
        }

        VmInstanceInventory vm = spec.getVmInstance();
        List<String> requiredPsUuids = CollectionUtils.transformToList(vm.getAllVolumes(), new Function<String, VolumeInventory>() {
            @Override
            public String call(VolumeInventory arg) {
                return arg.getPrimaryStorageUuid();
            }
        });

        // find out cluster that have all required primary storage attached
        SimpleQuery<PrimaryStorageClusterRefVO> q = dbf.createQuery(PrimaryStorageClusterRefVO.class);
        q.add(PrimaryStorageClusterRefVO_.primaryStorageUuid, Op.IN, requiredPsUuids);
        List<PrimaryStorageClusterRefVO> refs = q.list();
        Map<String, Set<String>> clusterPs = new HashMap<>();
        for (PrimaryStorageClusterRefVO ref : refs) {
            Set<String> pss = clusterPs.get(ref.getClusterUuid());
            if (pss == null) {
                pss = new HashSet<>();
                clusterPs.put(ref.getClusterUuid(), pss);
            }
            pss.add(ref.getPrimaryStorageUuid());
        }

        List<String> clusterHavingAllPs = new ArrayList<>();
        for (Map.Entry<String, Set<String>> e : clusterPs.entrySet()) {
            if (e.getValue().containsAll(requiredPsUuids)) {
                clusterHavingAllPs.add(e.getKey());
            }
        }

        // find out host in above result clusters
        List<HostVO> tmp = candidates;
        candidates = new ArrayList<>();
        if (!clusterHavingAllPs.isEmpty()) {
            for (HostVO h : tmp) {
                if (clusterHavingAllPs.contains(h.getClusterUuid())) {
                    candidates.add(h);
                }
            }
        }

        if (candidates.isEmpty()) {
            fail(String.format("no host found in clusters which have attached to all primary storage%s where vm[uuid:%s]'s volumes locate",
                    requiredPsUuids, vm.getUuid()));
        } else {
            next(candidates);
        }
    }
}
