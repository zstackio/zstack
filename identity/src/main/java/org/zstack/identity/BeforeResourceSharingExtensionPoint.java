package org.zstack.identity;

import java.util.List;
import java.util.Map;

public interface BeforeResourceSharingExtensionPoint {
    List<String> beforeResourceSharingExtensionPoint(Map<String, String> uuidType);
}
