package org.zstack.storage.snapshot;

/**
 */
public enum VolumeSnapshotErrors {
    NOT_IN_CORRECT_STATE(1000),
    RE_IMAGE_VM_NOT_IN_STOPPED_STATE(1001),
    RE_IMAGE_IMAGE_MEDIA_TYPE_SHOULD_NOT_BE_ISO(1002),
    RE_IMAGE_CANNOT_FIND_IMAGE_CACHE(1003);

    private String code;

    VolumeSnapshotErrors(int id) {
        code = String.format("VOLUME_SNAPSHOT.%s", id);
    }

    @Override
    public String toString() {
        return code;
    }
}
