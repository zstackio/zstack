package org.zstack.header.vm;

import java.util.List;

public interface ResourceConfigMemorySnapshotExtensionPoint {
    List<ArchiveResourceConfigBundle.ResourceConfigBundle> getNeedToArchiveResourceConfig(String resourceUuid);
}
