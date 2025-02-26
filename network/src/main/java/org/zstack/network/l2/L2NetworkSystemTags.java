package org.zstack.network.l2;

import org.zstack.header.network.l2.L2NetworkVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.PatternedSystemTag;

@TagDefinition
public class L2NetworkSystemTags {
    public static String L2_NETWORK_SDN_CONTROLLER_UUID_TOKEN = "SdnControllerUuid";
    public static PatternedSystemTag L2_NETWORK_SDN_CONTROLLER_UUID = new PatternedSystemTag(String.format("SdnControllerUuid::{%s}", L2_NETWORK_SDN_CONTROLLER_UUID_TOKEN), L2NetworkVO.class);
}
