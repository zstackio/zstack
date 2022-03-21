package org.zstack.identity;

import java.util.List;
import java.util.Map;

public interface ResourceSharingExtensionPoint {
    List<String> beforeResourceSharingExtensionPoint(Map<String, String> uuidType);

    void afterResourceSharingExtensionPoint(Map<String, String> uuidType, List<String> accountUuids, boolean isToPublic);
}
