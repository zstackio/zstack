package org.zstack.kvm.efi;

import org.zstack.compute.vm.VmGlobalConfig;
import org.zstack.compute.vm.VmSystemTags;
import org.zstack.compute.vm.devices.VmTpmManager;
import org.zstack.core.db.Q;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupRefVO;
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupRefVO_;
import org.zstack.header.volume.VolumeType;
import org.zstack.kvm.KVMSystemTags;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.tag.PatternedSystemTag;
import org.zstack.tag.SystemTagCreator;

import java.util.Collections;

public class SecureBootRecordHelper {
    public static boolean secureBootEnabled(String vmUuid, ResourceConfigFacade rcf) {
        String bootMode = VmSystemTags.BOOT_MODE.getTokenByResourceUuid(vmUuid, VmSystemTags.BOOT_MODE_TOKEN);
        return VmTpmManager.isUefiBootMode(bootMode)
                && Boolean.TRUE.equals(rcf.getResourceConfigValue(VmGlobalConfig.ENABLE_UEFI_SECURE_BOOT, vmUuid, Boolean.class));
    }

    public static void recordSecureBoot(PatternedSystemTag tag, String resourceUuid) {
        SystemTagCreator creator = tag.newSystemTagCreator(resourceUuid);
        creator.setTagByTokens(Collections.singletonMap(KVMSystemTags.SECURE_BOOT_TOKEN, Boolean.TRUE.toString()));
        creator.inherent = true;
        creator.recreate = true;
        creator.create();
    }

    public static boolean isSecureBootRecorded(PatternedSystemTag tag, String resourceUuid) {
        return Boolean.parseBoolean(tag.getTokenByResourceUuid(resourceUuid, KVMSystemTags.SECURE_BOOT_TOKEN));
    }

    public static boolean secureBootOfSnapshotGroup(String groupUuid) {
        if (isSecureBootRecorded(KVMSystemTags.SNAPSHOT_GROUP_SECURE_BOOT, groupUuid)) {
            return true;
        }

        String rootSnapshotUuid = Q.New(VolumeSnapshotGroupRefVO.class)
                .select(VolumeSnapshotGroupRefVO_.volumeSnapshotUuid)
                .eq(VolumeSnapshotGroupRefVO_.volumeSnapshotGroupUuid, groupUuid)
                .eq(VolumeSnapshotGroupRefVO_.volumeType, VolumeType.Root.toString())
                .limit(1)
                .findValue();
        return rootSnapshotUuid != null && isSecureBootRecorded(KVMSystemTags.VOLUME_SNAPSHOT_SECURE_BOOT, rootSnapshotUuid);
    }

    public static boolean secureBootOfVolumeSnapshot(String snapshotUuid) {
        if (isSecureBootRecorded(KVMSystemTags.VOLUME_SNAPSHOT_SECURE_BOOT, snapshotUuid)) {
            return true;
        }

        String groupUuid = Q.New(VolumeSnapshotGroupRefVO.class)
                .select(VolumeSnapshotGroupRefVO_.volumeSnapshotGroupUuid)
                .eq(VolumeSnapshotGroupRefVO_.volumeSnapshotUuid, snapshotUuid)
                .limit(1)
                .findValue();
        return groupUuid != null && isSecureBootRecorded(KVMSystemTags.SNAPSHOT_GROUP_SECURE_BOOT, groupUuid);
    }
}
