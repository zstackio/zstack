package org.zstack.header.storage.addon.primary;

import org.zstack.header.core.Completion;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.volume.VolumeStats;

public interface ExternalPrimaryStorageImageDownloadExtensionPoint {
    void beforeDownload(String primaryStorageUuid, String primaryStorageType, ImageInventory image,
                        CreateVolumeSpec spec, String targetResourceType);

    void afterDownload(String primaryStorageUuid, String primaryStorageType, ImageInventory image,
                       CreateVolumeSpec spec, String targetResourceType, VolumeStats volume, Completion completion);
}
