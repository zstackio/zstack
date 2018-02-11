UPDATE GarbageCollectorVO SET managementNodeUuid = NULL WHERE managementNodeUuid NOT IN (SELECT uuid FROM ManagementNodeVO);
ALTER TABLE GarbageCollectorVO ADD CONSTRAINT fkGarbageCollectorVOManagementNodeVO FOREIGN KEY (managementNodeUuid) REFERENCES ManagementNodeVO (uuid) ON DELETE SET NULL;

CREATE TABLE `RoleAccountRefVO` (
    `roleUuid` VARCHAR(32) NOT NULL,
    `accountUuid` VARCHAR(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`roleUuid`,`accountUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `RolePolicyRefVO` (
    `roleUuid` VARCHAR(32) NOT NULL,
    `policyUuid` VARCHAR(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`roleUuid`,`policyUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `RoleUserGroupRefVO` (
    `roleUuid` VARCHAR(32) NOT NULL,
    `groupUuid` VARCHAR(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`roleUuid`,`groupUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `RoleUserRefVO` (
    `roleUuid` VARCHAR(32) NOT NULL,
    `userUuid` VARCHAR(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`roleUuid`,`groupUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `RoleVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `name` VARCHAR(255) NOT NULL,
    `description` VARCHAR(2048) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`roleUuid`,`groupUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;