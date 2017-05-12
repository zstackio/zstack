package org.zstack.sdk;

public class BaremetalHostCfgInventory  {

    public java.lang.String uuid;
    public void setUuid(java.lang.String uuid) {
        this.uuid = uuid;
    }
    public java.lang.String getUuid() {
        return this.uuid;
    }

    public java.lang.String chessisUuid;
    public void setChessisUuid(java.lang.String chessisUuid) {
        this.chessisUuid = chessisUuid;
    }
    public java.lang.String getChessisUuid() {
        return this.chessisUuid;
    }

    public java.lang.String password;
    public void setPassword(java.lang.String password) {
        this.password = password;
    }
    public java.lang.String getPassword() {
        return this.password;
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

    public java.util.List<BaremetalHostNicCfgStruct> nicCfgs;
    public void setNicCfgs(java.util.List<BaremetalHostNicCfgStruct> nicCfgs) {
        this.nicCfgs = nicCfgs;
    }
    public java.util.List<BaremetalHostNicCfgStruct> getNicCfgs() {
        return this.nicCfgs;
    }

}
