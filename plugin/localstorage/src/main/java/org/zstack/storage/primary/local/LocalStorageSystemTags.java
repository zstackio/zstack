package org.zstack.storage.primary.local;

import org.zstack.header.tag.TagDefinition;
import org.zstack.header.volume.VolumeVO;
import org.zstack.tag.PatternedSystemTag;

/**
 * Created by xing5 on 2016/8/22.
 */
@TagDefinition
public class LocalStorageSystemTags {
    public static final String DEST_HOST_FOR_CREATING_DATA_VOLUME_TOKEN = "hostUuid";
    public static PatternedSystemTag DEST_HOST_FOR_CREATING_DATA_VOLUME = new PatternedSystemTag(
            String.format("localStorage::hostUuid::{%s}", DEST_HOST_FOR_CREATING_DATA_VOLUME_TOKEN),
            VolumeVO.class
    );
}
