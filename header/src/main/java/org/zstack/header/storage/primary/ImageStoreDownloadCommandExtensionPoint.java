package org.zstack.header.storage.primary;

import java.util.Map;

public interface ImageStoreDownloadCommandExtensionPoint {
    void prepare(String imageUuid, String hostUuid, Map<String, Object> addons);
}
