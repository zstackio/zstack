package org.zstack.compute.vm;

import org.zstack.header.tag.TagDefinition;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.tag.PatternedSystemTag;
import org.zstack.tag.SensitiveTag;
import org.zstack.tag.SystemTag;

/**
 */
@TagDefinition
public class VmSystemTags {
    public static String HOSTNAME_TOKEN = "hostname";
    public static PatternedSystemTag HOSTNAME = new PatternedSystemTag(String.format("hostname::{%s}", HOSTNAME_TOKEN), VmInstanceVO.class);

    public static String STATIC_IP_L3_UUID_TOKEN = "l3NetworkUuid";
    public static String STATIC_IP_TOKEN = "staticIp";
    public static PatternedSystemTag STATIC_IP = new PatternedSystemTag(String.format("staticIp::{%s}::{%s}", STATIC_IP_L3_UUID_TOKEN, STATIC_IP_TOKEN), VmInstanceVO.class);

    public static String MAC_TOKEN = "customMac";
    public static PatternedSystemTag CUSTOM_MAC = new PatternedSystemTag(String.format("customMac::{%s}::{%s}", STATIC_IP_L3_UUID_TOKEN, MAC_TOKEN), VmInstanceVO.class);

    public static PatternedSystemTag WINDOWS_VOLUME_ON_VIRTIO = new PatternedSystemTag("windows::virtioVolume", VmInstanceVO.class);

    public static String USERDATA_TOKEN = "userdata";
    public static PatternedSystemTag USERDATA = new PatternedSystemTag(String.format("userdata::{%s}", USERDATA_TOKEN), VmInstanceVO.class);

    public static String SSHKEY_TOKEN = "sshkey";
    public static PatternedSystemTag SSHKEY = new PatternedSystemTag(String.format("sshkey::{%s}", SSHKEY_TOKEN), VmInstanceVO.class);

    public static String ROOT_PASSWORD_TOKEN = "rootPassword";
    public static PatternedSystemTag ROOT_PASSWORD = new PatternedSystemTag(String.format("rootPassword::{%s}", ROOT_PASSWORD_TOKEN), VmInstanceVO.class);

    @Deprecated
    public static String ISO_DEVICEID_TOKEN = "deviceId";
    @Deprecated
    public static String ISO_TOKEN = "iso";
    @Deprecated
    public static PatternedSystemTag ISO = new PatternedSystemTag(String.format("iso::{%s}::{%s}", ISO_TOKEN, ISO_DEVICEID_TOKEN), VmInstanceVO.class);

    public static String BOOT_ORDER_TOKEN = "bootOrder";
    public static PatternedSystemTag BOOT_ORDER = new PatternedSystemTag(String.format("bootOrder::{%s}", BOOT_ORDER_TOKEN), VmInstanceVO.class);

    //this tag is deprecated.
    public static String CDROM_BOOT_ONCE_TOKEN = "cdromBootOnce";
    public static PatternedSystemTag CDROM_BOOT_ONCE = new PatternedSystemTag(String.format("cdromBootOnce::{%s}", CDROM_BOOT_ONCE_TOKEN), VmInstanceVO.class);

    public static String BOOT_ORDER_ONCE_TOKEN = "bootOrderOnce";
    public static PatternedSystemTag BOOT_ORDER_ONCE = new PatternedSystemTag(String.format("bootOrderOnce::{%s}", BOOT_ORDER_ONCE_TOKEN), VmInstanceVO.class);

    public static String CONSOLE_PASSWORD_TOKEN = "consolePassword";
    @SensitiveTag(tokens = {"consolePassword"})
    public static PatternedSystemTag CONSOLE_PASSWORD = new PatternedSystemTag(String.format("consolePassword::{%s}",CONSOLE_PASSWORD_TOKEN),VmInstanceVO.class);

    // set usbRedirect::true to enable usb redirect
    public static String USB_REDIRECT_TOKEN = "usbRedirect";
    public static PatternedSystemTag USB_REDIRECT = new PatternedSystemTag(String.format("usbRedirect::{%s}",USB_REDIRECT_TOKEN),VmInstanceVO.class);

    // set rdpEnable::true to enable RDP tag
    public static String RDP_ENABLE_TOKEN = "RDPEnable";
    public static PatternedSystemTag RDP_ENABLE = new PatternedSystemTag(String.format("RDPEnable::{%s}",RDP_ENABLE_TOKEN),VmInstanceVO.class);

    // set VDIMonitorNumber::Integer to set how many monitor will be support for VDI
    public static String VDI_MONITOR_NUMBER_TOKEN = "VDIMonitorNumber";
    public static PatternedSystemTag VDI_MONITOR_NUMBER = new PatternedSystemTag(String.format("VDIMonitorNumber::{%s}",VDI_MONITOR_NUMBER_TOKEN),VmInstanceVO.class);

    public static String INSTANCEOFFERING_ONLINECHANGE_TOKEN = "instanceOfferingOnliechange";
    public static PatternedSystemTag INSTANCEOFFERING_ONLIECHANGE = new PatternedSystemTag(String.format("instanceOfferingOnlinechange::{%s}",INSTANCEOFFERING_ONLINECHANGE_TOKEN),VmInstanceVO.class);

    public static String PENDING_CAPACITY_CHNAGE_CPU_NUM_TOKEN = "cpuNum";
    public static String PENDING_CAPACITY_CHNAGE_CPU_SPEED_TOKEN = "cpuSpeed";
    public static String PENDING_CAPACITY_CHNAGE_MEMORY_TOKEN = "memory";
    public static PatternedSystemTag PENDING_CAPACITY_CHANGE = new PatternedSystemTag(
            String.format("pendingCapacityChange::cpuNum::{%s}::cpuSpeed::{%s}::memory::{%s}",  PENDING_CAPACITY_CHNAGE_CPU_NUM_TOKEN, PENDING_CAPACITY_CHNAGE_CPU_SPEED_TOKEN, PENDING_CAPACITY_CHNAGE_MEMORY_TOKEN),
            VmInstanceVO.class
    );
    public static String VM_INJECT_QEMUGA_TOKEN = "qemuga";
    public static PatternedSystemTag VM_INJECT_QEMUGA = new PatternedSystemTag(String.format("%s", VM_INJECT_QEMUGA_TOKEN), VmInstanceVO.class);

    public static String PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN = "primaryStorageUuidForDataVolume";
    public static PatternedSystemTag PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME = new PatternedSystemTag(String.format("primaryStorageUuidForDataVolume::{%s}", PRIMARY_STORAGE_UUID_FOR_DATA_VOLUME_TOKEN), VmInstanceVO.class);

    public static final String VM_SYSTEM_SERIAL_NUMBER_TOKEN = "vmSystemSerialNumber";
    public static PatternedSystemTag VM_SYSTEM_SERIAL_NUMBER = new PatternedSystemTag(String.format("vmSystemSerialNumber::{%s}", VM_SYSTEM_SERIAL_NUMBER_TOKEN), VmInstanceVO.class);

    public static String RELEASE_NIC_AFTER_DETACH_NIC_TOKEN = "releaseVmNicAfterDetachVmNic";
    public static PatternedSystemTag RELEASE_NIC_AFTER_DETACH_NIC = new PatternedSystemTag(String.format("releaseVmNicAfterDetachVmNic::{%s}", RELEASE_NIC_AFTER_DETACH_NIC_TOKEN), VmInstanceVO.class);

    public static String BOOT_MODE_TOKEN = "bootMode";
    public static PatternedSystemTag BOOT_MODE = new PatternedSystemTag(String.format("bootMode::{%s}", BOOT_MODE_TOKEN), VmInstanceVO.class);

    public static String BOOT_VOLUME_TOKEN = "bootVolume";
    public static PatternedSystemTag BOOT_VOLUME = new PatternedSystemTag(String.format("bootVolume::{%s}", BOOT_VOLUME_TOKEN), VmInstanceVO.class);

    public static PatternedSystemTag ADDITIONAL_QMP_ADDED = new PatternedSystemTag("additionalQmp", VmInstanceVO.class);

    public static String CLEAN_TRAFFIC_TOKEN = "cleanTraffic";
    public static PatternedSystemTag CLEAN_TRAFFIC = new PatternedSystemTag(String.format("cleanTraffic::{%s}", CLEAN_TRAFFIC_TOKEN), VmInstanceVO.class);

    public static String AUTO_SCALING_GROUP_UUID_TOKEN = "autoScalingGroupUuid";
    public static PatternedSystemTag AUTO_SCALING_GROUP_UUID = new PatternedSystemTag(String.format("autoScalingGroupUuid::{%s}", AUTO_SCALING_GROUP_UUID_TOKEN), VmInstanceVO.class);

    // only use by create vm
    public static String CD_ROM_LIST_TOKEN = "cdroms";
    public static String CD_ROM_0 = "cdrom0";
    public static String CD_ROM_1 = "cdrom1";
    public static String CD_ROM_2 = "cdrom2";
    public static PatternedSystemTag CREATE_VM_CD_ROM_LIST = new PatternedSystemTag(String.format("cdroms::{%s}::{%s}::{%s}", CD_ROM_0, CD_ROM_1, CD_ROM_2), VmInstanceVO.class);

    public static String CREATE_WITHOUT_CD_ROM_TOKEN = "createWithoutCdRom";
    public static PatternedSystemTag CREATE_WITHOUT_CD_ROM = new PatternedSystemTag(String.format("createWithoutCdRom::{%s}", CREATE_WITHOUT_CD_ROM_TOKEN), VmInstanceVO.class);

    public static String CD_ROM_UUID_TOKEN = "cdromUuid";
    public static PatternedSystemTag CD_ROM = new PatternedSystemTag(String.format("cdromUuid::{%s}", CD_ROM_UUID_TOKEN), VmInstanceVO.class);

    @Deprecated
    public static final String V2V_VM_CDROMS_TOKEN = "v2vVmCdroms";
    @Deprecated
    public static PatternedSystemTag V2V_VM_CDROMS = new PatternedSystemTag(
            String.format("v2vVmCdroms::{%s}", V2V_VM_CDROMS_TOKEN),
            VmInstanceVO.class
    );

    public static final String MACHINE_TYPE_TOKEN = "vmMachineType";
    public static PatternedSystemTag MACHINE_TYPE = new PatternedSystemTag(
            String.format("vmMachineType::{%s}", MACHINE_TYPE_TOKEN),
            VmInstanceVO.class
    );

    public static PatternedSystemTag PACKER_BUILD = new PatternedSystemTag("packer", VmInstanceVO.class);
    public static final String VM_PRIORITY_TOKEN = "vmPriority";
    public static PatternedSystemTag VM_PRIORITY = new PatternedSystemTag(String.format("vmPriority::{%s}", VM_PRIORITY_TOKEN), VmInstanceVO.class
    );

    public static String SOUND_TYPE_TOKEN = "soundType";
    public static PatternedSystemTag SOUND_TYPE = new PatternedSystemTag(String.format("soundType::{%s}", SOUND_TYPE_TOKEN), VmInstanceVO.class);

    public static String QXL_RAM_TOKEN = "ram";
    public static String QXL_VRAM_TOKEN = "vram";
    public static String QXL_VGAMEM_TOKEN = "vgamem";
    public static PatternedSystemTag QXL_MEMORY = new PatternedSystemTag(String.format("qxlMemory::{%s}::{%s}::{%s}", QXL_RAM_TOKEN, QXL_VRAM_TOKEN, QXL_VGAMEM_TOKEN), VmInstanceVO.class);

    public static String MULTIPLE_GATEWAY_TOKEN = "vmMultipleGateway";
    public static PatternedSystemTag MULTIPLE_GATEWAY = new PatternedSystemTag(String.format("vmMultipleGateway::{%s}", MULTIPLE_GATEWAY_TOKEN), VmInstanceVO.class);

    public static String VM_GUEST_TOOLS_VERSION_TOKEN = "guestToolsVersion";
    public static PatternedSystemTag VM_GUEST_TOOLS =
            new PatternedSystemTag(String.format("GuestTools::{%s}", VM_GUEST_TOOLS_VERSION_TOKEN), VmInstanceVO.class);

    public static String VM_RESOURCE_BINGDING_TOKEN = "resourceUUids";
    public static PatternedSystemTag VM_RESOURCE_BINGDING =
            new PatternedSystemTag(String.format("resourceBindings::{%s}", VM_RESOURCE_BINGDING_TOKEN), VmInstanceVO.class);

    public static String RX_SIZE_TOKEN = "rxBufferSize";
    public static String TX_SIZE_TOKEN = "txBufferSize";
    public static PatternedSystemTag VM_VRING_BUFFER_SIZE = new PatternedSystemTag(String.format("vRingBufferSize::{%s}::{%s}", RX_SIZE_TOKEN, TX_SIZE_TOKEN), VmInstanceVO.class);

    public static SystemTag VIRTIO = new SystemTag("driver::virtio", VmInstanceVO.class);

    public static String VM_IP_CHANGED_TOKEN = "ipChanged";
    public static PatternedSystemTag VM_IP_CHANGED =
            new PatternedSystemTag(String.format("ipChanged::{%s}", VM_IP_CHANGED_TOKEN), VmInstanceVO.class);

    public static String L3_UUID_TOKEN = "l3Uuid";
    public static String SECURITY_GROUP_UUIDS_TOKEN = "securityGroupUuids";
    public static PatternedSystemTag L3_NETWORK_SECURITY_GROUP_UUIDS_REF =
            new PatternedSystemTag(String.format("l3::{%s}::SecurityGroupUuids::{%s}", L3_UUID_TOKEN, SECURITY_GROUP_UUIDS_TOKEN),
                    VmInstanceVO.class);
}
