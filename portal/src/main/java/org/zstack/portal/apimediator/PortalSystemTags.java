package org.zstack.portal.apimediator;

import org.zstack.header.tag.SystemTagVO;
import org.zstack.tag.EphemeralSystemTag;
import org.zstack.tag.PatternedSystemTag;

/**
 * Created by xing5 on 2016/11/21.
 */
public class PortalSystemTags {
    public static EphemeralSystemTag VALIDATION_ONLY = new EphemeralSystemTag("validationOnly");

    public static String REPLAY_ID = "replayId";
    public static PatternedSystemTag FOR_REPLAY = new PatternedSystemTag(String.format("replay::{%s}", REPLAY_ID), SystemTagVO.class);
}
