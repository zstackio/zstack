package org.zstack.sdk;



public class ZdfsService  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public int slaveCount;
    public void setSlaveCount(int slaveCount) {
        this.slaveCount = slaveCount;
    }
    public int getSlaveCount() {
        return this.slaveCount;
    }

    public int sentinelCount;
    public void setSentinelCount(int sentinelCount) {
        this.sentinelCount = sentinelCount;
    }
    public int getSentinelCount() {
        return this.sentinelCount;
    }

    public java.util.List metaServers;
    public void setMetaServers(java.util.List metaServers) {
        this.metaServers = metaServers;
    }
    public java.util.List getMetaServers() {
        return this.metaServers;
    }

}
