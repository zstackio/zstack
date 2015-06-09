package org.zstack.storage.primary.iscsi;

/**
 * Created by frank on 6/8/2015.
 */
public interface IscsiIsoStoreManager {
    public static class IscsiIsoSpec {
        private String imageUuid;
        private String vmInstanceUuid;
        private String primaryStorageUuid;

        public String getPrimaryStorageUuid() {
            return primaryStorageUuid;
        }

        public void setPrimaryStorageUuid(String primaryStorageUuid) {
            this.primaryStorageUuid = primaryStorageUuid;
        }

        public String getImageUuid() {
            return imageUuid;
        }

        public void setImageUuid(String imageUuid) {
            this.imageUuid = imageUuid;
        }

        public String getVmInstanceUuid() {
            return vmInstanceUuid;
        }

        public void setVmInstanceUuid(String vmInstanceUuid) {
            this.vmInstanceUuid = vmInstanceUuid;
        }
    }

    IscsiIsoVO take(IscsiIsoSpec spec);

    void store(IscsiIsoVO iso);

    void releaseByVmUuid(String vmUuid);
}
