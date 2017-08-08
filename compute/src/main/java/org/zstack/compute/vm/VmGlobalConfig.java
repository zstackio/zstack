package org.zstack.compute.vm;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;

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
    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig VM_EXPUNGE_INTERVAL = new GlobalConfig(CATEGORY, "expungeInterval");
    @GlobalConfigValidation(validValues = {"true", "false"})
    public static GlobalConfig VM_CLEAN_TRAFFIC = new GlobalConfig(CATEGORY, "cleanTraffic");
    @GlobalConfigValidation(validValues = {"cirrus","vga", "qxl"})
    public static GlobalConfig VM_VIDEO_TYPE = new GlobalConfig(CATEGORY, "videoType");
    @GlobalConfigValidation
    public static GlobalConfig NUMA = new GlobalConfig(CATEGORY, "numa");
    @GlobalConfigValidation
    public static GlobalConfig VM_BOOT_MENU = new GlobalConfig(CATEGORY, "bootMenu");
    @GlobalConfigValidation(validValues = {"true", "false"})
    public static GlobalConfig KVM_HIDDEN_STATE = new GlobalConfig(CATEGORY, "kvmHiddenState");
}
