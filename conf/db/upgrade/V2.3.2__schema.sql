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
    PRIMARY KEY (`roleUuid`,`userUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `RoleVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `name` VARCHAR(255) NOT NULL,
    `type` VARCHAR(32) NOT NULL,
    `description` VARCHAR(2048) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `IAM2GroupVirtualIDRefVO` (
    `virtualIDUuid` VARCHAR(32) NOT NULL,
    `groupUuid` VARCHAR(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`virtualIDUuid`,`groupUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `IAM2OrganizationAttributeVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `name` VARCHAR(2048) NOT NULL,
    `value` VARCHAR(2048) DEFAULT NULL,
    `type` VARCHAR(32) NOT NULL,
    `organizationUuid` VARCHAR(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `IAM2OrganizationVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `name` VARCHAR(255) NOT NULL,
    `description` VARCHAR(2048) DEFAULT NULL,
    `state` VARCHAR(64) NOT NULL,
    `type` VARCHAR(64) NOT NULL,
    `parentUuid` VARCHAR(32) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `IAM2ProjectAttributeVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `name` VARCHAR(2048) NOT NULL,
    `value` VARCHAR(2048) DEFAULT NULL,
    `type` VARCHAR(32) NOT NULL,
    `projectUuid` VARCHAR(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `IAM2ProjectVirtualIDRefVO` (
    `virtualIDUuid` VARCHAR(32) NOT NULL,
    `projectUuid` VARCHAR(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`virtualIDUuid`,`projectUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE `IAM2ProjectVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `name` VARCHAR(255) NOT NULL,
    `description` VARCHAR(2048) DEFAULT NULL,
    `state` VARCHAR(64) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `IAM2VirtualIDAttributeVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `name` VARCHAR(2048) NOT NULL,
    `value` VARCHAR(2048) DEFAULT NULL,
    `type` VARCHAR(32) NOT NULL,
    `virtualIDUuid` VARCHAR(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `IAM2VirtualIDGroupAttributeVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `name` VARCHAR(2048) NOT NULL,
    `value` VARCHAR(2048) DEFAULT NULL,
    `type` VARCHAR(32) NOT NULL,
    `groupUuid` VARCHAR(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `IAM2VirtualIDGroupRefVO` (
    `virtualIDUuid` VARCHAR(32) NOT NULL,
    `groupUuid` VARCHAR(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`virtualIDUuid`,`groupUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `IAM2VirtualIDGroupRoleRefVO` (
    `roleUuid` VARCHAR(32) NOT NULL,
    `groupUuid` VARCHAR(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`roleUuid`,`groupUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `IAM2VirtualIDGroupVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `name` VARCHAR(255) NOT NULL,
    `description` VARCHAR(2048) DEFAULT NULL,
    `projectUuid` VARCHAR(32) NOT NULL,
    `state` VARCHAR(64) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `IAM2VirtualIDRoleRefVO` (
    `virtualIDUuid` VARCHAR(32) NOT NULL,
    `roleUuid` VARCHAR(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`roleUuid`,`virtualIDUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `IAM2VirtualIDVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `name` VARCHAR(255) NOT NULL,
    `description` VARCHAR(2048) DEFAULT NULL,
    `password` VARCHAR(2048) NOT NULL,
    `state` VARCHAR(64) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE PolicyVO ADD COLUMN `type` varchar(32) NOT NULL DEFAULT 'System';

CREATE TABLE `SystemRoleVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `systemRoleType` VARCHAR(64) NOT NULL,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `IAM2ProjectAccountRefVO` (
    `accountUuid` VARCHAR(32) NOT NULL,
    `projectUuid` VARCHAR(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`accountUuid`,`projectUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `IAM2VirtualIDOrganizationRefVO` (
    `virtualIDUuid` VARCHAR(32) NOT NULL,
    `organizationUuid` VARCHAR(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`virtualIDUuid`,`organizationUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;