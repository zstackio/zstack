package org.zstack.kvm;

import org.zstack.header.core.Completion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.vm.VmInstanceInventory;
import org.zstack.header.volume.VolumeInventory;
import org.zstack.kvm.KVMAgentCommands.AttachDataVolumeCmd;

import java.util.Map;

/**
 * Created by frank on 4/24/2015.
 */
public interface KVMAttachVolumeExtensionPoint extends KVMConvertVolumeExtensionPoint {
    default void beforeAttachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, AttachDataVolumeCmd cmd, Map data) {}

    default void beforeAttachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume,
                                    AttachDataVolumeCmd cmd, Map data, Completion completion) {
        beforeAttachVolume(host, vm, volume, cmd, data);
        completion.success();
    }

    default void rollbackAttachVolumePreparation(Map data, ErrorCode err, Completion completion) {
        completion.success();
    }

    void afterAttachVolume(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, AttachDataVolumeCmd cmd);

    default void attachVolumeFailed(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume, AttachDataVolumeCmd cmd, ErrorCode err, Map data) {}

    default void attachVolumeFailed(KVMHostInventory host, VmInstanceInventory vm, VolumeInventory volume,
                                    AttachDataVolumeCmd cmd, ErrorCode err, Map data, Completion completion) {
        attachVolumeFailed(host, vm, volume, cmd, err, data);
        completion.success();
    }
}
