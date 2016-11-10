package org.zstack.storage.primary;

import org.zstack.core.Platform;
import org.zstack.header.image.ImageConstant.ImageMediaType;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.identity.AccountManager;
import org.zstack.utils.path.PathUtil;

public class PrimaryStoragePathMaker {
    private static AccountManager acntMgr;

    private static String getAccountUuidOfResource(String resourceUuid) {
        if (acntMgr == null) {
            acntMgr = Platform.getComponentLoader().getComponent(AccountManager.class);
        }
        return acntMgr.getOwnerAccountUuidOfResource(resourceUuid);
    }

    public static String makeRootVolumeInstallPath(VolumeInventory vol) {
        return PathUtil.join("rootVolumes", "acct-" + getAccountUuidOfResource(vol.getUuid()), "vol-" + vol.getUuid(), vol.getUuid() + ".qcow2");
    }

    public static String makeDataVolumeInstallPath(String volUuid) {
        return PathUtil.join("dataVolumes", "acct-" + getAccountUuidOfResource(volUuid), "vol-" + volUuid, volUuid + ".qcow2");
    }

    public static String makeImageFromSnapshotWorkspacePath(String imageUuid) {
        return PathUtil.join("snapshotWorkspace", String.format("image-%s", imageUuid));
    }

    public static String makeCachedImageInstallPath(ImageInventory iminv) {
        if (iminv.getMediaType().equals(ImageMediaType.ISO.toString())) {
            return PathUtil.join("imagecache", "iso", iminv.getUuid(), iminv.getUuid() + ".iso");
        } else {
            return PathUtil.join("imagecache", "template", iminv.getUuid(), iminv.getUuid() + ".qcow2");
        }
    }

    public static String makeCachedImageInstallPathFromImageUuidForTemplate(String imageUuid) {
        return PathUtil.join("imagecache", "template", imageUuid, imageUuid + ".qcow2");
    }
}
