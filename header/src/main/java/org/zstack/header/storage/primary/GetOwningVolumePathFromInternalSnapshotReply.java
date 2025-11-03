package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;

import java.util.HashMap;
import java.util.Map;

public class GetOwningVolumePathFromInternalSnapshotReply extends MessageReply {
    private Map<String, String> owningVolumePaths = new HashMap<>();

    public Map<String, String> getOwningVolumePaths() {
        return owningVolumePaths;
    }

    public void setOwningVolumePaths(Map<String, String> owningVolumePaths) {
        this.owningVolumePaths = owningVolumePaths;
    }

    public void putOwningVolumePath(String snapshotPath, String volumePath) {
        this.owningVolumePaths.put(snapshotPath, volumePath);
    }
}
