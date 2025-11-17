package org.zstack.sdk;

import org.zstack.sdk.InstType;
import org.zstack.sdk.FuncType;

public class NfvInstGroupInventory  {

    public java.lang.String name;
    public void setName(java.lang.String name) {
        this.name = name;
    }
    public java.lang.String getName() {
        return this.name;
    }

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String description;
    public void setDescription(java.lang.String description) {
        this.description = description;
    }
    public java.lang.String getDescription() {
        return this.description;
    }

    public java.lang.String nfvInstOfferingUuid;
    public void setNfvInstOfferingUuid(java.lang.String nfvInstOfferingUuid) {
        this.nfvInstOfferingUuid = nfvInstOfferingUuid;
    }
    public java.lang.String getNfvInstOfferingUuid() {
        return this.nfvInstOfferingUuid;
    }

    public InstType instType;
    public void setInstType(InstType instType) {
        this.instType = instType;
    }
    public InstType getInstType() {
        return this.instType;
    }

    public FuncType funcType;
    public void setFuncType(FuncType funcType) {
        this.funcType = funcType;
    }
    public FuncType getFuncType() {
        return this.funcType;
    }

    public int configVersion;
    public void setConfigVersion(int configVersion) {
        this.configVersion = configVersion;
    }
    public int getConfigVersion() {
        return this.configVersion;
    }

    public java.lang.String netOsDistro;
    public void setNetOsDistro(java.lang.String netOsDistro) {
        this.netOsDistro = netOsDistro;
    }
    public java.lang.String getNetOsDistro() {
        return this.netOsDistro;
    }

    public java.lang.String baseOsDistro;
    public void setBaseOsDistro(java.lang.String baseOsDistro) {
        this.baseOsDistro = baseOsDistro;
    }
    public java.lang.String getBaseOsDistro() {
        return this.baseOsDistro;
    }

    public java.sql.Timestamp createDate;
    public void setCreateDate(java.sql.Timestamp createDate) {
        this.createDate = createDate;
    }
    public java.sql.Timestamp getCreateDate() {
        return this.createDate;
    }

    public java.sql.Timestamp lastOpDate;
    public void setLastOpDate(java.sql.Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
    public java.sql.Timestamp getLastOpDate() {
        return this.lastOpDate;
    }

    public java.util.List instances;
    public void setInstances(java.util.List instances) {
        this.instances = instances;
    }
    public java.util.List getInstances() {
        return this.instances;
    }

    public java.util.List monitors;
    public void setMonitors(java.util.List monitors) {
        this.monitors = monitors;
    }
    public java.util.List getMonitors() {
        return this.monitors;
    }

    public java.util.List services;
    public void setServices(java.util.List services) {
        this.services = services;
    }
    public java.util.List getServices() {
        return this.services;
    }

    public java.util.List configTasks;
    public void setConfigTasks(java.util.List configTasks) {
        this.configTasks = configTasks;
    }
    public java.util.List getConfigTasks() {
        return this.configTasks;
    }

    public java.util.List l3Networks;
    public void setL3Networks(java.util.List l3Networks) {
        this.l3Networks = l3Networks;
    }
    public java.util.List getL3Networks() {
        return this.l3Networks;
    }

}
