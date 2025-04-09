package org.zstack.sdk;



public class MetaServerService  {

    public java.lang.String url;
    public void setUrl(java.lang.String url) {
        this.url = url;
    }
    public java.lang.String getUrl() {
        return this.url;
    }

    public boolean up;
    public void setUp(boolean up) {
        this.up = up;
    }
    public boolean getUp() {
        return this.up;
    }

    public java.lang.String role;
    public void setRole(java.lang.String role) {
        this.role = role;
    }
    public java.lang.String getRole() {
        return this.role;
    }

    public long usedMemoryInBytes;
    public void setUsedMemoryInBytes(long usedMemoryInBytes) {
        this.usedMemoryInBytes = usedMemoryInBytes;
    }
    public long getUsedMemoryInBytes() {
        return this.usedMemoryInBytes;
    }

    public long maxMemoryInBytes;
    public void setMaxMemoryInBytes(long maxMemoryInBytes) {
        this.maxMemoryInBytes = maxMemoryInBytes;
    }
    public long getMaxMemoryInBytes() {
        return this.maxMemoryInBytes;
    }

    public long connectedClients;
    public void setConnectedClients(long connectedClients) {
        this.connectedClients = connectedClients;
    }
    public long getConnectedClients() {
        return this.connectedClients;
    }

    public long maxClients;
    public void setMaxClients(long maxClients) {
        this.maxClients = maxClients;
    }
    public long getMaxClients() {
        return this.maxClients;
    }

    public boolean syncInProgress;
    public void setSyncInProgress(boolean syncInProgress) {
        this.syncInProgress = syncInProgress;
    }
    public boolean getSyncInProgress() {
        return this.syncInProgress;
    }

    public long replLagBytes;
    public void setReplLagBytes(long replLagBytes) {
        this.replLagBytes = replLagBytes;
    }
    public long getReplLagBytes() {
        return this.replLagBytes;
    }

    public java.lang.String version;
    public void setVersion(java.lang.String version) {
        this.version = version;
    }
    public java.lang.String getVersion() {
        return this.version;
    }

}
