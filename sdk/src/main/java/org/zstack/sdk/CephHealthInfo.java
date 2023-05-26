package org.zstack.sdk;

import org.zstack.sdk.CephHealthStatus;

public class CephHealthInfo  {

    public java.lang.String hostname;
    public void setHostname(java.lang.String hostname) {
        this.hostname = hostname;
    }
    public java.lang.String getHostname() {
        return this.hostname;
    }

    public CephHealthStatus status;
    public void setStatus(CephHealthStatus status) {
        this.status = status;
    }
    public CephHealthStatus getStatus() {
        return this.status;
    }

    public java.lang.String detail;
    public void setDetail(java.lang.String detail) {
        this.detail = detail;
    }
    public java.lang.String getDetail() {
        return this.detail;
    }

}
