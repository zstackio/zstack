package org.zstack.sdnController;

import org.zstack.header.host.HostVO;
import org.zstack.header.network.sdncontroller.SdnControllerVO;
import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.PatternedSystemTag;
import org.zstack.tag.SystemTag;

@TagDefinition
public class SdnControllerSystemTags {
    public static String START_VNI_TOKEN = "startVni";
    public static String END_VNI_TOKEN = "endVni";
    public static PatternedSystemTag VNI_RANGE = new PatternedSystemTag(String.format("startVni::{%s}::endVni::{%s}",
            START_VNI_TOKEN, END_VNI_TOKEN), SdnControllerVO.class);

    public static String START_VLAN_TOKEN = "startVlan";
    public static String END_VLAN_TOKEN = "endVlan";
    public static PatternedSystemTag VLAN_RANGE = new PatternedSystemTag(String.format("startVlan::{%s}::endVlan::{%s}",
            START_VLAN_TOKEN, END_VLAN_TOKEN), SdnControllerVO.class);

    public static SystemTag ZNS_PROXY_PREPARED = new SystemTag("znsProxy::prepared", HostVO.class);
}
