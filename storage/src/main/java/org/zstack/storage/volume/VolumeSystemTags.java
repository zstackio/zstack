package org.zstack.storage.volume;

import org.zstack.header.cluster.ClusterVO;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.tag.TagDefinition;
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
}
