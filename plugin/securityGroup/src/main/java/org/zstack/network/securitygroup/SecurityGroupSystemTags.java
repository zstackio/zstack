package org.zstack.network.securitygroup;

import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.PatternedSystemTag;

@TagDefinition
public class SecurityGroupSystemTags {
    public static String SDN_CONTROLLER_UUID_TOKEN = "SdnControllerUuid";
    public static PatternedSystemTag SDN_CONTROLLER_UUID = new PatternedSystemTag(
            String.format("SdnControllerUuid::{%s}", SDN_CONTROLLER_UUID_TOKEN), SecurityGroupVO.class);
}
