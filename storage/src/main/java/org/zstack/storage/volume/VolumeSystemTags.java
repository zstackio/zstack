package org.zstack.storage.volume;

import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.EphemeralSystemTag;

/**
 * Created by miao on 12/23/16.
 */
@TagDefinition
public class VolumeSystemTags {
    public static EphemeralSystemTag SHAREABLE = new EphemeralSystemTag("shareable");
}
