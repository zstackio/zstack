package org.zstack.kvm;

import org.zstack.header.host.HostMessage;
import org.zstack.header.message.NeedReplyMessage;

public class KVMHotUnplugVmShmemMsg extends NeedReplyMessage implements HostMessage {
    private String hostUuid;
    private String vmUuid;
    private String shmemName;
    private String shmemPath;
    private long shmemSize;

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = normalize(hostUuid, "hostUuid");
    }

    public String getVmUuid() {
        return vmUuid;
    }

    public void setVmUuid(String vmUuid) {
        this.vmUuid = normalize(vmUuid, "vmUuid");
    }

    public String getShmemName() {
        return shmemName;
    }

    public void setShmemName(String shmemName) {
        this.shmemName = normalize(shmemName, "shmemName");
    }

    public String getShmemPath() {
        return shmemPath;
    }

    public void setShmemPath(String shmemPath) {
        this.shmemPath = normalize(shmemPath, "shmemPath");
    }

    public long getShmemSize() {
        return shmemSize;
    }

    public void setShmemSize(long shmemSize) {
        if (shmemSize <= 0) {
            throw new IllegalArgumentException("shmemSize must be greater than 0");
        }
        this.shmemSize = shmemSize;
    }

    private String normalize(String value, String fieldName) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be empty");
        }
        return normalized;
    }
}
