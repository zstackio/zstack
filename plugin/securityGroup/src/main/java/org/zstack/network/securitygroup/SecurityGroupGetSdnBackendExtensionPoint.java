package org.zstack.network.securitygroup;

public interface SecurityGroupGetSdnBackendExtensionPoint {
    SecurityGroupSdnBackend getSecurityGroupSdnBackend(String sdnControllerUuid);
}
