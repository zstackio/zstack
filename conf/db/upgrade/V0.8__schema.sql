CREATE TABLE  `zstack`.`LocalStorageHostRefVO` (
    `hostUuid` varchar(32) NOT NULL UNIQUE,
    `primaryStorageUuid` varchar(32) NOT NULL,
    `totalCapacity` bigint unsigned DEFAULT 0,
    `availableCapacity` bigint unsigned DEFAULT 0,
    `totalPhysicalCapacity` bigint unsigned DEFAULT 0,
    `availablePhysicalCapacity` bigint unsigned DEFAULT 0,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`hostUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`LocalStorageResourceRefVO` (
    `resourceUuid` varchar(32) NOT NULL UNIQUE,
    `primaryStorageUuid` varchar(32) NOT NULL,
    `hostUuid` varchar(32),
    `size` bigint unsigned DEFAULT 0,
    `resourceType` varchar(255),
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`resourceUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`QuotaVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `name` varchar(255) NOT NULL,
    `identityUuid` varchar(32) DEFAULT NULL,
    `identityType` varchar(255) NOT NULL,
    `value` bigint unsigned DEFAULT 0,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`SharedResourceVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `ownerAccountUuid` varchar(32) NOT NULL,
    `receiverAccountUuid` varchar(32) DEFAULT NULL,
    `toPublic` tinyint(1) unsigned NOT NULL DEFAULT 0,
    `resourceType` varchar(256) NOT NULL,
    `resourceUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE AccountVO ADD description VARCHAR(2048) DEFAULT NULL;
ALTER TABLE UserVO ADD description VARCHAR(2048) DEFAULT NULL;

# Foreign keys for table SharedResourceVO

ALTER TABLE SharedResourceVO ADD CONSTRAINT fkSharedResourceVOAccountVO FOREIGN KEY (ownerAccountUuid) REFERENCES AccountVO (uuid) ON DELETE CASCADE;
ALTER TABLE SharedResourceVO ADD CONSTRAINT fkSharedResourceVOAccountVO1 FOREIGN KEY (receiverAccountUuid) REFERENCES AccountVO (uuid) ON DELETE CASCADE;

# Foreign keys for table LocalStorageHostRefVO

ALTER TABLE LocalStorageHostRefVO ADD CONSTRAINT fkLocalStorageHostRefVOHostEO FOREIGN KEY (hostUuid) REFERENCES HostEO (uuid) ON DELETE CASCADE;
ALTER TABLE LocalStorageHostRefVO ADD CONSTRAINT fkLocalStorageHostRefVOPrimaryStorageEO FOREIGN KEY (primaryStorageUuid) REFERENCES PrimaryStorageEO (uuid) ON DELETE CASCADE;

# Foreign keys for table LocalStorageResourceRefVO

ALTER TABLE LocalStorageResourceRefVO ADD CONSTRAINT fkLocalStorageResourceRefVOHostEO FOREIGN KEY (hostUuid) REFERENCES HostEO (uuid) ON DELETE CASCADE;
ALTER TABLE LocalStorageResourceRefVO ADD CONSTRAINT fkLocalStorageResourceRefVOPrimaryStorageEO FOREIGN KEY (primaryStorageUuid) REFERENCES PrimaryStorageEO (uuid) ON DELETE CASCADE;

# Drop all old foreign keys for identity VOs; foreign keys are all auto-generated now

ALTER TABLE AccountResourceRefVO  DROP FOREIGN KEY fkAccountResourceAcntUuid;
ALTER TABLE AccountResourceRefVO  DROP FOREIGN KEY fkAccountResourceOwnerUuid;
ALTER TABLE UserVO  DROP FOREIGN KEY fkUserAccountUuid;
ALTER TABLE SessionVO  DROP FOREIGN KEY fkSessionAccountUuid;
ALTER TABLE PolicyVO  DROP FOREIGN KEY fkPolicyAccountUuid;
ALTER TABLE UserPolicyRefVO  DROP FOREIGN KEY fkUpPolicyUuid;
ALTER TABLE UserPolicyRefVO  DROP FOREIGN KEY fkUpUserUuid;
ALTER TABLE UserGroupVO  DROP FOREIGN KEY fkGroupAccountUuid;
ALTER TABLE UserGroupPolicyRefVO  DROP FOREIGN KEY fkGpPolicyUuid;
ALTER TABLE UserGroupPolicyRefVO  DROP FOREIGN KEY fkGpGroupUuid;
ALTER TABLE UserGroupUserRefVO  DROP FOREIGN KEY fkUgPolicyUuid;
ALTER TABLE UserGroupUserRefVO  DROP FOREIGN KEY fkUgGroupUuid;

# Foreign keys for table SessionVO

ALTER TABLE SessionVO ADD CONSTRAINT fkSessionVOAccountVO FOREIGN KEY (accountUuid) REFERENCES AccountVO (uuid) ON DELETE CASCADE;

# Foreign keys for table AccountResourceRefVO

ALTER TABLE AccountResourceRefVO ADD CONSTRAINT fkAccountResourceRefVOAccountVO FOREIGN KEY (accountUuid) REFERENCES AccountVO (uuid) ON DELETE CASCADE;
ALTER TABLE AccountResourceRefVO ADD CONSTRAINT fkAccountResourceRefVOAccountVO1 FOREIGN KEY (ownerAccountUuid) REFERENCES AccountVO (uuid) ON DELETE CASCADE;

# Foreign keys for table PolicyVO

ALTER TABLE PolicyVO ADD CONSTRAINT fkPolicyVOAccountVO FOREIGN KEY (accountUuid) REFERENCES AccountVO (uuid) ON DELETE CASCADE;

# Foreign keys for table UserGroupVO

ALTER TABLE UserGroupVO ADD CONSTRAINT fkUserGroupVOAccountVO FOREIGN KEY (accountUuid) REFERENCES AccountVO (uuid) ON DELETE CASCADE;

# Foreign keys for table UserVO

ALTER TABLE UserVO ADD CONSTRAINT fkUserVOAccountVO FOREIGN KEY (accountUuid) REFERENCES AccountVO (uuid) ON DELETE CASCADE;

# Foreign keys for table UserGroupPolicyRefVO

ALTER TABLE UserGroupPolicyRefVO ADD CONSTRAINT fkUserGroupPolicyRefVOPolicyVO FOREIGN KEY (policyUuid) REFERENCES PolicyVO (uuid) ON DELETE CASCADE;
ALTER TABLE UserGroupPolicyRefVO ADD CONSTRAINT fkUserGroupPolicyRefVOUserGroupVO FOREIGN KEY (groupUuid) REFERENCES UserGroupVO (uuid) ON DELETE CASCADE;

# Foreign keys for table UserGroupUserRefVO

ALTER TABLE UserGroupUserRefVO ADD CONSTRAINT fkUserGroupUserRefVOUserGroupVO FOREIGN KEY (groupUuid) REFERENCES UserGroupVO (uuid) ON DELETE CASCADE;
ALTER TABLE UserGroupUserRefVO ADD CONSTRAINT fkUserGroupUserRefVOUserVO FOREIGN KEY (userUuid) REFERENCES UserVO (uuid) ON DELETE CASCADE;

# Foreign keys for table UserPolicyRefVO

ALTER TABLE UserPolicyRefVO ADD CONSTRAINT fkUserPolicyRefVOPolicyVO FOREIGN KEY (policyUuid) REFERENCES PolicyVO (uuid) ON DELETE CASCADE;
ALTER TABLE UserPolicyRefVO ADD CONSTRAINT fkUserPolicyRefVOUserVO FOREIGN KEY (userUuid) REFERENCES UserVO (uuid) ON DELETE CASCADE;

# Index for table LocalStorageHostRefVO

CREATE INDEX idxLocalStorageHostRefVOtotalCapacity ON LocalStorageHostRefVO (totalCapacity);
CREATE INDEX idxLocalStorageHostRefVOavailableCapacity ON LocalStorageHostRefVO (availableCapacity);
CREATE INDEX idxLocalStorageHostRefVOtotalPhysicalCapacity ON LocalStorageHostRefVO (totalPhysicalCapacity);
CREATE INDEX idxLocalStorageHostRefVOavailablePhysicalCapacity ON LocalStorageHostRefVO (availablePhysicalCapacity);

ALTER TABLE PolicyVO DROP COLUMN type;
