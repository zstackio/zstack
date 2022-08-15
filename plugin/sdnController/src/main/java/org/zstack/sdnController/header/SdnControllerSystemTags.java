package org.zstack.sdnController.header;

import org.zstack.header.tag.TagDefinition;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkVO;
import org.zstack.tag.PatternedSystemTag;

@TagDefinition
public class SdnControllerSystemTags {
    public static String H3C_TENANT_UUID_TOKEN = "tenantUuid";
    public static PatternedSystemTag H3C_TENANT_UUID = new PatternedSystemTag(String.format("tenantUuid::{%s}", H3C_TENANT_UUID_TOKEN), SdnControllerVO.class);

    public static String H3C_VDS_TOKEN = "vdsUuid";
    public static PatternedSystemTag H3C_VDS_UUID = new PatternedSystemTag(String.format("vdsUuid::{%s}", H3C_VDS_TOKEN), SdnControllerVO.class);

    public static String H3C_START_VNI_TOKEN = "startVni";
    public static String H3C_END_VNI_TOKEN = "endVni";
    public static PatternedSystemTag H3C_VNI_RANGE = new PatternedSystemTag(String.format("startVni::{%s}::endVni::{%s}", H3C_START_VNI_TOKEN, H3C_END_VNI_TOKEN), SdnControllerVO.class);

    public static String H3C_L2_NETWORK_UUID_TOKEN = "h3cL2NetworkUuid";
    public static PatternedSystemTag H3C_L2_NETWORK_UUID = new PatternedSystemTag(String.format("h3cL2NetworkUuid::{%s}", H3C_L2_NETWORK_UUID_TOKEN), VxlanNetworkVO.class);
}
