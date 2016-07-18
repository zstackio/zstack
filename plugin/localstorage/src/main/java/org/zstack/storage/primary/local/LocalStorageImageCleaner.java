package org.zstack.storage.primary.local;

import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.storage.primary.ImageCacheCleaner;

/**
 * Created by xing5 on 2016/7/20.
 */
public class LocalStorageImageCleaner extends ImageCacheCleaner implements ManagementNodeReadyExtensionPoint {
    @Override
    public void managementNodeReady() {
        startGC();
    }

    @Override
    protected String getPrimaryStorageType() {
        return LocalStorageConstants.LOCAL_STORAGE_TYPE;
    }
}
