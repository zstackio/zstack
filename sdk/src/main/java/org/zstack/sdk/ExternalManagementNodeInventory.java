package org.zstack.sdk;

import org.zstack.sdk.ExternalManagementNodeStatus;

public class ExternalManagementNodeInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String hostName;
    public void setHostName(java.lang.String hostName) {
        this.hostName = hostName;
    }
    public java.lang.String getHostName() {
        return this.hostName;
    }

    public int port;
    public void setPort(int port) {
        this.port = port;
    }
    public int getPort() {
        return this.port;
    }

    public java.lang.String accessKeyId;
    public void setAccessKeyId(java.lang.String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }
    public java.lang.String getAccessKeyId() {
        return this.accessKeyId;
    }

    public java.lang.String accessKeySecret;
    public void setAccessKeySecret(java.lang.String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
    }
    public java.lang.String getAccessKeySecret() {
        return this.accessKeySecret;
    }

    public ExternalManagementNodeStatus status;
    public void setStatus(ExternalManagementNodeStatus status) {
        this.status = status;
    }
    public ExternalManagementNodeStatus getStatus() {
        return this.status;
    }

}
