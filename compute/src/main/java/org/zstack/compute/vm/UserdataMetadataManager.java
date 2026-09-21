package org.zstack.compute.vm;

import org.zstack.header.core.Completion;

public interface UserdataMetadataManager {
    /**
     * Refreshes the metadata userdata served to a running VM.
     * Fails if the VM is not running or has no assigned host.
     */
    void refreshUserdataMetadata(String vmUuid, Completion completion);
}
