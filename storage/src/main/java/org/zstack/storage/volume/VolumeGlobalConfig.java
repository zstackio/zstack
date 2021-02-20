package org.zstack.storage.volume;

import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigDef;
import org.zstack.core.config.GlobalConfigDefinition;
import org.zstack.core.config.GlobalConfigValidation;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.volume.VolumeVO;
import org.zstack.resourceconfig.BindResourceConfig;

/**
 */
@GlobalConfigDefinition
public class VolumeGlobalConfig {
    public static final String CATEGORY = "volume";

    @GlobalConfigValidation
    public static GlobalConfig UPDATE_DISK_OFFERING_TO_NULL_WHEN_DELETING = new GlobalConfig(CATEGORY, "diskOffering.setNullWhenDeleting");

    @GlobalConfigValidation(validValues = {"Direct","Delay", "Never"})
    public static GlobalConfig VOLUME_DELETION_POLICY = new GlobalConfig(CATEGORY, "deletionPolicy");

    @GlobalConfigValidation(numberGreaterThan = 0)
    public static GlobalConfig VOLUME_EXPUNGE_PERIOD = new GlobalConfig(CATEGORY, "expungePeriod");

    @GlobalConfigValidation(numberGreaterThan = 1)
    public static GlobalConfig VOLUME_EXPUNGE_INTERVAL = new GlobalConfig(CATEGORY, "expungeInterval");

    @GlobalConfigValidation(numberGreaterThan = 600)
    public static GlobalConfig REFRESH_VOLUME_SIZE_INTERVAL = new GlobalConfig(CATEGORY, "refreshVolumeSizeInterval");

    @GlobalConfigValidation(validValues = {"0", "512", "4096"})
    @GlobalConfigDef(defaultValue = "0", type = Integer.class, description = "physical block size of the underlying storage")
    @BindResourceConfig({VolumeVO.class, PrimaryStorageVO.class})
    public static GlobalConfig VOLUME_PHYSICAL_BLOCK_SIZE = new GlobalConfig(CATEGORY, "physical.block.size");

    @GlobalConfigValidation
    @GlobalConfigDef(defaultValue = "AllStatus", type = String.class, description = "specify the status of the volume that allows cascading operations, example: Deleted")
    public static GlobalConfig CASCADE_ALLOWS_VOLUME_STATUS = new GlobalConfig(CATEGORY, "cascade.allowsVolumeStatus");
}
