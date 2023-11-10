package org.zstack.storage.volume;

import org.zstack.header.core.NonCloneable;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.header.volume.VolumeVO;
import org.zstack.tag.EphemeralSystemTag;
import org.zstack.tag.PatternedSystemTag;

/**
 * Created by miao on 12/23/16.
 */
@TagDefinition
public class VolumeSystemTags {
    public static EphemeralSystemTag SHAREABLE = new EphemeralSystemTag("shareable");

    public static String VOLUME_MAX_INCREMENTAL_SNAPSHOT_NUM_TOKEN = "volumeSnapshotMaxNum";
    public static PatternedSystemTag VOLUME_MAX_INCREMENTAL_SNAPSHOT_NUM = new PatternedSystemTag(String.format("volumeMaxIncrementalSnapshotNum::{%s}", VOLUME_MAX_INCREMENTAL_SNAPSHOT_NUM_TOKEN), VolumeVO.class);

    public static String VOLUME_PROVISIONING_STRATEGY_TOKEN = "volumeProvisioningStrategy";
    public static PatternedSystemTag VOLUME_PROVISIONING_STRATEGY = new PatternedSystemTag(String.format("volumeProvisioningStrategy::{%s}", VOLUME_PROVISIONING_STRATEGY_TOKEN), VolumeVO.class);


    public static String PRIMARY_STORAGE_VOLUME_PROVISIONING_STRATEGY_TOKEN = "primaryStorageVolumeProvisioningStrategy";
    public static PatternedSystemTag PRIMARY_STORAGE_VOLUME_PROVISIONING_STRATEGY = new PatternedSystemTag(String.format("primaryStorageVolumeProvisioningStrategy::{%s}", PRIMARY_STORAGE_VOLUME_PROVISIONING_STRATEGY_TOKEN), PrimaryStorageVO.class);

    public static String AUTO_SCALING_GROUP_UUID_TOKEN = "autoScalingGroupUuid";
    public static PatternedSystemTag AUTO_SCALING_GROUP_UUID = new PatternedSystemTag(String.format("autoScalingGroupUuid::{%s}", AUTO_SCALING_GROUP_UUID_TOKEN), VolumeVO.class);

    @NonCloneable
    public static String OVERWRITED_VOLUME_TOKEN = "overwritedVolumeUuid";
    public static PatternedSystemTag OVERWRITED_VOLUME = new PatternedSystemTag(String.format("overwriteBy::volume::{%s}", OVERWRITED_VOLUME_TOKEN), VolumeVO.class);

    public static String NOT_SUPPORT_ACTUAL_SIZE_FLAG_TOKEN = "notSupportActualSize";
    public static PatternedSystemTag NOT_SUPPORT_ACTUAL_SIZE_FLAG = new PatternedSystemTag(String.format("notSupportActualSize::{%s}", NOT_SUPPORT_ACTUAL_SIZE_FLAG_TOKEN), VolumeVO.class);

    public static PatternedSystemTag PACKER_BUILD = new PatternedSystemTag("packer", VolumeVO.class);

    @NonCloneable
    public static EphemeralSystemTag FAST_CREATE = new EphemeralSystemTag("volume::fastCreate");

    @NonCloneable
    public static EphemeralSystemTag FLATTEN = new EphemeralSystemTag("volume::flatten");

    public static String VOLUME_QOS_TOKEN = "qos";
    public static PatternedSystemTag VOLUME_QOS = new PatternedSystemTag(String.format("%s::{%s}", VOLUME_QOS_TOKEN, VOLUME_QOS_TOKEN), VolumeVO.class);
}
