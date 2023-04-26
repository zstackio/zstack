package org.zstack.storage.snapshot;

import java.util.Collections;

/**
 * Created by MaJin on 2021/6/21.
 */
public class VolumeSnapshotTagHelper {
    public static String getBackingVolumeTag(String volumeUuid) {
        return VolumeSnapshotSystemTags.BACKING_TO_VOLUME.instantiateTag(Collections.singletonMap(
                VolumeSnapshotSystemTags.BACKING_VOLUME_TOKEN, volumeUuid
        ));
    }


}
