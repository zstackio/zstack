package org.zstack.network.l3;

import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.PatternedSystemTag;

@TagDefinition
public class L3NetworkSystemTags {
    // NOTE(WeiW): specify the router interface ip in guest l3 network, if admin dont want it same as network gateway
    public static String ROUTER_INTERFACE_IP_TOKEN = "routerInterfaceIp";
    public static PatternedSystemTag ROUTER_INTERFACE_IP = new PatternedSystemTag(String.format("routerInterfaceIp::{%s}", ROUTER_INTERFACE_IP_TOKEN), L3NetworkVO.class);
}
