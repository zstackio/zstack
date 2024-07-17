package org.zstack.header.tag;

import org.zstack.header.PackageAPIInfo;

import static org.zstack.header.PackageAPIInfo.*;

@PackageAPIInfo(
        APICategoryName = "标签",
        permissions = {
                PERMISSION_COMMUNITY_AVAILABLE,
                PERMISSION_ZSV_BASIC_AVAILABLE,
                PERMISSION_ZSV_PRO_AVAILABLE,
        }
)
public class PackageInfo {
}
