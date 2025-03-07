package org.zstack.sdnController;

public class SdnControllerHelper {
    public static String getSdnControllerUuidFromL2Uuid(String l2Uuid) {
        return SdnControllerSystemTags.L2_NETWORK_OVN_UUID.getTokenByResourceUuid(
                l2Uuid, SdnControllerSystemTags.L2_NETWORK_OVN_UUID_TOKEN);
    }
}
