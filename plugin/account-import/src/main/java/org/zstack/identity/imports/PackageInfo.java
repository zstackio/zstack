package org.zstack.identity.imports;

import org.zstack.header.PackageAPIInfo;
import org.zstack.header.rest.SDKPackage;

import static org.zstack.header.PackageAPIInfo.*;

@PackageAPIInfo(
        APICategoryName = "账号",
        permissions = {
                PERMISSION_COMMUNITY_AVAILABLE,
                PERMISSION_ZSV_BASIC_AVAILABLE,
                PERMISSION_ZSV_PRO_AVAILABLE,
        }
)
@SDKPackage(packageName = "org.zstack.sdk.identity.imports")
public class PackageInfo {
}
