package org.zstack.storage.ceph.primary;

import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.storage.snapshot.VolumeSnapshotDeletionProtector;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.storage.ceph.CephConstants;
import static org.zstack.core.Platform.inerr;

public class CephSnapshotProtector implements VolumeSnapshotDeletionProtector {
    @Override
    public String getPrimaryStorageType() {
        return CephConstants.CEPH_PRIMARY_STORAGE_TYPE;
    }

    @Override
    public void protect(VolumeSnapshotInventory snapshot) {
        if (!snapshot.getPrimaryStorageInstallPath().contains(snapshot.getVolumeUuid())) {
            throw new OperationFailureException(inerr("the snapshot[name:%s, uuid:%s, path: %s] seems not belong to the volume[uuid:%s]",
                    snapshot.getName(), snapshot.getUuid(), snapshot.getPrimaryStorageInstallPath(), snapshot.getVolumeUuid()));
        }
    }
}
