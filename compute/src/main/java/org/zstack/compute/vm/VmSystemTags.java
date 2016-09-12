package org.zstack.compute.vm;

import org.zstack.header.tag.TagDefinition;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.tag.PatternedSystemTag;

/**
 */
@TagDefinition
public class VmSystemTags {
    public static String HOSTNAME_TOKEN = "hostname";
    public static PatternedSystemTag HOSTNAME = new PatternedSystemTag(String.format("hostname::{%s}", HOSTNAME_TOKEN), VmInstanceVO.class);

    public static String STATIC_IP_L3_UUID_TOKEN = "l3NetworkUuid";
    public static String STATIC_IP_TOKEN = "staticIp";
    public static PatternedSystemTag STATIC_IP = new PatternedSystemTag(String.format("staticIp::{%s}::{%s}", STATIC_IP_L3_UUID_TOKEN, STATIC_IP_TOKEN), VmInstanceVO.class);

    public static PatternedSystemTag WINDOWS_VOLUME_ON_VIRTIO = new PatternedSystemTag("windows::virtioVolume", VmInstanceVO.class);

    public static String USERDATA_TOKEN = "userdata";
    public static PatternedSystemTag USERDATA = new PatternedSystemTag(String.format("userdata::{%s}", USERDATA_TOKEN), VmInstanceVO.class);

    public static String SSHKEY_TOKEN = "sshkey";
    public static PatternedSystemTag SSHKEY = new PatternedSystemTag(String.format("sshkey::{%s}", SSHKEY_TOKEN), VmInstanceVO.class);

    public static String ROOT_PASSWORD_TOKEN = "rootPassword";
    public static PatternedSystemTag ROOT_PASSWORD = new PatternedSystemTag(String.format("rootPassword::{%s}", ROOT_PASSWORD_TOKEN), VmInstanceVO.class);

    public static String ISO_TOKEN = "iso";
    public static PatternedSystemTag ISO = new PatternedSystemTag(String.format("iso::{%s}", ISO_TOKEN), VmInstanceVO.class);

    public static String BOOT_ORDER_TOKEN = "bootOrder";
    public static PatternedSystemTag BOOT_ORDER = new PatternedSystemTag(String.format("bootOrder::{%s}", BOOT_ORDER_TOKEN), VmInstanceVO.class);

    public static String CONSOLE_PASSWORD_TOKEN = "consolePassword";
    public static PatternedSystemTag CONSOLE_PASSWORD = new PatternedSystemTag(String.format("consolePassword::{%s}",CONSOLE_PASSWORD_TOKEN),VmInstanceVO.class);

    public static String INSTANCEOFFERING_ONLINECHANGE_TOKEN = "instanceOfferingOnliechange";
    public static PatternedSystemTag INSTANCEOFFERING_ONLIECHANGE = new PatternedSystemTag(String.format("instanceOfferingOnlinechange::{%s}",INSTANCEOFFERING_ONLINECHANGE_TOKEN),VmInstanceVO.class);

    public static String PENDING_CAPACITY_CHNAGE_CPU_NUM_TOKEN = "cpuNum";
    public static String PENDING_CAPACITY_CHNAGE_CPU_SPEED_TOKEN = "cpuSpeed";
    public static String PENDING_CAPACITY_CHNAGE_MEMORY_TOKEN = "memory";
    public static PatternedSystemTag PENDING_CAPACITY_CHANGE = new PatternedSystemTag(
            String.format("pendingCapacityChange::cpuNum::{%s}::cpuSpeed::{%s}::memory::{%s}",  PENDING_CAPACITY_CHNAGE_CPU_NUM_TOKEN, PENDING_CAPACITY_CHNAGE_CPU_SPEED_TOKEN, PENDING_CAPACITY_CHNAGE_MEMORY_TOKEN),
            VmInstanceVO.class
    );
}
