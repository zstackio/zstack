package org.zstack.header.storage.addon.primary;

import org.zstack.header.core.Completion;

public interface ExternalPrimaryStorageImageDownloadExtensionPoint {
    boolean supports(ExternalPrimaryStorageImageDownloadContext context);

    long requiredDownloadSize(ExternalPrimaryStorageImageDownloadContext context, long defaultSize);

    void afterDownload(ExternalPrimaryStorageImageDownloadContext context, Completion completion);
}
