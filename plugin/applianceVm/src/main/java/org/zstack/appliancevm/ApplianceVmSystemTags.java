package org.zstack.appliancevm;

import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.PatternedSystemTag;

/**
 * Created by shixin on 2019/06/11
 */
@TagDefinition
public class ApplianceVmSystemTags {
    public static String APPLIANCEVM_HA_UUID_TOKEN = "haUuid";
    public static PatternedSystemTag APPLIANCEVM_HA_UUID =
            new PatternedSystemTag(String.format(
                    "haUuid::{%s}", APPLIANCEVM_HA_UUID_TOKEN),
                    ApplianceVmVO.class);

}
