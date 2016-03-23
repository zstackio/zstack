ALTER TABLE `zstack`.`UserGroupPolicyRefVO` ADD CONSTRAINT uqUserGroupPolicyVO UNIQUE (policyUuid, groupUuid);
ALTER TABLE `zstack`.`L2NetworkClusterRefVO` ADD CONSTRAINT uqL2NetworkClusterRefVO UNIQUE (l2NetworkUuid, clusterUuid);
ALTER TABLE `zstack`.`UserPolicyRefVO` ADD CONSTRAINT uqUserPolicyRefVO UNIQUE (policyUuid, userUuid);
ALTER TABLE `zstack`.`PrimaryStorageClusterRefVO` ADD CONSTRAINT uqPrimaryStorageClusterRefVO UNIQUE (primaryStorageUuid, clusterUuid);
ALTER TABLE `zstack`.`SecurityGroupL3NetworkRefVO` ADD CONSTRAINT uqSecurityGroupL3NetworkRefVO UNIQUE (l3NetworkUuid, securityGroupUuid);
ALTER TABLE `zstack`.`NetworkServiceProviderL2NetworkRefVO` ADD CONSTRAINT uqNetworkServiceProviderL2NetworkRefVO UNIQUE (networkServiceProviderUuid, l2NetworkUuid);

CREATE TABLE  `zstack`.`ConsoleProxyAgentVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `description` varchar(1024) DEFAULT NULL,
    `managementIp` varchar(255) NOT NULL,
    `type` varchar(255) NOT NULL,
    `status` varchar(64) NOT NULL,
    `state` varchar(64) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# Foreign keys for table ConsoleProxyAgentVO

ALTER TABLE ConsoleProxyAgentVO ADD CONSTRAINT fkConsoleProxyAgentVOManagementNodeVO FOREIGN KEY (uuid) REFERENCES ManagementNodeVO (uuid) ON DELETE CASCADE;

ALTER TABLE GarbageCollectorVO ADD type varchar(1024) NOT NULL;
UPDATE GarbageCollectorVO SET type = "org.zstack.core.gc.TimeBasedGCPersistentContext";
