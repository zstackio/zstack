package org.zstack.header.storage.addon.primary;

import org.zstack.header.core.workflow.Flow;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.volume.VolumeStats;

import java.util.function.Supplier;

public interface ExternalPrimaryStorageImageDownloadExtensionPoint {
    void beforeDownload(String primaryStorageUuid, String primaryStorageType, ImageInventory image,
                        CreateVolumeSpec spec, String targetResourceType);

    Flow afterDownloadFlow(String primaryStorageUuid, String primaryStorageType, ImageInventory image,
                           CreateVolumeSpec spec, String targetResourceType,
                           Supplier<VolumeStats> volumeSupplier);
}
