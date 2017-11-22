package org.zstack.header.allocator;

import java.util.List;

public interface PreCloneVmInstanceSystemTagsExtensionPoint {
    void cloneVmInstanceSystemTag(String dstUuid, List<String> tags);
}
