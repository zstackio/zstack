package org.zstack.header.storage.addon.primary;

import org.zstack.header.core.Completion;
import org.zstack.header.image.ImageInventory;

public interface ExternalPrimaryStorageImageDownloadExtensionPoint {
    void beforeDownload(String primaryStorageUuid, String primaryStorageType, ImageInventory image,
                        CreateVolumeSpec spec, String targetResourceType);

    boolean supportsDownload(ExternalPrimaryStorageImageDownloadContext context);

    void download(ExternalPrimaryStorageImageDownloadContext context, Completion completion);
}
