ALTER TABLE `zstack`.`UserGroupPolicyRefVO` ADD CONSTRAINT uqUserGroupPolicyVO UNIQUE (policyUuid, groupUuid);
ALTER TABLE `zstack`.`L2NetworkClusterRefVO` ADD CONSTRAINT uqL2NetworkClusterRefVO UNIQUE (l2NetworkUuid, clusterUuid);
ALTER TABLE `zstack`.`UserPolicyRefVO` ADD CONSTRAINT uqUserPolicyRefVO UNIQUE (policyUuid, userUuid);
ALTER TABLE `zstack`.`PrimaryStorageClusterRefVO` ADD CONSTRAINT uqPrimaryStorageClusterRefVO UNIQUE (primaryStorageUuid, clusterUuid);
ALTER TABLE `zstack`.`SecurityGroupL3NetworkRefVO` ADD CONSTRAINT uqSecurityGroupL3NetworkRefVO UNIQUE (l3NetworkUuid, securityGroupUuid);
ALTER TABLE `zstack`.`NetworkServiceProviderL2NetworkRefVO` ADD CONSTRAINT uqNetworkServiceProviderL2NetworkRefVO UNIQUE (networkServiceProviderUuid, l2NetworkUuid);
