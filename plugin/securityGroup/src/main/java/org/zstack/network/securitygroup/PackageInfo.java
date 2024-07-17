package org.zstack.network.securitygroup;

import org.zstack.header.PackageAPIInfo;

import static org.zstack.header.PackageAPIInfo.*;

@PackageAPIInfo(
        APICategoryName = "安全组",
        permissions = {
                PERMISSION_COMMUNITY_AVAILABLE,
                PERMISSION_ZSV_BASIC_AVAILABLE,
        }
)
public class PackageInfo {
}
