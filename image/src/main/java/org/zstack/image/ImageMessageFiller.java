package org.zstack.image;

import edu.emory.mathcs.backport.java.util.Collections;
import org.zstack.compute.vm.VmExtraInfoGetter;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.image.*;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.vm.VmInstanceVO_;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;

/**
 * Created by MaJin on 2021/3/16.
 */
public class ImageMessageFiller {
    public static void fillDefault(AddImageMessage msg) {
        if (ImageConstant.ImageMediaType.DataVolumeTemplate.toString().equals(msg.getMediaType())) {
            return;
        }

        if (msg.getPlatform() == null) {
            msg.setPlatform(ImagePlatform.Linux.toString());
        }

        if (msg.getArchitecture() == null && !ImageConstant.ImageMediaType.DataVolumeTemplate.toString().equals(msg.getMediaType())) {
            msg.setArchitecture(CoreGlobalProperty.UNIT_TEST_ON ?
                    ImageArchitecture.x86_64.toString() : ImageArchitecture.defaultArch());
        }

        setBootModeIfAarch64(msg);
    }

    public static void fillFromVm(AddImageMessage msg, String vmUuid) {
        if (!ImageConstant.ImageMediaType.RootVolumeTemplate.toString().equals(msg.getMediaType())) {
            return;
        }

        if (msg.getPlatform() == null) {
            String platform = Q.New(VmInstanceVO.class).eq(VmInstanceVO_.uuid, vmUuid).select(VmInstanceVO_.platform).findValue();
            msg.setPlatform(platform == null ? ImagePlatform.Linux.toString() : platform);
        }

        if (msg.getGuestOsType() == null) {
            msg.setGuestOsType(VmExtraInfoGetter.New(vmUuid).getGuestOsType());
        }

        if (msg.getArchitecture() == null) {
            msg.setArchitecture(VmExtraInfoGetter.New(vmUuid).getArchitecture());
        }

        setBootModeIfAarch64(msg);
    }

    public static void fillFromVolume(AddImageMessage msg, String volumeUuid) {
        String vmUuid = Q.New(VolumeVO.class).eq(VolumeVO_.uuid, volumeUuid).select(VolumeVO_.vmInstanceUuid).findValue();
        if (vmUuid == null) {
            fillDefault(msg);
        } else {
            fillFromVm(msg, vmUuid);
        }
    }

    public static void fillFromSnapshot(AddImageMessage msg, String volumeSnapshotUuid) {
        String vmUuid = SQL.New("select vol.vmInstanceUuid from VolumeVO vol, VolumeSnapshotVO snap" +
                " where snap.uuid = :snapUuid" +
                " and vol.uuid = snap.volumeUuid", String.class)
                .param("snapUuid", volumeSnapshotUuid)
                .find();

        if (vmUuid == null) {
            fillDefault(msg);
        } else {
            fillFromVm(msg, vmUuid);
        }
    }

    private static void setBootModeIfAarch64(AddImageMessage msg) {
        if (ImageArchitecture.aarch64.toString().equals(msg.getArchitecture())) {
            if (msg.getSystemTags() != null) {
                msg.getSystemTags().removeIf(tag -> ImageSystemTags.BOOT_MODE.isMatch(tag));
            }

            msg.addSystemTag(ImageSystemTags.BOOT_MODE.instantiateTag(Collections.singletonMap(
                    ImageSystemTags.BOOT_MODE_TOKEN, ImageBootMode.UEFI.toString()
            )));
        }
    }
}
