package org.zstack.sdk;

public class BaremetalHostCfgInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String chassisUuid;
    public void setChassisUuid(java.lang.String chassisUuid) {
        this.chassisUuid = chassisUuid;
    }
    public java.lang.String getChassisUuid() {
        return this.chassisUuid;
    }

    public java.lang.Boolean vnc;
    public void setVnc(java.lang.Boolean vnc) {
        this.vnc = vnc;
    }
    public java.lang.Boolean getVnc() {
        return this.vnc;
    }

    public java.lang.Boolean unattended;
    public void setUnattended(java.lang.Boolean unattended) {
        this.unattended = unattended;
    }
    public java.lang.Boolean getUnattended() {
        return this.unattended;
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

    public java.util.List<BaremetalHostNicCfgInventory> nicCfgs;
    public void setNicCfgs(java.util.List<BaremetalHostNicCfgInventory> nicCfgs) {
        this.nicCfgs = nicCfgs;
    }
    public java.util.List<BaremetalHostNicCfgInventory> getNicCfgs() {
        return this.nicCfgs;
    }

    public java.util.List<BaremetalHostBondingInventory> bondings;
    public void setBondings(java.util.List<BaremetalHostBondingInventory> bondings) {
        this.bondings = bondings;
    }
    public java.util.List<BaremetalHostBondingInventory> getBondings() {
        return this.bondings;
    }

}
