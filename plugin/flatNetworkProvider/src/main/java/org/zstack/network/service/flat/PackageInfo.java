package org.zstack.network.service.flat;

import org.zstack.header.PackageAPIInfo;

import static org.zstack.header.PackageAPIInfo.*;

@PackageAPIInfo(
        APICategoryName = "扁平网络",
        permissions = {
                PERMISSION_COMMUNITY_AVAILABLE,
                PERMISSION_ZSV_BASIC_AVAILABLE,
                PERMISSION_ZSV_PRO_AVAILABLE,
        }
)
public class PackageInfo {
}
