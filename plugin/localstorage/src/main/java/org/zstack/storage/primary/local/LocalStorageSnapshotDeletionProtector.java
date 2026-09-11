package org.zstack.storage.primary.local;

import org.zstack.header.core.Completion;
import org.zstack.header.storage.snapshot.VolumeSnapshotDeletionProtector;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.storage.volume.VolumeSystemTags;

import static org.zstack.core.Platform.inerr;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class LocalStorageSnapshotDeletionProtector implements VolumeSnapshotDeletionProtector {
    @Override
    public String getPrimaryStorageType() {
        return LocalStorageConstants.LOCAL_STORAGE_TYPE;
    }

    @Override
    public void protect(VolumeSnapshotInventory snapshot, Completion completion) {
        List<String> volumeUuids = getUsedVolumeUuids(snapshot);
        volumeUuids.add(snapshot.getVolumeUuid());

        if (!isSnapshotPathOwnedByVolume(snapshot.getPrimaryStorageInstallPath(), volumeUuids)) {
            completion.fail(inerr("the snapshot[name:%s, uuid:%s, path: %s] seems not belong to the volume[uuid:%s]",
                    snapshot.getName(), snapshot.getUuid(), snapshot.getPrimaryStorageInstallPath(), snapshot.getVolumeUuid()));
            return;
        }
        completion.success();
    }

    private List<String> getUsedVolumeUuids(VolumeSnapshotInventory snapshot) {
        return VolumeSystemTags.OVERWRITED_VOLUME.getTags(snapshot.getVolumeUuid()).stream()
                .map(it -> VolumeSystemTags.OVERWRITED_VOLUME.getTokenByTag(
                        it, VolumeSystemTags.OVERWRITED_VOLUME_TOKEN))
                .collect(Collectors.toList());
    }

    private static boolean isSnapshotPathOwnedByVolume(String installPath, List<String> volumeUuids) {
        Path path = Paths.get(installPath);
        return volumeUuids.stream().anyMatch(it -> path.getParent().toString().contains(it));
    }
}
