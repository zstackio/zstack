package org.zstack.network.service.vip;

import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.EphemeralSystemTag;

/**
 * Created by xing5 on 2016/11/19.
 */
@TagDefinition
public class VipSystemTags {
    public static EphemeralSystemTag DELETE_ON_FAILURE = new EphemeralSystemTag("deleteVipOnFailure");
}
