package org.zstack.header.vm;

import java.util.List;

public interface VmInstanceBeforeStartExtensionPoint {
    void handleSystemTag(String vmUuid, List<String> tags);
}
