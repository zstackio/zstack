package org.zstack.physicalserver;

import org.zstack.header.PackageAPIInfo;

@PackageAPIInfo(
        APICategoryName = "物理服务器",
        permissions = {
                PackageAPIInfo.PERMISSION_COMMUNITY_AVAILABLE,
                PackageAPIInfo.PERMISSION_ZSV_BASIC_AVAILABLE
        }
)
public class PackageInfo {
}
