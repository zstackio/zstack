package org.zstack.storage.primary.nfs;

import org.zstack.header.image.ImageInventory;
import org.zstack.header.storage.primary.PrimaryStorageInventory;
import org.zstack.header.storage.snapshot.VolumeSnapshotInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.header.volume.VolumeType;
import org.zstack.storage.primary.PrimaryStoragePathMaker;
import org.zstack.utils.path.PathUtil;

import java.io.File;

public class NfsPrimaryStorageKvmHelper {
    public static String makeRootVolumeInstallUrl(PrimaryStorageInventory pinv, VolumeInventory vol) {
        return PathUtil.join(pinv.getMountPath(), PrimaryStoragePathMaker.makeRootVolumeInstallPath(vol));
    }
    
    public static String makeDataVolumeInstallUrl(PrimaryStorageInventory pinv, String volUuid) {
        return PathUtil.join(pinv.getMountPath(), PrimaryStoragePathMaker.makeDataVolumeInstallPath(volUuid));
    }
    
    public static String makeCachedImageInstallUrl(PrimaryStorageInventory pinv, ImageInventory iminv) {
        return PathUtil.join(pinv.getMountPath(), PrimaryStoragePathMaker.makeCachedImageInstallPath(iminv));
    }

    public static String makeCachedImageInstallUrlFromImageUuidForTemplate(PrimaryStorageInventory pinv, String imageUuid) {
        return PathUtil.join(pinv.getMountPath(), PrimaryStoragePathMaker.makeCachedImageInstallPathFromImageUuidForTemplate(imageUuid));
    }

    public static String makeTemplateFromVolumeInWorkspacePath(PrimaryStorageInventory pinv, String imageUuid) {
        return PathUtil.join(pinv.getMountPath(), "templateWorkspace", String.format("image-%s", imageUuid), String.format("%s.qcow2", imageUuid));
    }

    public static String makeKvmSnapshotInstallPath(PrimaryStorageInventory pinv, VolumeInventory vol, VolumeSnapshotInventory snapshot) {
        String volPath;
        if (VolumeType.Data.toString().equals(vol.getType())) {
            volPath = makeDataVolumeInstallUrl(pinv, vol.getUuid());
        } else {
            volPath = makeRootVolumeInstallUrl(pinv, vol);
        }
        File volDir = new File(volPath).getParentFile();
        return PathUtil.join(volDir.getAbsolutePath(), "snapshots", String.format("%s.qcow2", snapshot.getUuid()));
    }

    public static String makeSnapshotWorkspacePath(PrimaryStorageInventory pinv, String imageUuid) {
        return PathUtil.join(
                pinv.getMountPath(),
                PrimaryStoragePathMaker.makeImageFromSnapshotWorkspacePath(imageUuid),
                String.format("%s.qcow2", imageUuid)
        );
    }
    
    public static String makeDownloadImageJobName(ImageInventory iminv, PrimaryStorageInventory pinv) {
        return String.format("download-image-%s-to-pri-%s", iminv.getUuid(), pinv.getUuid());
    }
    
    public static String makeJobOwnerName(PrimaryStorageInventory pinv) {
        return "NfsPrimaryStorage-" + pinv.getUuid();
    }
}
