package org.zstack.storage.primary.nfs;

import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.storage.snapshot.VolumeSnapshotDeletionProtector;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import static org.zstack.core.Platform.inerr;

import java.nio.file.Path;
import java.nio.file.Paths;

public class NfsVolumeSnapshotProtector implements VolumeSnapshotDeletionProtector {
    @Override
    public String getPrimaryStorageType() {
        return NfsPrimaryStorageConstant.NFS_PRIMARY_STORAGE_TYPE;
    }

    @Override
    public void protect(VolumeSnapshotInventory snapshot) {
        Path path = Paths.get(snapshot.getPrimaryStorageInstallPath());
        if (!path.getParent().toString().contains(snapshot.getVolumeUuid())) {
            throw new OperationFailureException(inerr("the snapshot[name:%s, uuid:%s, path: %s] seems not belong to the volume[uuid:%s]",
                    snapshot.getName(), snapshot.getUuid(), snapshot.getPrimaryStorageInstallPath(), snapshot.getVolumeUuid()));
        }
    }
}
