package org.zstack.storage.addon.primary;

public class ExternalPrimaryStorageNameHelper {
    public static String buildVolumeName(String volumeUuid) {
        return String.format("volume_%s", volumeUuid);
    }

    public static String buildImageName(String imageUuid) {
        return String.format("image_%s", imageUuid);
    }

    public static String buildSnapshotName(String snapshotUuid) {
        return String.format("snapshot_%s", snapshotUuid);
    }
}
