ALTER TABLE `zstack`.`UserGroupPolicyRefVO` ADD CONSTRAINT uqUserGroupPolicyVO UNIQUE (policyUuid, groupUuid);
ALTER TABLE `zstack`.`L2NetworkClusterRefVO` ADD CONSTRAINT uqL2NetworkClusterRefVO UNIQUE (l2NetworkUuid, clusterUuid);
ALTER TABLE `zstack`.`UserPolicyRefVO` ADD CONSTRAINT uqUserPolicyRefVO UNIQUE (policyUuid, userUuid);
