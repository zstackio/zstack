package org.zstack.header.tag;

import java.util.List;

/**
 */
public interface SystemTagLifeCycleExtension {
    List<String> getResourceTypeForVirtualRouterSystemTags();

    void tagCreated(SystemTagInventory tag);

    void tagDeleted(SystemTagInventory tag);
}
