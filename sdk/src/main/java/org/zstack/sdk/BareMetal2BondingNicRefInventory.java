package org.zstack.sdk;

import org.zstack.sdk.VmNicInventory;
import org.zstack.sdk.BareMetal2BondingInventory;

public class BareMetal2BondingNicRefInventory  {

    public java.lang.Long id;
    public void setId(java.lang.Long id) {
        this.id = id;
    }
    public java.lang.Long getId() {
        return this.id;
    }

    public java.lang.String nicUuid;
    public void setNicUuid(java.lang.String nicUuid) {
        this.nicUuid = nicUuid;
    }
    public java.lang.String getNicUuid() {
        return this.nicUuid;
    }

    public java.lang.String instanceUuid;
    public void setInstanceUuid(java.lang.String instanceUuid) {
        this.instanceUuid = instanceUuid;
    }
    public java.lang.String getInstanceUuid() {
        return this.instanceUuid;
    }

    public java.lang.String bondingUuid;
    public void setBondingUuid(java.lang.String bondingUuid) {
        this.bondingUuid = bondingUuid;
    }
    public java.lang.String getBondingUuid() {
        return this.bondingUuid;
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

    public VmNicInventory vmNic;
    public void setVmNic(VmNicInventory vmNic) {
        this.vmNic = vmNic;
    }
    public VmNicInventory getVmNic() {
        return this.vmNic;
    }

    public BareMetal2BondingInventory bareMetal2Bonding;
    public void setBareMetal2Bonding(BareMetal2BondingInventory bareMetal2Bonding) {
        this.bareMetal2Bonding = bareMetal2Bonding;
    }
    public BareMetal2BondingInventory getBareMetal2Bonding() {
        return this.bareMetal2Bonding;
    }

}
