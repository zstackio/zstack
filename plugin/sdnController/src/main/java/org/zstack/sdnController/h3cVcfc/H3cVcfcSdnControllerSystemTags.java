package org.zstack.sdnController.h3cVcfc;

import org.zstack.header.tag.TagDefinition;
import org.zstack.network.l2.vxlan.vxlanNetwork.VxlanNetworkVO;
import org.zstack.header.network.sdncontroller.SdnControllerVO;
import org.zstack.tag.PatternedSystemTag;

@TagDefinition
public class H3cVcfcSdnControllerSystemTags {
    public static String H3C_TENANT_UUID_TOKEN = "tenantUuid";
    public static PatternedSystemTag H3C_TENANT_UUID = new PatternedSystemTag(String.format("tenantUuid::{%s}", H3C_TENANT_UUID_TOKEN), SdnControllerVO.class);

    public static String H3C_VDS_TOKEN = "vdsUuid";
    public static PatternedSystemTag H3C_VDS_UUID = new PatternedSystemTag(String.format("vdsUuid::{%s}", H3C_VDS_TOKEN), SdnControllerVO.class);

    public static String H3C_L2_NETWORK_UUID_TOKEN = "h3cL2NetworkUuid";
    public static PatternedSystemTag H3C_L2_NETWORK_UUID = new PatternedSystemTag(String.format("h3cL2NetworkUuid::{%s}", H3C_L2_NETWORK_UUID_TOKEN), VxlanNetworkVO.class);

    public static String H3C_L2_VROUTER_UUID_TOKEN = "h3cL2RouterUuid";
    public static PatternedSystemTag H3C_L2_VROUTER_UUID = new PatternedSystemTag(String.format("h3cL2RouterUuid::{%s}", H3C_L2_VROUTER_UUID_TOKEN), VxlanNetworkVO.class);

    public static String H3C_L2_TENANT_UUID_TOKEN = "h3cL2TenantUuid";
    public static PatternedSystemTag H3C_L2_TENANT_UUID = new PatternedSystemTag(String.format("h3cL2TenantUuid::{%s}", H3C_L2_TENANT_UUID_TOKEN), VxlanNetworkVO.class);
}
