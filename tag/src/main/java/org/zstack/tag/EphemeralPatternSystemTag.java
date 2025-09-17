package org.zstack.tag;

import org.zstack.header.tag.TagConstant;

public class EphemeralPatternSystemTag extends PatternedSystemTag {
    public EphemeralPatternSystemTag(String tagFormat, Class resourceClass) {
        super(String.format("%s::%s", TagConstant.EPHEMERAL_TAG_PREFIX, tagFormat), resourceClass);
    }

    public String getTagFormatWithoutEphemeralPrefix() {
        return tagFormat.substring(String.format("%s::", TagConstant.EPHEMERAL_TAG_PREFIX).length());
    }
}
