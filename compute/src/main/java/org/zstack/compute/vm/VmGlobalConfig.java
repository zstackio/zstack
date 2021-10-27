package org.zstack.compute.vm;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.header.cluster.ClusterVO;
import org.zstack.resourceconfig.BindResourceConfig;
import org.zstack.header.vm.VmInstanceConstant;
import org.zstack.header.vm.VmInstanceVO;

@GlobalConfigDefinition
public class VmGlobalConfig {
    public static final String CATEGORY = "vm";

    @GlobalConfigValidation
    public static GlobalConfig DELETE_DATA_VOLUME_ON_VM_DESTROY = new GlobalConfig(CATEGORY, "dataVolume.deleteOnVmDestroy");
    @GlobalConfigValidation
    public static GlobalConfig UPDATE_INSTANCE_OFFERING_TO_NULL_WHEN_DELETING = new GlobalConfig(CATEGORY, "instanceOffering.setNullWhenDeleting");
    @GlobalConfigValidation(validValues = {"Direct","Delay", "Never"})
    public static GlobalConfig VM_DELETION_POLICY = new GlobalConfig(CATEGORY, "deletionPolicy");
    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig VM_EXPUNGE_PERIOD = new GlobalConfig(CATEGORY, "expungePeriod");
    @GlobalConfigValidation(numberGreaterThan = 1)
    public static GlobalConfig VM_EXPUNGE_INTERVAL = new GlobalConfig(CATEGORY, "expungeInterval");
    @GlobalConfigValidation(validValues = {"true", "false"})
    public static GlobalConfig VM_CLEAN_TRAFFIC = new GlobalConfig(CATEGORY, "cleanTraffic");
    @GlobalConfigValidation(validValues = {"cirrus","vga", "qxl"})
    @BindResourceConfig(value = {ClusterVO.class})
    public static GlobalConfig VM_VIDEO_TYPE = new GlobalConfig(CATEGORY, "videoType");
    @GlobalConfigValidation(validValues = {"off","all", "filter"})
    public static GlobalConfig VM_SPICE_STREAMING_MODE= new GlobalConfig(CATEGORY, "spiceStreamingMode");
    @GlobalConfigValidation
    @BindResourceConfig(value = {VmInstanceVO.class, ClusterVO.class})
    public static GlobalConfig NUMA = new GlobalConfig(CATEGORY, "numa");
    @GlobalConfigValidation
    public static GlobalConfig VM_BOOT_MENU = new GlobalConfig(CATEGORY, "bootMenu");
    @GlobalConfigValidation(validValues = {"true", "false"})
    @BindResourceConfig(value = {VmInstanceVO.class, ClusterVO.class})
    public static GlobalConfig KVM_HIDDEN_STATE = new GlobalConfig(CATEGORY, "kvmHiddenState");
    @GlobalConfigValidation(validValues = {"true", "false"})
    public static GlobalConfig VM_PORT_OFF = new GlobalConfig(CATEGORY, "vmPortOff");
    @GlobalConfigValidation(validValues = {"true", "false"})
    @BindResourceConfig(value = {VmInstanceVO.class, ClusterVO.class})
    public static GlobalConfig EMULATE_HYPERV = new GlobalConfig(CATEGORY, "emulateHyperV");
    @GlobalConfigValidation(validValues = {"true", "false"})
    public static GlobalConfig ADDITIONAL_QMP = new GlobalConfig(CATEGORY, "additionalQmp");

    @GlobalConfigValidation(validValues = {"true","false"})
    public static GlobalConfig MULTI_VNIC_SUPPORT = new GlobalConfig(CATEGORY, "multivNic.support");

    @GlobalConfigValidation(numberGreaterThan = 0, numberLessThan = VmInstanceConstant.MAXIMUM_CDROM_NUMBER)
    public static GlobalConfig VM_DEFAULT_CD_ROM_NUM = new GlobalConfig(CATEGORY, "vmDefaultCdRomNum");

    @GlobalConfigValidation(numberGreaterThan = 1, numberLessThan = VmInstanceConstant.MAXIMUM_CDROM_NUMBER)
    public static GlobalConfig MAXIMUM_CD_ROM_NUM = new GlobalConfig(CATEGORY, "maximumCdRomNum");

    @GlobalConfigValidation(inNumberRange = {0, 28})
    public static GlobalConfig PCIE_PORT_NUMS = new GlobalConfig(CATEGORY, "pciePortNums");

    @GlobalConfigValidation(validValues = {"Hard", "Soft"})
    @BindResourceConfig({VmInstanceVO.class})
    public static GlobalConfig RESOURCE_BINDING_STRATEGY = new GlobalConfig(CATEGORY, "resourceBinding.strategy");

    @GlobalConfigValidation(validValues = {"Auto", "All"})
    @BindResourceConfig({VmInstanceVO.class})
    public static GlobalConfig RESOURCE_BINDING_SCENE = new GlobalConfig(CATEGORY, "resourceBinding.Scene");

    @GlobalConfigValidation(inNumberRange = {1, 256})
    @BindResourceConfig({VmInstanceVO.class, ClusterVO.class})
    public static GlobalConfig VM_NIC_MULTIQUEUE_NUM = new GlobalConfig(CATEGORY, "nicMultiQueueNum");

    @GlobalConfigValidation(numberGreaterThan = 1)
    public static GlobalConfig UNKNOWN_GC_INTERVAL = new GlobalConfig(CATEGORY, "set.unknown.gc.interval");
}
