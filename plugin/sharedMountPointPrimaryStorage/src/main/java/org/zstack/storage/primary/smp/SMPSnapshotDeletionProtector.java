package org.zstack.storage.primary.smp;

import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.storage.snapshot.VolumeSnapshotDeletionProtector;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.storage.snapshot.VolumeSnapshotSystemTags;

import static org.zstack.core.Platform.*;

import java.nio.file.Path;
import java.nio.file.Paths;

public class SMPSnapshotDeletionProtector implements VolumeSnapshotDeletionProtector {
    @Override
    public String getPrimaryStorageType() {
        return SMPConstants.SMP_TYPE;
    }

    @Override
    public void protect(VolumeSnapshotInventory snapshot, Completion completion) {
        String backingToVolumeUuid = VolumeSnapshotSystemTags.BACKING_TO_VOLUME.getTokenByResourceUuid(
                snapshot.getUuid(), VolumeSnapshotSystemTags.BACKING_VOLUME_TOKEN);
        if (backingToVolumeUuid != null) {
            completion.fail(operr("snapshot[uuid:%s] is backing file of volume[uuid:%s], cannot delete.",
                    snapshot.getUuid(), backingToVolumeUuid));
            return;
        }

        Path path = Paths.get(snapshot.getPrimaryStorageInstallPath());
        if (!path.getParent().toString().contains(snapshot.getVolumeUuid())) {
            completion.fail(inerr("the snapshot[name:%s, uuid:%s, path: %s] seems not belong to the volume[uuid:%s]",
                    snapshot.getName(), snapshot.getUuid(), snapshot.getPrimaryStorageInstallPath(), snapshot.getVolumeUuid()));
            return;
        }
        completion.success();
    }
}
