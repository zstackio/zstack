package org.zstack.header.storage.primary;

import org.zstack.header.message.MessageReply;

import java.util.HashMap;
import java.util.Map;

public class ConvertVolumeEncryptionOnPrimaryStorageReply extends MessageReply {
    private Map<String, Long> actualSizes = new HashMap<>();
    private String hostUuid;

    public Map<String, Long> getActualSizes() {
        return actualSizes;
    }

    public void setActualSizes(Map<String, Long> actualSizes) {
        this.actualSizes = actualSizes;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }
}
