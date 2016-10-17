package org.zstack.storage.primary.local;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.allocator.AbstractHostAllocatorFlow;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.host.HostVO;
import org.zstack.header.storage.primary.PrimaryStorageOverProvisioningManager;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_;
import org.zstack.header.vm.VmInstanceConstant.VmOperation;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.function.Function;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by frank on 10/24/2015.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class AllocatePrimaryStorageForVmMigrationFlow  extends AbstractHostAllocatorFlow {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PrimaryStorageOverProvisioningManager ratioMgr;

    @Override
    public void allocate() {
        throwExceptionIfIAmTheFirstFlow();

        if (!VmOperation.Migrate.toString().equals(spec.getVmOperation())) {
            throw new CloudRuntimeException("AllocatePrimaryStorageForVmMigrationFlow is only used for migrating vm");
        }

        String psUuid = spec.getVmInstance().getRootVolume().getPrimaryStorageUuid();
        List<String> huuids = CollectionUtils.transformToList(candidates, new Function<String, HostVO>() {
            @Override
            public String call(HostVO arg) {
                return arg.getUuid();
            }
        });

        long volumeSize = 0;
        List<String> volUuids = new ArrayList<>();
        for (VolumeInventory vol : spec.getVmInstance().getAllVolumes()) {
            volumeSize += vol.getSize();
            volUuids.add(vol.getUuid());
        }

        long snapshotSize = 0;
        SimpleQuery<VolumeSnapshotVO> sq = dbf.createQuery(VolumeSnapshotVO.class);
        sq.select(VolumeSnapshotVO_.size);
        sq.add(VolumeSnapshotVO_.volumeUuid, Op.IN, volUuids);
        List<Long> snapshotSizes = sq.listValue();
        for (Long s : snapshotSizes) {
            snapshotSize += s;
        }

        SimpleQuery<LocalStorageHostRefVO> q = dbf.createQuery(LocalStorageHostRefVO.class);
        q.add(LocalStorageHostRefVO_.hostUuid, Op.IN, huuids);
        q.add(LocalStorageHostRefVO_.primaryStorageUuid, Op.EQ, psUuid);
        List<LocalStorageHostRefVO> refs = q.list();
        final List<String> hostUuids = new ArrayList<>();
        for (LocalStorageHostRefVO ref : refs) {
            if (ref.getAvailableCapacity() > ratioMgr.calculateByRatio(psUuid, volumeSize) + snapshotSize) {
                hostUuids.add(ref.getHostUuid());
            }
        }

        candidates = CollectionUtils.transformToList(candidates, new Function<HostVO, HostVO>() {
            @Override
            public HostVO call(HostVO arg) {
                return hostUuids.contains(arg.getUuid()) ? arg : null;
            }
        });

        if (candidates.isEmpty()) {
            fail(String.format("no hosts can provide %s bytes for all volumes of the vm[uuid:%s]", volumeSize, spec.getVmInstance().getUuid()));
        } else {
            next(candidates);
        }
    }
}
