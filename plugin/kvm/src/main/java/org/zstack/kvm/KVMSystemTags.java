package org.zstack.kvm;

import org.zstack.header.host.HostVO;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.volume.VolumeVO;
import org.zstack.tag.PatternedSystemTag;
import org.zstack.tag.SystemTag;

/**
 */
@TagDefinition
public class KVMSystemTags {
    public static final String QEMU_IMG_VERSION_TOKEN = "version";
    public static PatternedSystemTag QEMU_IMG_VERSION = new PatternedSystemTag(String.format("qemu-img::version::{%s}", QEMU_IMG_VERSION_TOKEN), HostVO.class);

    public static final String LIBVIRT_VERSION_TOKEN = "version";
    public static PatternedSystemTag LIBVIRT_VERSION = new PatternedSystemTag(String.format("libvirt::version::{%s}", LIBVIRT_VERSION_TOKEN), HostVO.class);

    public static final String HVM_CPU_FLAG_TOKEN = "flag";
    public static PatternedSystemTag HVM_CPU_FLAG = new PatternedSystemTag(String.format("hvm::{%s}", HVM_CPU_FLAG_TOKEN), HostVO.class);

    public static SystemTag VIRTIO_SCSI = new SystemTag("capability:virtio-scsi", HostVO.class);

    public static final String L2_BRIDGE_NAME_TOKEN = "name";
    public static PatternedSystemTag L2_BRIDGE_NAME = new PatternedSystemTag(String.format("kvm::bridge::{%s}", L2_BRIDGE_NAME_TOKEN), L2NetworkVO.class);

    public static SystemTag VOLUME_VIRTIO_SCSI = new SystemTag("capability::virtio-scsi", VolumeVO.class);

    public static final String DISK_OFFERING_VIRTIO_SCSI_TOKEN = "diskOfferingUuid";
    public static final String DISK_OFFERING_VIRTIO_SCSI_NUM_TOKEN = "number";
    public static PatternedSystemTag DISK_OFFERING_VIRTIO_SCSI = new PatternedSystemTag(
            String.format("virtio::diskOffering::{%s}::num::{%s}",
                    DISK_OFFERING_VIRTIO_SCSI_TOKEN, DISK_OFFERING_VIRTIO_SCSI_NUM_TOKEN), VmInstanceVO.class);

    public static final String VOLUME_WWN_TOKEN = "wwn";
    public static PatternedSystemTag VOLUME_WWN = new PatternedSystemTag(String.format("kvm::volume::{%s}", VOLUME_WWN_TOKEN), VolumeVO.class);
}
