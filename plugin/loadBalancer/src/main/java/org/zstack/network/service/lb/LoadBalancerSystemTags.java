package org.zstack.network.service.lb;

import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.PatternedSystemTag;

/**
 * Created by frank on 8/9/2015.
 */
@TagDefinition
public class LoadBalancerSystemTags {
    public static PatternedSystemTag SEPARATE_VR = new PatternedSystemTag("separateVirtualRouterVm", LoadBalancerVO.class);
}
