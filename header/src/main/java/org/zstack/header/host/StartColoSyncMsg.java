package org.zstack.header.host;

import org.zstack.header.message.NeedReplyMessage;

public class StartColoSyncMsg extends NeedReplyMessage implements HostMessage {
    private String hostUuid;
    private String vmInstanceUuid;
    private Integer blockReplicationPort;
    private Integer nbdServerPort;
    private String secondaryVmHostIp;
    private long checkpointDelay;
    private boolean fullSync;
    private Integer nicNumber;

    @Override
    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getVmInstanceUuid() {
        return vmInstanceUuid;
    }

    public void setVmInstanceUuid(String vmInstanceUuid) {
        this.vmInstanceUuid = vmInstanceUuid;
    }

    public Integer getBlockReplicationPort() {
        return blockReplicationPort;
    }

    public void setBlockReplicationPort(Integer blockReplicationPort) {
        this.blockReplicationPort = blockReplicationPort;
    }

    public Integer getNbdServerPort() {
        return nbdServerPort;
    }

    public void setNbdServerPort(Integer nbdServerPort) {
        this.nbdServerPort = nbdServerPort;
    }

    public String getSecondaryVmHostIp() {
        return secondaryVmHostIp;
    }

    public void setSecondaryVmHostIp(String secondaryVmHostIp) {
        this.secondaryVmHostIp = secondaryVmHostIp;
    }

    public long getCheckpointDelay() {
        return checkpointDelay;
    }

    public void setCheckpointDelay(long checkpointDelay) {
        this.checkpointDelay = checkpointDelay;
    }

    public boolean isFullSync() {
        return fullSync;
    }

    public void setFullSync(boolean fullSync) {
        this.fullSync = fullSync;
    }

    public Integer getNicNumber() {
        return nicNumber;
    }

    public void setNicNumber(Integer nicNumber) {
        this.nicNumber = nicNumber;
    }
}
