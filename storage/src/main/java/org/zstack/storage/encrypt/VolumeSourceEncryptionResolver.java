package org.zstack.storage.encrypt;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.Q;
import org.zstack.header.image.ImageConstant;
import org.zstack.header.image.ImageVO;
import org.zstack.header.image.ImageVO_;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_;
import org.zstack.header.vm.DiskAO;
import org.zstack.header.vm.VmInstanceSpec;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;

import java.util.List;

public class VolumeSourceEncryptionResolver {
    @Autowired
    private VolumeSnapshotEncryptionHelper snapshotEncryptionHelper;

    public void resolve(VmInstanceSpec spec) {
        if (spec == null) {
            return;
        }

        String rootImageUuid = spec.getImageSpec() == null || spec.getImageSpec().getInventory() == null
                ? null : spec.getImageSpec().getInventory().getUuid();
        resolve(spec.getRootDisk(), rootImageUuid);
        resolve(spec.getDataDisks());
    }

    public boolean isEncrypted(String imageUuid) {
        if (StringUtils.isBlank(imageUuid)) {
            return false;
        }

        if (snapshotEncryptionHelper.hasTemporarySnapshotImageKey(imageUuid)) {
            return true;
        }

        String imageUrl = Q.New(ImageVO.class)
                .eq(ImageVO_.uuid, imageUuid)
                .select(ImageVO_.url)
                .findValue();
        if (StringUtils.isBlank(imageUrl)) {
            return false;
        }

        if (imageUrl.startsWith("volume://")) {
            String volumeUuid = imageUrl.substring("volume://".length());
            return Boolean.TRUE.equals(Q.New(VolumeVO.class)
                    .eq(VolumeVO_.uuid, volumeUuid)
                    .select(VolumeVO_.encrypted)
                    .findValue());
        }

        String snapshotUuid;
        if (imageUrl.startsWith(ImageConstant.IMAGE_FROM_SNAPSHOT_SCHEMA)) {
            snapshotUuid = imageUrl.substring(ImageConstant.IMAGE_FROM_SNAPSHOT_SCHEMA.length());
        } else if (imageUrl.startsWith(ImageConstant.SNAPSHOT_REUSE_IMAGE_SCHEMA)) {
            snapshotUuid = imageUrl.substring(ImageConstant.SNAPSHOT_REUSE_IMAGE_SCHEMA.length());
        } else {
            return false;
        }
        snapshotUuid = snapshotUuid.length() >= 32 ? snapshotUuid.substring(0, 32) : snapshotUuid;
        return Boolean.TRUE.equals(Q.New(VolumeSnapshotVO.class)
                .eq(VolumeSnapshotVO_.uuid, snapshotUuid)
                .select(VolumeSnapshotVO_.encrypted)
                .findValue());
    }

    private void resolve(List<DiskAO> disks) {
        if (disks != null) {
            disks.forEach(disk -> resolve(disk, null));
        }
    }

    private void resolve(DiskAO disk, String defaultImageUuid) {
        if (disk == null) {
            return;
        }

        String imageUuid = StringUtils.defaultIfBlank(disk.getTemplateUuid(), defaultImageUuid);
        if (isEncrypted(imageUuid)) {
            disk.setEncrypted(true);
        }
    }
}
