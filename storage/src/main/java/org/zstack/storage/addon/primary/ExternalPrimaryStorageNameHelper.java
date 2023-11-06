package org.zstack.storage.addon.primary;

public class ExternalPrimaryStorageNameHelper {
    public static String buildVolumeName(String volumeUuid) {
        return String.format("volume_%s", volumeUuid);
    }

    public static String buildChangeImageVolumeName(String volumeUuid) {
        return String.format("change_image_volume_%s_%d", volumeUuid, System.currentTimeMillis());
    }

    public static String buildReimageVolumeName(String volumeUuid) {
        return String.format("reimage_volume_%s_%d", volumeUuid, System.currentTimeMillis());
    }

    public static String buildImageName(String imageUuid) {
        return String.format("image_%s", imageUuid);
    }

    public static String buildSnapshotName(String snapshotUuid) {
        return String.format("snapshot_%s", snapshotUuid);
    }
}
