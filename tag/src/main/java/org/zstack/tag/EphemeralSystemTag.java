package org.zstack.tag;

import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.TagConstant;

/**
 * Created by xing5 on 2016/11/19.
 */
public class EphemeralSystemTag extends SystemTag {
    public EphemeralSystemTag(String tagFormat) {
        super(String.format("%s::%s", TagConstant.EPHEMERAL_TAG_PREFIX, tagFormat), SystemTagVO.class);
    }
}
