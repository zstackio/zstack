package org.zstack.sdk;

public class GetCandidatePrimaryStoragesForCreatingVmResult {
    public java.util.List<PrimaryStorageInventory> rootVolumePrimaryStorages;
    public void setRootVolumePrimaryStorages(java.util.List<PrimaryStorageInventory> rootVolumePrimaryStorages) {
        this.rootVolumePrimaryStorages = rootVolumePrimaryStorages;
    }
    public java.util.List<PrimaryStorageInventory> getRootVolumePrimaryStorages() {
        return this.rootVolumePrimaryStorages;
    }

    public java.util.Map dataVolumePrimaryStorages;
    public void setDataVolumePrimaryStorages(java.util.Map dataVolumePrimaryStorages) {
        this.dataVolumePrimaryStorages = dataVolumePrimaryStorages;
    }
    public java.util.Map getDataVolumePrimaryStorages() {
        return this.dataVolumePrimaryStorages;
    }

}
