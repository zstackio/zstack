package org.zstack.sdk;

import org.zstack.sdk.ZdfsService;

public class ModelCenterServiceInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.util.List serviceStatuses;
    public void setServiceStatuses(java.util.List serviceStatuses) {
        this.serviceStatuses = serviceStatuses;
    }
    public java.util.List getServiceStatuses() {
        return this.serviceStatuses;
    }

    public ZdfsService zdfs;
    public void setZdfs(ZdfsService zdfs) {
        this.zdfs = zdfs;
    }
    public ZdfsService getZdfs() {
        return this.zdfs;
    }

}
