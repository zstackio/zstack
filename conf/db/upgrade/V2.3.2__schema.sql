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
    `state` VARCHAR(64) NOT NULL,
    `description` VARCHAR(2048) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `RolePolicyStatementVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `roleUuid` VARCHAR(32) NOT NULL,
    `statement` VARCHAR(65535) NOT NULL,
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
    `name` VARCHAR(255) NOT NULL UNIQUE,
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
    `name` VARCHAR(255) NOT NULL UNIQUE,
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

CREATE TABLE `IAM2ProjectTemplateVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `name` VARCHAR(255) NOT NULL UNIQUE,
    `description` VARCHAR(2048) DEFAULT NULL,
    `template` VARCHAR(65535) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`SharedResourceVO` ADD COLUMN `permission` int unsigned DEFAULT 1;
ALTER TABLE `zstack`.`SNSTopicVO` ADD COLUMN `ownerType` varchar(32) DEFAULT 'Customized';
ALTER TABLE `zstack`.`SNSApplicationEndpointVO` ADD COLUMN `ownerType` varchar(32) DEFAULT 'Customized';

# Foreign keys for table IAM2OrganizationAttributeVO

ALTER TABLE IAM2OrganizationAttributeVO ADD CONSTRAINT fkIAM2OrganizationAttributeVOIAM2OrganizationVO FOREIGN KEY (organizationUuid) REFERENCES IAM2OrganizationVO (uuid) ;

# Foreign keys for table IAM2ProjectAttributeVO

ALTER TABLE IAM2ProjectAttributeVO ADD CONSTRAINT fkIAM2ProjectAttributeVOIAM2ProjectVO FOREIGN KEY (projectUuid) REFERENCES IAM2ProjectVO (uuid) ;

# Foreign keys for table IAM2VirtualIDAttributeVO

ALTER TABLE IAM2VirtualIDAttributeVO ADD CONSTRAINT fkIAM2VirtualIDAttributeVOIAM2VirtualIDVO FOREIGN KEY (virtualIDUuid) REFERENCES IAM2VirtualIDVO (uuid) ;

# Foreign keys for table IAM2VirtualIDGroupAttributeVO

ALTER TABLE IAM2VirtualIDGroupAttributeVO ADD CONSTRAINT fkIAM2VirtualIDGroupAttributeVOIAM2VirtualIDGroupVO FOREIGN KEY (groupUuid) REFERENCES IAM2VirtualIDGroupVO (uuid) ;

# Foreign keys for table IAM2GroupVirtualIDRefVO

ALTER TABLE IAM2GroupVirtualIDRefVO ADD CONSTRAINT fkIAM2GroupVirtualIDRefVOIAM2VirtualIDGroupVO FOREIGN KEY (groupUuid) REFERENCES IAM2VirtualIDGroupVO (uuid) ON DELETE CASCADE;
ALTER TABLE IAM2GroupVirtualIDRefVO ADD CONSTRAINT fkIAM2GroupVirtualIDRefVOIAM2VirtualIDVO FOREIGN KEY (virtualIDUuid) REFERENCES IAM2VirtualIDVO (uuid) ON DELETE CASCADE;

# Foreign keys for table IAM2ProjectAccountRefVO

ALTER TABLE IAM2ProjectAccountRefVO ADD CONSTRAINT fkIAM2ProjectAccountRefVOAccountVO FOREIGN KEY (accountUuid) REFERENCES AccountVO (uuid) ON DELETE CASCADE;
ALTER TABLE IAM2ProjectAccountRefVO ADD CONSTRAINT fkIAM2ProjectAccountRefVOIAM2ProjectVO FOREIGN KEY (projectUuid) REFERENCES IAM2ProjectVO (uuid) ON DELETE CASCADE;

# Foreign keys for table IAM2ProjectVirtualIDRefVO

ALTER TABLE IAM2ProjectVirtualIDRefVO ADD CONSTRAINT fkIAM2ProjectVirtualIDRefVOIAM2ProjectVO FOREIGN KEY (projectUuid) REFERENCES IAM2ProjectVO (uuid) ON DELETE CASCADE;
ALTER TABLE IAM2ProjectVirtualIDRefVO ADD CONSTRAINT fkIAM2ProjectVirtualIDRefVOIAM2VirtualIDVO FOREIGN KEY (virtualIDUuid) REFERENCES IAM2VirtualIDVO (uuid) ON DELETE CASCADE;

# Foreign keys for table IAM2VirtualIDGroupRefVO

ALTER TABLE IAM2VirtualIDGroupRefVO ADD CONSTRAINT fkIAM2VirtualIDGroupRefVOIAM2VirtualIDGroupVO FOREIGN KEY (groupUuid) REFERENCES IAM2VirtualIDGroupVO (uuid) ON DELETE CASCADE;
ALTER TABLE IAM2VirtualIDGroupRefVO ADD CONSTRAINT fkIAM2VirtualIDGroupRefVOIAM2VirtualIDVO FOREIGN KEY (virtualIDUuid) REFERENCES IAM2VirtualIDVO (uuid) ON DELETE CASCADE;

# Foreign keys for table IAM2VirtualIDGroupRoleRefVO

ALTER TABLE IAM2VirtualIDGroupRoleRefVO ADD CONSTRAINT fkIAM2VirtualIDGroupRoleRefVOIAM2VirtualIDGroupVO FOREIGN KEY (groupUuid) REFERENCES IAM2VirtualIDGroupVO (uuid) ON DELETE CASCADE;
ALTER TABLE IAM2VirtualIDGroupRoleRefVO ADD CONSTRAINT fkIAM2VirtualIDGroupRoleRefVORoleVO FOREIGN KEY (roleUuid) REFERENCES RoleVO (uuid) ON DELETE CASCADE;

# Foreign keys for table IAM2VirtualIDOrganizationRefVO

ALTER TABLE IAM2VirtualIDOrganizationRefVO ADD CONSTRAINT fkIAM2VirtualIDOrganizationRefVOIAM2OrganizationVO FOREIGN KEY (organizationUuid) REFERENCES IAM2OrganizationVO (uuid) ON DELETE CASCADE;
ALTER TABLE IAM2VirtualIDOrganizationRefVO ADD CONSTRAINT fkIAM2VirtualIDOrganizationRefVOIAM2VirtualIDVO FOREIGN KEY (virtualIDUuid) REFERENCES IAM2VirtualIDVO (uuid) ON DELETE CASCADE;

# Foreign keys for table IAM2VirtualIDRoleRefVO

ALTER TABLE IAM2VirtualIDRoleRefVO ADD CONSTRAINT fkIAM2VirtualIDRoleRefVOIAM2VirtualIDVO FOREIGN KEY (virtualIDUuid) REFERENCES IAM2VirtualIDVO (uuid) ON DELETE CASCADE;
ALTER TABLE IAM2VirtualIDRoleRefVO ADD CONSTRAINT fkIAM2VirtualIDRoleRefVORoleVO FOREIGN KEY (roleUuid) REFERENCES RoleVO (uuid) ON DELETE CASCADE;

ALTER TABLE SharedResourceVO ADD CONSTRAINT fkSharedResourceVOResourceVO FOREIGN KEY (resourceUuid) REFERENCES ResourceVO (uuid) ON DELETE CASCADE;

# Foreign keys for table RolePolicyRefVO

ALTER TABLE RolePolicyRefVO ADD CONSTRAINT fkRolePolicyRefVOPolicyVO FOREIGN KEY (policyUuid) REFERENCES PolicyVO (uuid) ON DELETE CASCADE;
ALTER TABLE RolePolicyRefVO ADD CONSTRAINT fkRolePolicyRefVORoleVO FOREIGN KEY (roleUuid) REFERENCES RoleVO (uuid) ON DELETE CASCADE;


SET FOREIGN_KEY_CHECKS = 0;
CREATE TABLE IF NOT EXISTS `DahoDCAccessVO` (
	  `uuid` varchar(32) UNIQUE NOT NULL,
	  `dcName` varchar(128) NOT NULL,
	  `building` varchar(64) DEFAULT NULL,
	  `dcLocation` varchar(64) NOT NULL,
	  `room` varchar(64) DEFAULT NULL,
	  `rackNo` varchar(64) DEFAULT NULL,
	  `deviceType` varchar(32) NOT NULL,
	  `portNo` varchar(32) DEFAULT NULL,
	  `deviceNo` varchar(32) DEFAULT NULL,
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`uuid`),
	  CONSTRAINT fkDahoDCAccessVODahoConnectionVO FOREIGN KEY (uuid) REFERENCES DahoConnectionVO (uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `DahoConnectionVO` (
	  `uuid` varchar(32) UNIQUE NOT NULL,
	  `connectionId` varchar(128) NOT NULL,
	  `name` varchar(255) NOT NULL,
	  `description` varchar(1024) DEFAULT NULL,
	  `bandwidthMbps` decimal(8,1) NOT NULL,
	  `status` varchar(32) NOT NULL,
	  `type` varchar(32) NOT NULL,
	  `contractEndTime` timestamp DEFAULT '0000-00-00 00:00:00',
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `DahoVllsVO` (
	  `uuid` varchar(32) UNIQUE NOT NULL,
	  `vllId` varchar(64) NOT NULL,
	  `vlanId` int(32) NOT NULL,
	  `name` varchar(32) NOT NULL,
	  `description` varchar(128) DEFAULT NULL,
	  `bandwidthMbps` bigint unsigned NOT NULL,
	  `dataCenterUuid` varchar(32) DEFAULT NULL,
	  `type` varchar(32) NOT NULL,
	  `status` varchar(32) NOT NULL,
	  `expirePolicy` varchar(32) NOT NULL,
	  `connZUuid` varchar(32) NOT NULL,
	  `connAUuid` varchar(32) NOT NULL,
	  `startDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`uuid`),
	  CONSTRAINT fkDahoVllsVODahoConnectionVO FOREIGN KEY (connZUuid) REFERENCES DahoConnectionVO (uuid) ON DELETE RESTRICT,
	  CONSTRAINT fkDahoVllsVODahoCloudConnectionVO FOREIGN KEY (connAUuid) REFERENCES DahoCloudConnectionVO (uuid) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `DahoCloudConnectionVO` (
	  `uuid` varchar(32) UNIQUE NOT NULL,
	  `name` varchar(255) NOT NULL,
	  `connectionId` varchar(128) NOT NULL,
	  `description` varchar(1024) DEFAULT NULL,
	  `bandwidth` bigint unsigned NOT NULL,
	  `usedBandwidth` bigint unsigned NOT NULL,
	  `cloud` varchar(32) NOT NULL,
	  `dataCenterUuid` varchar(32) DEFAULT NULL,
	  `accessPointId` varchar(32) NOT NULL,
	  `accessPointName` varchar(32) NOT NULL,
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`uuid`),
	  CONSTRAINT fkDahoCloudConnectionVODataCenterVO FOREIGN KEY (dataCenterUuid) REFERENCES DataCenterVO (uuid) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `DahoVllVbrRefVO` (
	  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
      `vllUuid` varchar(32) NOT NULL,
      `vbrUuid` varchar(32) NOT NULL,
      `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
      `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`id`),
	  CONSTRAINT fkDahoVllVbrRefVODahoVllsVO FOREIGN KEY (vllUuid) REFERENCES DahoVllsVO (uuid) ON DELETE CASCADE,
	  CONSTRAINT fkDahoVllVbrRefVOVirtualBorderRouterVO FOREIGN KEY (vbrUuid) REFERENCES VirtualBorderRouterVO (uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

RENAME TABLE VirtualRouterInterfaceVO to AliyunRouterInterfaceVO;
ALTER TABLE AliyunRouterInterfaceVO MODIFY COLUMN oppositeInterfaceUuid varchar(32) DEFAULT NULL;
SET FOREIGN_KEY_CHECKS = 1;
ALTER TABLE CephPrimaryStoragePoolVO ADD availableCapacity bigint(20) unsigned NOT NULL DEFAULT 0;
ALTER TABLE CephPrimaryStoragePoolVO ADD usedCapacity bigint(20) unsigned NOT NULL DEFAULT 0;
ALTER TABLE CephPrimaryStoragePoolVO ADD replicatedSize int unsigned;

ALTER TABLE CephBackupStorageVO ADD poolAvailableCapacity bigint(20) unsigned NOT NULL DEFAULT 0;
ALTER TABLE CephBackupStorageVO ADD poolUsedCapacity bigint(20) unsigned NOT NULL DEFAULT 0;
ALTER TABLE CephBackupStorageVO ADD poolReplicatedSize int unsigned;

CREATE TABLE  `zstack`.`CertificateVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) NOT NULL,
    `description` varchar(2048),
    `certificate` text NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`LoadBalancerListenerCertificateRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `listenerUuid` varchar(32) NOT NULL,
    `certificateUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY (`id`),
    CONSTRAINT `fkLoadBalancerListenerCertificateRefVOLoadBalancerListenerVO` FOREIGN KEY (`listenerUuid`) REFERENCES `zstack`.`LoadBalancerListenerVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkLoadBalancerListenerCertificateRefVOCertificateVO` FOREIGN KEY (`certificateUuid`) REFERENCES `zstack`.`CertificateVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`L3NetworkHostRouteVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `l3NetworkUuid` varchar(32) NOT NULL COMMENT 'l3 network uuid',
    `prefix` varchar(255) NOT NULL,
    `nexthop` varchar(255) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP COMMENT 'last operation date',
    `createDate` timestamp,
    PRIMARY KEY (`id`),
    CONSTRAINT `fkL3NetworkHostRouteVOL3NetworkEO` FOREIGN KEY (`l3NetworkUuid`) REFERENCES `zstack`.`L3NetworkEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

UPDATE `SchedulerJobVO` SET `jobData` = CONCAT('{"uuid":"', uuid, '",'
                    ,'"targetResourceUuid":"', targetResourceUuid, '",'
                    ,'"name":"', name,'",'
                    ,'"createDate":"', Json_getKeyValue(jobData, 'createDate'), '",'
                    ,'"accountUuid":"', Json_getKeyValue(jobData, 'accountUuid'), '"}');

ALTER TABLE `SharedBlockVO` modify diskUuid VARCHAR(255) NOT NULL;
