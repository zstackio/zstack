package org.zstack.compute.host;

import org.zstack.header.host.HostVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.PatternedSystemTag;
import org.zstack.tag.SystemTag;

/**
 */
@TagDefinition
public class HostSystemTags {
    public static String RESERVED_CPU_CAPACITY_TOKEN = "capacity";
    public static PatternedSystemTag RESERVED_CPU_CAPACITY = new PatternedSystemTag(String.format("reservedCpu::{%s}", RESERVED_CPU_CAPACITY_TOKEN), HostVO.class);

    public static String RESERVED_MEMORY_CAPACITY_TOKEN = "capacity";
    public static PatternedSystemTag RESERVED_MEMORY_CAPACITY = new PatternedSystemTag(String.format("reservedMemory::{%s}", RESERVED_MEMORY_CAPACITY_TOKEN), HostVO.class);

    public static SystemTag LIVE_SNAPSHOT = new SystemTag("capability::liveSnapshot", HostVO.class);

    public static String OS_DISTRIBUTION_TOKEN = "distribution";
    public static PatternedSystemTag OS_DISTRIBUTION = new PatternedSystemTag(String.format("os::distribution::{%s}", OS_DISTRIBUTION_TOKEN), HostVO.class);

    public static String OS_RELEASE_TOKEN = "release";
    public static PatternedSystemTag OS_RELEASE = new PatternedSystemTag(String.format("os::release::{%s}", OS_RELEASE_TOKEN), HostVO.class);

    public static String OS_VERSION_TOKEN = "version";
    public static PatternedSystemTag OS_VERSION = new PatternedSystemTag(String.format("os::version::{%s}", OS_VERSION_TOKEN), HostVO.class);

    public static String EXTRA_IPS_TOKEN = "extraips";
    public static PatternedSystemTag EXTRA_IPS = new PatternedSystemTag(String.format("extraips::{%s}", EXTRA_IPS_TOKEN), HostVO.class);

    public static final String HOST_CPU_MODEL_NAME_TOKEN = "hostCpuModelName";
    public static PatternedSystemTag HOST_CPU_MODEL_NAME = new PatternedSystemTag(String.format("hostCpuModelName::{%s}", HOST_CPU_MODEL_NAME_TOKEN), HostVO.class);

    public static final String SYSTEM_PRODUCT_NAME_TOKEN = "systemProductName";
    public static PatternedSystemTag SYSTEM_PRODUCT_NAME = new PatternedSystemTag(String.format("systemProductName::{%s}", SYSTEM_PRODUCT_NAME_TOKEN), HostVO.class);

    public static final String CPU_GHZ_TOKEN = "cpuGHz";
    public static PatternedSystemTag CPU_GHZ = new PatternedSystemTag(String.format("cpuGHz::{%s}", CPU_GHZ_TOKEN), HostVO.class);

    public static final String PAGE_TABLE_EXTENSION_DISABLED_TOKEN = "pageTableExtensionDisabled";
    public static PatternedSystemTag PAGE_TABLE_EXTENSION_DISABLED = new PatternedSystemTag(PAGE_TABLE_EXTENSION_DISABLED_TOKEN, HostVO.class);
}
