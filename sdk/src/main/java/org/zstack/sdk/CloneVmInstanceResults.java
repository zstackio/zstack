package org.zstack.sdk;

public class CloneVmInstanceResults  {

    public int numberOfClonedVm;
    public void setNumberOfClonedVm(int numberOfClonedVm) {
        this.numberOfClonedVm = numberOfClonedVm;
    }
    public int getNumberOfClonedVm() {
        return this.numberOfClonedVm;
    }

    public java.util.List<CloneVmInstanceInventory> inventories;
    public void setInventories(java.util.List<CloneVmInstanceInventory> inventories) {
        this.inventories = inventories;
    }
    public java.util.List<CloneVmInstanceInventory> getInventories() {
        return this.inventories;
    }

}
