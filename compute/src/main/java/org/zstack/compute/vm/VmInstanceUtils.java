package org.zstack.compute.vm;

import org.apache.commons.collections.CollectionUtils;
import org.zstack.core.Platform;
import org.zstack.core.db.Q;
import org.zstack.header.configuration.InstanceOfferingInventory;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.image.ImageVO;
import org.zstack.header.image.ImageVO_;
import org.zstack.header.vm.APIChangeInstanceOfferingMsg;
import org.zstack.header.vm.APICreateVmInstanceMsg;
import org.zstack.header.vm.CreateVmInstanceMsg;
import org.zstack.header.vm.UpdateVmInstanceMsg;
import org.zstack.header.vm.UpdateVmInstanceSpec;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.tag.SystemTagUtils;

import java.util.List;
import java.util.Objects;

import static java.util.Objects.requireNonNull;
/**
 * Created by Wenhao.Zhang on 22/03/10
 */
public class VmInstanceUtils {
    public static CreateVmInstanceMsg fromAPICreateVmInstanceMsg(APICreateVmInstanceMsg msg) {
        CreateVmInstanceMsg cmsg = NewVmInstanceMsgBuilder.fromAPINewVmInstanceMsg(msg);

        boolean useVirtioSettingOfImage = msg.getVirtio() == null;

        cmsg.setImageUuid(msg.getImageUuid());
        // create vm without image is supported
        if (msg.getImageUuid() != null) {
            ImageVO image = Q.New(ImageVO.class).eq(ImageVO_.uuid, msg.getImageUuid()).find();
            cmsg.setPlatform(msg.getPlatform() == null ? image.getPlatform().toString() : msg.getPlatform());
            cmsg.setGuestOsType(msg.getGuestOsType() == null ? image.getGuestOsType() : msg.getGuestOsType());
            cmsg.setArchitecture(msg.getArchitecture() == null ? image.getArchitecture() : msg.getArchitecture());

            if (useVirtioSettingOfImage) {
                cmsg.setVirtio(image.getVirtio());
            }
        } else {
            cmsg.setPlatform(msg.getPlatform());
            cmsg.setArchitecture(msg.getArchitecture());
            cmsg.setGuestOsType(msg.getGuestOsType());
        }

        if (!useVirtioSettingOfImage) {
            cmsg.setVirtio(msg.isVirtio());
        }

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

        cmsg.setDiskAOs(msg.getDiskAOs());

        if (CollectionUtils.isNotEmpty(msg.getDataDiskOfferingUuids()) || CollectionUtils.isNotEmpty(msg.getDataDiskSizes())) {
            cmsg.setPrimaryStorageUuidForDataVolume(getPSUuidForDataVolume(msg.getSystemTags()));
        }

        cmsg.setSshKeyPairUuids(msg.getSshKeyPairUuids());

        return cmsg;
    }

    private static String getPSUuidForDataVolume(List<String> systemTags){
        if (systemTags == null || systemTags.isEmpty()){
            return null;
        }

        return SystemTagUtils.findTagValue(systemTags, VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME, VmSystemTags.PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN);
    }

    public static UpdateVmInstanceSpec convertToSpec(UpdateVmInstanceMsg message, VmInstanceVO vm) {
        requireNonNull(message);
        String vmUuid = requireNonNull(requireNonNull(vm).getUuid());

        UpdateVmInstanceSpec spec = new UpdateVmInstanceSpec();
        spec.setVmInstanceUuid(vmUuid);

        if (vm.getHostUuid() != null) {
            spec.setHostUuid(vm.getHostUuid());
        } else if (vm.getLastHostUuid() != null) {
            spec.setHostUuid(vm.getLastHostUuid());
        } else {
            throw new OperationFailureException(Platform.operr("failed to find host of vm[uuid=%s]", vmUuid));
        }

        if (!Objects.equals(vm.getName(), message.getName())) {
            spec.setName(message.getName());
        }
        if (!Objects.equals(vm.getCpuNum(), message.getCpuNum())) {
            spec.setCpuNum(message.getCpuNum());
        }
        if (!Objects.equals(vm.getMemorySize(), message.getMemorySize())) {
            spec.setMemorySize(message.getMemorySize());
        }
        if (!Objects.equals(vm.getReservedMemorySize(), message.getReservedMemorySize())) {
            spec.setReservedMemorySize(message.getReservedMemorySize());
        }

        return spec;
    }

    public static UpdateVmInstanceSpec convertToSpec(APIChangeInstanceOfferingMsg message,
                                                     InstanceOfferingInventory inv,
                                                     VmInstanceVO vm) {
        requireNonNull(message);
        final String vmUuid = requireNonNull(vm.getUuid());

        UpdateVmInstanceSpec spec = new UpdateVmInstanceSpec();
        spec.setVmInstanceUuid(vmUuid);

        if (vm.getHostUuid() != null) {
            spec.setHostUuid(vm.getHostUuid());
        } else if (vm.getLastHostUuid() != null) {
            spec.setHostUuid(vm.getLastHostUuid());
        } else {
            throw new OperationFailureException(Platform.operr("failed to find host of vm[uuid=%s]", vmUuid));
        }

        if (!Objects.equals(vm.getCpuNum(), inv.getCpuNum())) {
            spec.setCpuNum(inv.getCpuNum());
        }
        if (!Objects.equals(vm.getMemorySize(), inv.getMemorySize())) {
            spec.setMemorySize(inv.getMemorySize());
        }

        return spec;
    }
}
