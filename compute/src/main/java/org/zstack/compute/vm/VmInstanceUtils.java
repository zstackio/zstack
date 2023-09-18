package org.zstack.compute.vm;

import org.apache.commons.collections.CollectionUtils;
import org.zstack.header.image.ImageArchitecture;
import org.zstack.header.image.ImageHelper;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.vm.APICreateVmInstanceMsg;
import org.zstack.header.vm.CreateVmInstanceMsg;
import org.zstack.tag.SystemTagUtils;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.List;

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
        cmsg.setPlatform(msg.getPlatform());
        cmsg.setGuestOsType(msg.getGuestOsType());
        cmsg.setArchitecture(msg.getArchitecture());
        cmsg.setVirtio(msg.getVirtio());
        if (CollectionUtils.isNotEmpty(msg.getDataDiskOfferingUuids()) || CollectionUtils.isNotEmpty(msg.getDataDiskSizes())) {
            cmsg.setPrimaryStorageUuidForDataVolume(getPSUuidForDataVolume(msg.getSystemTags()));
        }
        cmsg.setDiskAOs(msg.getDiskAOs());
        return cmsg;
    }

    private static String getPSUuidForDataVolume(List<String> systemTags){
        if (systemTags == null || systemTags.isEmpty()){
            return null;
        }

        return SystemTagUtils.findTagValue(systemTags, VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME, VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN);
    }
}
