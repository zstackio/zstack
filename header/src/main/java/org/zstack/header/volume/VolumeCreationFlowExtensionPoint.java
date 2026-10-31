package org.zstack.header.volume;

import org.zstack.header.core.workflow.Flow;

import java.util.List;

public interface VolumeCreationFlowExtensionPoint {
    List<Flow> beforeStorageWrite(VolumeCreationContext context);

    List<Flow> beforeReady(VolumeCreationContext context);
}
