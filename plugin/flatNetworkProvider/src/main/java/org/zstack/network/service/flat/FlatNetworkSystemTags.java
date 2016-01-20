package org.zstack.network.service.flat;

import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.PatternedSystemTag;

/**
 * Created by frank on 9/20/2015.
 */
@TagDefinition
public class FlatNetworkSystemTags {
    public static final String L3_NETWORK_DHCP_IP_TOKEN = "ip";
    public static final String L3_NETWORK_DHCP_IP_UUID_TOKEN = "ipUuid";
    public static PatternedSystemTag L3_NETWORK_DHCP_IP = new PatternedSystemTag(
            String.format("flatNetwork::DhcpServer::{%s}::ipUuid::{%s}", L3_NETWORK_DHCP_IP_TOKEN, L3_NETWORK_DHCP_IP_UUID_TOKEN),
            L3NetworkVO.class
    );
}
