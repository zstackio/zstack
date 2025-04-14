package org.zstack.network.securitygroup;

import java.util.List;

public class SecurityGroupHelper {
    public static String getSdnControllerUuid(List<String> systemTags) {
        if (systemTags == null) {
            return null;
        }
        
        for (String tag : systemTags) {
            if (SecurityGroupSystemTags.SDN_CONTROLLER_UUID.isMatch(tag)) {
                return SecurityGroupSystemTags.SDN_CONTROLLER_UUID.getTokenByTag(tag, SecurityGroupSystemTags.SDN_CONTROLLER_UUID_TOKEN);
            }
        }

        return null;
    }

    public static String getSdnControllerUuid(String sgUuid) {
        return SecurityGroupSystemTags.SDN_CONTROLLER_UUID.getTokenByResourceUuid(sgUuid, SecurityGroupSystemTags.SDN_CONTROLLER_UUID_TOKEN);
    }
}
