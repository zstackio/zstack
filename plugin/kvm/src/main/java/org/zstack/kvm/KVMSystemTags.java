package org.zstack.kvm;

import org.zstack.header.host.HostVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.PatternedSystemTag;

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
}
