package org.zstack.ldap;

import org.zstack.header.PackageAPIInfo;
import org.zstack.header.rest.SDKPackage;

import static org.zstack.header.PackageAPIInfo.*;

@PackageAPIInfo(
    APICategoryName = "LDAP",
    permissions = {PERMISSION_COMMUNITY_AVAILABLE, PERMISSION_ZSV_ADVANCED_AVAILABLE}
)
@SDKPackage(packageName = "org.zstack.sdk.identity.ldap")
public class PackageInfo {
}
