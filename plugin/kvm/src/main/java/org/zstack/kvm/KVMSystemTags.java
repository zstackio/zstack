package org.zstack.kvm;

import org.zstack.header.host.HostVO;
import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.tag.TagDefinition;
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
}
