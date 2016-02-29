ALTER TABLE `zstack`.`UserGroupPolicyRefVO` ADD CONSTRAINT uqUserGroupPolicyVO UNIQUE (policyUuid, groupUuid);
