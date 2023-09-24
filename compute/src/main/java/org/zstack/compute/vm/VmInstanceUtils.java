package org.zstack.compute.vm;

import org.apache.commons.collections.CollectionUtils;
import org.checkerframework.checker.units.qual.C;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.vm.APICreateVmInstanceMsg;
import org.zstack.header.vm.CreateVmInstanceMsg;
import org.zstack.tag.SystemTagUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.zstack.core.Platform.argerr;

/**
 * Created by Wenhao.Zhang on 22/03/10
 */
public class VmInstanceUtils {
    public static CreateVmInstanceMsg fromAPICreateVmInstanceMsg(APICreateVmInstanceMsg msg) {
        CreateVmInstanceMsg cmsg = NewVmInstanceMsgBuilder.fromAPINewVmInstanceMsg(msg);
        cmsg.setImageUuid(msg.getImageUuid());
        cmsg.setRootDiskOfferingUuid(msg.getRootDiskOfferingUuid());
        if (msg.getRootDiskSize() != null) {
            cmsg.setRootDiskSize(msg.getRootDiskSize());
        }
        cmsg.setDataDiskSizes(msg.getDataDiskSizes());
        cmsg.setDataDiskOfferingUuids(msg.getDataDiskOfferingUuids());
        cmsg.setRootVolumeSystemTags(msg.getRootVolumeSystemTags());
        cmsg.setDataVolumeSystemTags(msg.getDataVolumeSystemTags());
        cmsg.setPrimaryStorageUuidForRootVolume(msg.getPrimaryStorageUuidForRootVolume());
        cmsg.setDataVolumeSystemTagsOnIndex(msg.getDataVolumeSystemTagsOnIndex());
        if (CollectionUtils.isNotEmpty(msg.getDataDiskOfferingUuids()) || CollectionUtils.isNotEmpty(msg.getDataDiskSizes())) {
            cmsg.setPrimaryStorageUuidForDataVolume(getPSUuidForDataVolume(msg.getSystemTags()));
        }
        cmsg.setDiskAOs(msg.getDiskAOs());
        setVmInstanceInfoFromRootDiskAO(cmsg, msg);
        return cmsg;
    }

    private static String getPSUuidForDataVolume(List<String> systemTags){
        if (systemTags == null || systemTags.isEmpty()){
            return null;
        }

        return SystemTagUtils.findTagValue(systemTags, VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME, VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN);
    }

    private static void setVmInstanceInfoFromRootDiskAO(CreateVmInstanceMsg cmsg, APICreateVmInstanceMsg msg) {
        if (CollectionUtils.isEmpty(msg.getDiskAOs())) {
            return;
        }
        APICreateVmInstanceMsg.DiskAO rootdiskAO = msg.getDiskAOs().stream()
                .filter(APICreateVmInstanceMsg.DiskAO::isBoot).findFirst().orElse(null);
        if (rootdiskAO == null) {
            return;
        }
        cmsg.setPlatform(rootdiskAO.getPlatform());
        cmsg.setGuestOsType(rootdiskAO.getGuestOsType());
        cmsg.setArchitecture(rootdiskAO.getArchitecture());
        if (CollectionUtils.isNotEmpty(rootdiskAO.getSystemTags())
                && rootdiskAO.getSystemTags().contains(VmSystemTags.VIRTIO.getTagFormat())) {
            cmsg.setVirtio(true);
        }
    }
}
