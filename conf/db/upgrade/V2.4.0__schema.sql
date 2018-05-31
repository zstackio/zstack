SET FOREIGN_KEY_CHECKS = 0;
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
    `statement` text NOT NULL,
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
    `template` text NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`SharedResourceVO` ADD COLUMN `permission` int unsigned DEFAULT 1;
ALTER TABLE `zstack`.`SNSTopicVO` ADD COLUMN `ownerType` varchar(32) DEFAULT 'Customized';
ALTER TABLE `zstack`.`SNSApplicationEndpointVO` ADD COLUMN `ownerType` varchar(32) DEFAULT 'Customized';

CREATE TABLE `TicketFlowVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `name` VARCHAR(255) NOT NULL,
    `description` VARCHAR(2048) DEFAULT NULL,
    `collectionUuid` VARCHAR(32) NOT NULL,
    `parentFlowUuid` VARCHAR(32) DEFAULT NULL,
    `flowContext` text NOT NULL,
    `flowContextType` VARCHAR(255) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `TicketFlowCollectionVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `name` VARCHAR(255) NOT NULL,
    `description` VARCHAR(2048) DEFAULT NULL,
    `isDefault` tinyint(1) unsigned DEFAULT 0,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `TicketStatusHistoryVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `ticketUuid` VARCHAR(32) NOT NULL,
    `fromStatus` VARCHAR(255) NOT NULL,
    `toStatus` VARCHAR(255) NOT NULL,
    `comment` text DEFAULT NULL,
    `operatorUuid` VARCHAR(32) NOT NULL,
    `operatorType` VARCHAR(255) NOT NULL,
    `operationContext` text DEFAULT NULL,
    `operationContextType` VARCHAR(255) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `TicketVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `name` VARCHAR(255) DEFAULT NULL,
    `description` text DEFAULT NULL,
    `status` VARCHAR(255) NOT NULL,
    `accountSystemType` VARCHAR(255) NOT NULL,
    `accountSystemContext` text DEFAULT NULL,
    `requests` text NOT NULL,
    `flowCollectionUuid` VARCHAR(32) NOT NULL,
    `currentFlowUuid` VARCHAR(32) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ArchiveTicketVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `ticketUuid` VARCHAR(32) NOT NULL,
    `accountUuid` VARCHAR(32) NOT NULL,
    `name` VARCHAR(255) DEFAULT NULL,
    `description` text DEFAULT NULL,
    `status` VARCHAR(255) NOT NULL,
    `accountSystemType` VARCHAR(255) NOT NULL,
    `accountSystemContext` text DEFAULT NULL,
    `requests` text NOT NULL,
    `flowCollectionUuid` VARCHAR(32) NOT NULL,
    `currentFlowUuid` VARCHAR(32) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ArchiveTicketStatusHistoryVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `historyUuid` VARCHAR(32) NOT NULL,
    `accountUuid` VARCHAR(32) NOT NULL,
    `ticketUuid` VARCHAR(32) NOT NULL,
    `fromStatus` VARCHAR(255) NOT NULL,
    `toStatus` VARCHAR(255) NOT NULL,
    `comment` text DEFAULT NULL,
    `operatorUuid` VARCHAR(32) NOT NULL,
    `operatorType` VARCHAR(255) NOT NULL,
    `operationContext` text DEFAULT NULL,
    `operationContextType` VARCHAR(255) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

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

UPDATE SNSApplicationEndpointVO SET state = 'Enabled' WHERE state = '0';
UPDATE SNSApplicationEndpointVO SET state = 'Disabled' WHERE state = '1';

# Foreign keys for table RolePolicyRefVO

ALTER TABLE RolePolicyRefVO ADD CONSTRAINT fkRolePolicyRefVOPolicyVO FOREIGN KEY (policyUuid) REFERENCES PolicyVO (uuid) ON DELETE CASCADE;
ALTER TABLE RolePolicyRefVO ADD CONSTRAINT fkRolePolicyRefVORoleVO FOREIGN KEY (roleUuid) REFERENCES RoleVO (uuid) ON DELETE CASCADE;

CREATE TABLE IF NOT EXISTS `NasFileSystemVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `name` VARCHAR(255) NOT NULL,
    `protocol` VARCHAR(16) NOT NULL,
    `fileSystemId` VARCHAR(32) NOT NULL,
    `type` VARCHAR(16) NOT NULL,
    `description` VARCHAR(1024) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `AliyunNasFileSystemVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `dataCenterUuid` VARCHAR(32) NOT NULL,
    `storageType` VARCHAR(32) NOT NULL,
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkAliyunNasFileSystemVODataCenterVO` FOREIGN KEY (`dataCenterUuid`) REFERENCES `zstack`.`DataCenterVO` (`uuid`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `AliyunEbsPrimaryStorageVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `appName` varchar(64) DEFAULT NULL,
    `aZone` varchar(255) NOT NULL,
    `oceanUrl` varchar(255) NOT NULL,
    `secretKey` varchar(255) NOT NULL,
    `riverClusterId` varchar(255) NOT NULL,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `AliyunNasAccessGroupVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `name` VARCHAR(255) NOT NULL,
    `dataCenterUuid` VARCHAR(32) NOT NULL,
    `type` VARCHAR(16) NOT NULL,
    `description` VARCHAR(1024) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkAliyunNasAccessGroupVODataCenterVO` FOREIGN KEY (`dataCenterUuid`) REFERENCES `zstack`.`DataCenterVO` (`uuid`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `AliyunNasAccessRuleVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `accessGroupUuid` VARCHAR(32) NOT NULL,
    `rule` VARCHAR(16) NOT NULL,
    `priority` int(10) unsigned,
    `sourceCidr` VARCHAR(32) NOT NULL,
    `userAccess` VARCHAR(32) NOT NULL,
    `ruleId` VARCHAR(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkAliyunNasAccessRuleVOAliyunNasAccessGroupVO` FOREIGN KEY (`accessGroupUuid`) REFERENCES `zstack`.`AliyunNasAccessGroupVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `NasMountTargetVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `name` VARCHAR(255) NOT NULL,
    `nasFileSystemUuid` VARCHAR(32) NOT NULL,
    `mountDomain` VARCHAR(255) NOT NULL,
    `type` VARCHAR(16) NOT NULL,
    `description` VARCHAR(1024) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkNasMountTargetVONasFileSystemVO` FOREIGN KEY (`nasFileSystemUuid`) REFERENCES `zstack`.`NasFileSystemVO` (`uuid`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `AliyunNasMountTargetVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `accessGroupUuid` VARCHAR(32) NOT NULL,
    `status` VARCHAR(32) NOT NULL,
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkAliyunNasMountTargetVOAliyunNasAccessGroupVO` FOREIGN KEY (`accessGroupUuid`) REFERENCES `zstack`.`AliyunNasAccessGroupVO` (`uuid`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `AliyunNasPrimaryStorageFileSystemRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `primaryStorageUuid` varchar(32) NOT NULL,
    `nasFileSystemUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`id`),
    CONSTRAINT `fkPSNasRefVONasFileSystemVO` FOREIGN KEY (`nasFileSystemUuid`) REFERENCES `zstack`.`NasFileSystemVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkPSNasRefVOPrimaryStorageVO` FOREIGN KEY (`primaryStorageUuid`) REFERENCES `zstack`.`PrimaryStorageEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `AliyunNasMountVolumeRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `volumeUuid` varchar(32) DEFAULT NULL,
    `imageUuid` varchar(32) DEFAULT NULL,
    `nasMountUuid` varchar(32) NOT NULL,
    `hostUuid` varchar(32) NOT NULL,
    `sourceType` varchar(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`id`),
    CONSTRAINT `fkAliyunNasMountVolumeRefVOVolumeEO` FOREIGN KEY (`volumeUuid`) REFERENCES `zstack`.`VolumeEO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkAliyunNasMountVolumeRefVOImageEO` FOREIGN KEY (`imageUuid`) REFERENCES `zstack`.`ImageEO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkAliyunNasMountVolumeRefVOHostEO` FOREIGN KEY (`hostUuid`) REFERENCES `zstack`.`HostEO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkAliyunNasMountVolumeRefVOAliyunNasMountTargetVO` FOREIGN KEY (`nasMountUuid`) REFERENCES `zstack`.`AliyunNasMountTargetVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`PricePciDeviceOfferingRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `priceUuid` varchar(32) NOT NULL,
    `pciDeviceOfferingUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY (`id`),
    CONSTRAINT `fkPricePciDeviceOfferingRefVOPriceVO` FOREIGN KEY (`priceUuid`) REFERENCES `zstack`.`PriceVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkPricePciDeviceOfferingRefVOPciDeviceOfferingVO` FOREIGN KEY (`pciDeviceOfferingUuid`) REFERENCES `zstack`.`PciDeviceOfferingVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`PciDeviceUsageVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `pciDeviceUuid` varchar(32) NOT NULL,
    `vendorId` varchar(64) NOT NULL,
    `deviceId` varchar(64) NOT NULL,
    `subvendorId` varchar(64) DEFAULT NULL,
    `subdeviceId` varchar(64) DEFAULT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `vmUuid` varchar(32) NOT NULL,
    `vmName` varchar(255) DEFAULT NULL,
    `status` varchar(64) NOT NULL,
    `accountUuid` varchar(32) NOT NULL,
    `dateInLong` bigint unsigned NOT NULL,
    `inventory` text DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

UPDATE PciDeviceVO SET state = 'Enabled' WHERE state = '0';
UPDATE PciDeviceVO SET state = 'Disabled' WHERE state = '1';

UPDATE PciDeviceVO SET status = 'Active' WHERE status = '0';
UPDATE PciDeviceVO SET status = 'Inactive' WHERE status = '1';
UPDATE PciDeviceVO SET status = 'Attached' WHERE status = '2';
UPDATE PciDeviceVO SET status = 'System' WHERE status = '3';

UPDATE PciDeviceVO SET type = 'GPU_Video_Controller' WHERE type = '0';
UPDATE PciDeviceVO SET type = 'GPU_Audio_Controller' WHERE type = '1';
UPDATE PciDeviceVO SET type = 'GPU_3D_Controller' WHERE type = '2';
UPDATE PciDeviceVO SET type = 'Moxa_Device' WHERE type = '3';
UPDATE PciDeviceVO SET type = 'Generic' WHERE type = '4';

CREATE TABLE IF NOT EXISTS `VpcRouterDnsVO` ( 
      `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
      `vpcRouterUuid` varchar(32) NOT NULL,
      `dns` varchar(255) NOT NULL,
      `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
      `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
      PRIMARY KEY (`id`),
      CONSTRAINT fkVpcRouterDnsVOVirtualRouterVmVO FOREIGN KEY (vpcRouterUuid) REFERENCES VirtualRouterVmVO (uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`VpcRouterVmVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# create VpcRouterVmVO from ApplianceVmVO
DELIMITER $$
CREATE PROCEDURE generateVpcRouterVmVO()
    BEGIN
        DECLARE vrUuid varchar(32);
        DECLARE done INT DEFAULT FALSE;
        DECLARE cur CURSOR FOR SELECT uuid FROM zstack.ApplianceVmVO where applianceVmType = 'vpcvrouter';
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
        OPEN cur;
        read_loop: LOOP
            FETCH cur INTO vrUuid;
            IF done THEN
                LEAVE read_loop;
            END IF;

            INSERT INTO zstack.VpcRouterVmVO (uuid) values (vrUuid);
            UPDATE zstack.ResourceVO set resourceType='VpcRouterVmVO' where uuid= uuid;

        END LOOP;
        CLOSE cur;
        # work around a bug of mysql : jira.mariadb.org/browse/MDEV-4602
        SELECT CURTIME();
    END $$
DELIMITER ;

CALL generateVpcRouterVmVO();
DROP PROCEDURE IF EXISTS generateVpcRouterVmVO;
SET FOREIGN_KEY_CHECKS = 1;
