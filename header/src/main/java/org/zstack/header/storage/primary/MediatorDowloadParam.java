package org.zstack.header.storage.primary;

import org.zstack.header.vm.VmInstanceSpec;

/**
 * Created by mingjian.deng on 2017/11/24.
 */
public class MediatorDowloadParam implements MediatorParam{
    private VmInstanceSpec.ImageSpec image;
    private String installPath;
    private String primaryStorageUuid;

    public VmInstanceSpec.ImageSpec getImage() {
        return image;
    }

    public void setImage(VmInstanceSpec.ImageSpec image) {
        this.image = image;
    }

    public String getInstallPath() {
        return installPath;
    }

    public void setInstallPath(String installPath) {
        this.installPath = installPath;
    }

    public String getPrimaryStorageUuid() {
        return primaryStorageUuid;
    }

    public void setPrimaryStorageUuid(String primaryStorageUuid) {
        this.primaryStorageUuid = primaryStorageUuid;
    }
}
