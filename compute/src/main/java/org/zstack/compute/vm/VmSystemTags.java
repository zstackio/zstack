package org.zstack.compute.vm;

import org.zstack.header.tag.TagDefinition;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.tag.PatternedSystemTag;

/**
 */
@TagDefinition
public class VmSystemTags {
    public static String HOSTNAME_TOKEN = "hostname";
    public static PatternedSystemTag HOSTNAME = new PatternedSystemTag(String.format("hostname::{%s}", HOSTNAME_TOKEN), VmInstanceVO.class);
}
