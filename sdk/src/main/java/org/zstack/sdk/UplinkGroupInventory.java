package org.zstack.sdk;

import org.zstack.sdk.UplinkGroupType;

public class UplinkGroupInventory  {

    public long id;
    public void setId(long id) {
        this.id = id;
    }
    public long getId() {
        return this.id;
    }

    public java.lang.String interfaceName;
    public void setInterfaceName(java.lang.String interfaceName) {
        this.interfaceName = interfaceName;
    }
    public java.lang.String getInterfaceName() {
        return this.interfaceName;
    }

    public java.lang.String vSwitchUuid;
    public void setVSwitchUuid(java.lang.String vSwitchUuid) {
        this.vSwitchUuid = vSwitchUuid;
    }
    public java.lang.String getVSwitchUuid() {
        return this.vSwitchUuid;
    }

    public java.lang.String hostUuid;
    public void setHostUuid(java.lang.String hostUuid) {
        this.hostUuid = hostUuid;
    }
    public java.lang.String getHostUuid() {
        return this.hostUuid;
    }

    public UplinkGroupType type;
    public void setType(UplinkGroupType type) {
        this.type = type;
    }
    public UplinkGroupType getType() {
        return this.type;
    }

    public java.lang.String bondingUuid;
    public void setBondingUuid(java.lang.String bondingUuid) {
        this.bondingUuid = bondingUuid;
    }
    public java.lang.String getBondingUuid() {
        return this.bondingUuid;
    }

    public java.lang.String interfaceUuid;
    public void setInterfaceUuid(java.lang.String interfaceUuid) {
        this.interfaceUuid = interfaceUuid;
    }
    public java.lang.String getInterfaceUuid() {
        return this.interfaceUuid;
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

}
