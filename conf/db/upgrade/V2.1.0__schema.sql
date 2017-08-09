ALTER TABLE `EcsSecurityGroupRuleVO` DROP COLUMN `sourceGroupId`;

CREATE TABLE  `zstack`.`PrimaryStorageHostRefVO` (
     `primaryStorageUuid` varchar(32) NOT NULL,
     `hostUuid` varchar(32) NOT NULL,
     `status` varchar(32) NOT NULL,
     `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
     `createDate` timestamp,
      CONSTRAINT `fkPrimaryStorageHostRefVOPrimaryStorageEO` FOREIGN KEY (`primaryStorageUuid`) REFERENCES `zstack`.`PrimaryStorageEO` (`uuid`) ON DELETE CASCADE,
      CONSTRAINT `fkPrimaryStorageHostRefVOHostEO` FOREIGN KEY (`hostUuid`) REFERENCES `zstack`.`HostEO` (`uuid`) ON DELETE CASCADE,
      PRIMARY KEY (`primaryStorageUuid`, `hostUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 0;
CREATE TABLE `VpcUserVpnGatewayVO` (
	  `uuid` varchar(32) UNIQUE NOT NULL,
	  `accountName` varchar(128) NOT NULL,
	  `gatewayId` varchar(32) NOT NULL,
	  `dataCenterUuid` varchar(32) NOT NULL,
	  `type` varchar(32) NOT NULL,
	  `name` varchar(128) NOT NULL,
	  `ip` varchar(32) NOT NULL,
	  `description` varchar(1024) DEFAULT NULL,
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`uuid`),
	  UNIQUE KEY `ukVpcUserVpnGatewayVO` (`dataCenterUuid`,`accountName`,`gatewayId`) USING BTREE,
	  CONSTRAINT fkVpcUserVpnGatewayVODataCenterVO FOREIGN KEY (dataCenterUuid) REFERENCES DataCenterVO (uuid) ON DELETE RESTRICT,
	  CONSTRAINT fkVpcUserVpnGatewayVOAccountVO FOREIGN KEY (accountName) REFERENCES AccountVO (name) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VpcVpnConnectionVO` (
	  `uuid` varchar(32) UNIQUE NOT NULL,
	  `accountName` varchar(128) NOT NULL,
	  `connectionId` varchar(32) NOT NULL,
	  `userGatewayUuid` varchar(32) NOT NULL,
	  `vpnGatewayUuid` varchar(32) NOT NULL,
	  `type` varchar(32) NOT NULL,
	  `name` varchar(128) NOT NULL,
	  `localSubnet` varchar(64) NOT NULL,
	  `remoteSubnet` varchar(64) NOT NULL,
	  `ikeConfigUuid` varchar(32) NOT NULL,
	  `ipsecConfigUuid` varchar(32) NOT NULL,
	  `description` varchar(1024) DEFAULT NULL,
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`uuid`),
	  UNIQUE KEY `ukVpcVpnConnectionVO` (`connectionId`,`accountName`,`userGatewayUuid`) USING BTREE,
	  CONSTRAINT fkVpcVpnConnectionVOVpcUserVpnGatewayVO FOREIGN KEY (userGatewayUuid) REFERENCES VpcUserVpnGatewayVO (uuid) ON DELETE RESTRICT,
	  CONSTRAINT fkVpcVpnConnectionVOVpcVpnGatewayVO FOREIGN KEY (vpnGatewayUuid) REFERENCES VpcVpnGatewayVO (uuid) ON DELETE RESTRICT,
	  CONSTRAINT fkVpcVpnConnectionVOVpcVpnIkeConfigVO FOREIGN KEY (ikeConfigUuid) REFERENCES VpcVpnIkeConfigVO (uuid) ON DELETE RESTRICT,
	  CONSTRAINT fkVpcVpnConnectionVOVpcVpnIpSecConfigVO FOREIGN KEY (ipsecConfigUuid) REFERENCES VpcVpnIpSecConfigVO (uuid) ON DELETE RESTRICT,
	  CONSTRAINT fkVpcVpnConnectionVOAccountVO FOREIGN KEY (accountName) REFERENCES AccountVO (name) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VpcVpnGatewayVO` (
	  `uuid` varchar(32) UNIQUE NOT NULL,
	  `accountName` varchar(128) NOT NULL,
	  `gatewayId` varchar(32) NOT NULL,
	  `vSwitchUuid` varchar(32) NOT NULL,
	  `type` varchar(32) NOT NULL,
	  `name` varchar(128) NOT NULL,
	  `publicIp` varchar(32) NOT NULL,
	  `spec` varchar(32) NOT NULL,
	  `status` varchar(32) NOT NULL,
	  `businessStatus` varchar(32) NOT NULL,
	  `description` varchar(1024) DEFAULT NULL,
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `endDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`uuid`),
	  UNIQUE KEY `ukVpcVpnGatewayVO` (`vSwitchUuid`,`accountName`,`gatewayId`) USING BTREE,
	  CONSTRAINT fkVpcVpnGatewayVOEcsVSwitchVO FOREIGN KEY (vSwitchUuid) REFERENCES EcsVSwitchVO (uuid) ON DELETE RESTRICT,
	  CONSTRAINT fkVpcVpnGatewayVOAccountVO FOREIGN KEY (accountName) REFERENCES AccountVO (name) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VpcVpnIkeConfigVO` (
	  `uuid` varchar(32) UNIQUE NOT NULL,
	  `accountName` varchar(128) NOT NULL,
	  `name` varchar(128) NOT NULL,
	  `psk` varchar(100) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
	  `version` varchar(32) NOT NULL,
	  `mode` varchar(32) NOT NULL,
	  `encodeAlgorithm` varchar(32) NOT NULL,
	  `authAlgorithm` varchar(32) NOT NULL,
	  `pfs` varchar(32) NOT NULL,
	  `lifetime` bigint unsigned NOT NULL,
	  `localIp` varchar(32) NOT NULL,
	  `remoteIp` varchar(32) NOT NULL,
	  `description` varchar(1024) DEFAULT NULL,
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`uuid`),
	  CONSTRAINT fkVpcVpnIkeConfigVOAccountVO FOREIGN KEY (accountName) REFERENCES AccountVO (name) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VpcVpnIpSecConfigVO` (
	  `uuid` varchar(32) UNIQUE NOT NULL,
	  `accountName` varchar(128) NOT NULL,
	  `name` varchar(128) NOT NULL,
	  `encodeAlgorithm` varchar(32) NOT NULL,
	  `authAlgorithm` varchar(32) NOT NULL,
	  `pfs` varchar(32) NOT NULL,
	  `lifetime` bigint unsigned NOT NULL,
	  `description` varchar(1024) DEFAULT NULL,
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`uuid`),
	  CONSTRAINT fkVpcVpnIpSecConfigVOAccountVO FOREIGN KEY (accountName) REFERENCES AccountVO (name) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO ResourceVO (uuid, resourceType) SELECT t.uuid, "VpcUserVpnGatewayVO" FROM VpcUserVpnGatewayVO t;
INSERT INTO ResourceVO (uuid, resourceType) SELECT t.uuid, "VpcVpnConnectionVO" FROM VpcVpnConnectionVO t;
INSERT INTO ResourceVO (uuid, resourceType) SELECT t.uuid, "VpcVpnGatewayVO" FROM VpcVpnGatewayVO t;
INSERT INTO ResourceVO (uuid, resourceType) SELECT t.uuid, "VpcVpnIkeConfigVO" FROM VpcVpnIkeConfigVO t;
INSERT INTO ResourceVO (uuid, resourceType) SELECT t.uuid, "VpcVpnIpSecConfigVO" FROM VpcVpnIpSecConfigVO t;



-- ----------------------------
--  Table structure for `SnapshotUsageVO`
-- ----------------------------
CREATE TABLE `SnapShotUsageVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `volumeUuid` varchar(32) NOT NULL,
  `SnapshotUuid` varchar(32) NOT NULL,
  `SnapshotStatus` varchar(64) NOT NULL,
  `SnapshotName` varchar(255) NOT NULL,
  `accountUuid` varchar(32) NOT NULL,
  `SnapshotSize` bigint unsigned NOT NULL,
  `dateInLong` bigint unsigned NOT NULL,
  `inventory` text,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS EcsInstanceConsoleProxyVO;
-- ----------------------------
--  Table structure for `SchedulerJobVO`
-- ----------------------------
CREATE TABLE  `zstack`.`SchedulerJobVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `targetResourceUuid` varchar(32) NOT NULL,
    `name` varchar(255) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `jobClassName` varchar(255),
    `jobData` text,
    `state` varchar(255),
    `managementNodeUuid` varchar(32) DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkSchedulerJobVOManagementNodeVO` FOREIGN KEY (`managementNodeUuid`) REFERENCES `ManagementNodeVO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
--  Table structure for `SchedulerTriggerVO`
-- ----------------------------
CREATE TABLE  `zstack`.`SchedulerTriggerVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `schedulerType` varchar(255) NOT NULL,
    `schedulerInterval` int unsigned DEFAULT NULL,
    `repeatCount` int unsigned DEFAULT NULL,
    `managementNodeUuid` varchar(32) DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `startTime` timestamp NULL DEFAULT 0,
    `stopTime` timestamp NULL DEFAULT 0,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkSchedulerTriggerVOManagementNodeVO` FOREIGN KEY (`managementNodeUuid`) REFERENCES `ManagementNodeVO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
--  Table structure for `SchedulerJobSchedulerTriggerRefVO`
-- ----------------------------
CREATE TABLE  `zstack`.`SchedulerJobSchedulerTriggerRefVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `schedulerJobUuid` varchar(32) NOT NULL,
    `schedulerTriggerUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkSchedulerJobSchedulerTriggerRefVOSchedulerJobVO` FOREIGN KEY (`schedulerJobUuid`) REFERENCES `SchedulerJobVO` (`uuid`),
    CONSTRAINT `fkSchedulerJobSchedulerTriggerRefVOSchedulerTriggerVO` FOREIGN KEY (`schedulerTriggerUuid`) REFERENCES `SchedulerTriggerVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE `VRouterRouteTableVO` (
  `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'uuid',
  `type` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VRouterRouteEntryVO` (
  `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'uuid',
  `description` varchar(2048) DEFAULT NULL,
  `routeTableUuid` varchar(32) NOT NULL,
  `destination` varchar(64) NOT NULL,
  `target` varchar(64) DEFAULT NULL,
  `type` varchar(32) NOT NULL,
  `distance` int unsigned NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  CONSTRAINT fkRouteEntryVOVRouterRouteTableVO FOREIGN KEY (`routeTableUuid`) REFERENCES `zstack`.`VRouterRouteTableVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VirtualRouterVRouterRouteTableRefVO` (
  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
  `virtualRouterVmUuid` varchar(32) NOT NULL UNIQUE COMMENT 'uuid',
  `routeTableUuid` varchar(32) NOT NULL,
  CONSTRAINT `VirutalRouterVRouterRouteTableRefVOVRouterRouteTableVO` FOREIGN KEY (`routeTableUuid`) REFERENCES `zstack`.`VRouterRouteTableVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `VirutalRouterVRouterRouteTableRefVOVirtualRouterVmVO` FOREIGN KEY (`virtualRouterVmUuid`) REFERENCES `zstack`.`VirtualRouterVmVO` (`uuid`) ON DELETE CASCADE,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `ConsoleProxyAgentVO` ADD `consoleProxyOverriddenIp` varchar(255) NOT NULL;

CREATE TABLE `OssUploadPartsVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `uploadId` varchar(32) NOT NULL,
  `ossBucketUuid` varchar(32) NOT NULL,
  `fileKey` varchar(128) NOT NULL,
  `partNumber` int(16) NOT NULL,
  `total` int(16) NOT NULL,
  `eTag` varchar(32) NOT NULL,
  `partSize` bigint(32) NOT NULL,
  `partCRC` bigint(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  CONSTRAINT fkOssUploadPartsVOOssBucketVO FOREIGN KEY (ossBucketUuid) REFERENCES OssBucketVO (uuid) ON DELETE CASCADE,
  PRIMARY KEY (`id`),
  KEY `uploadId` (`uploadId`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
--  Add name and description for Baremetal Resources
-- ----------------------------
ALTER TABLE `BaremetalPxeServerVO` ADD COLUMN `name` varchar(255) DEFAULT NULL COMMENT 'baremetal pxeserver name';
ALTER TABLE `BaremetalPxeServerVO` ADD COLUMN `description` varchar(2048) DEFAULT NULL COMMENT 'baremetal pxeserver description';
ALTER TABLE `BaremetalChassisVO` ADD COLUMN `name` varchar(255) DEFAULT NULL COMMENT 'baremetal chassis name';
ALTER TABLE `BaremetalChassisVO` ADD COLUMN `description` varchar(2048) DEFAULT NULL COMMENT 'baremetal chassis description';
ALTER TABLE `BaremetalChassisVO` ADD COLUMN `ipmiPort` varchar(32) DEFAULT NULL COMMENT 'baremetal chassis ipmi port';
ALTER TABLE `BaremetalChassisVO` DROP INDEX `ipmiAddress`;
ALTER TABLE `BaremetalChassisVO` ADD CONSTRAINT ukBaremetalChassisVO UNIQUE (`ipmiAddress`, `ipmiPort`);
ALTER TABLE `BaremetalChassisVO` ADD COLUMN `status` varchar(32) DEFAULT NULL COMMENT 'baremetal chassis status';
UPDATE `BaremetalChassisVO` SET `status` = "Unprovisioned" WHERE `provisioned` = 0;
UPDATE `BaremetalChassisVO` SET `status` = "Provisioned" WHERE `provisioned` = 1;
ALTER TABLE `BaremetalChassisVO` DROP COLUMN `provisioned`;

CREATE TABLE `ConnectionRelationShipVO` (
  `uuid` varchar(32) NOT NULL UNIQUE,
  `name` varchar(128) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `relationShips` text NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `HybridConnectionRefVO` (
  `uuid` varchar(32) NOT NULL UNIQUE,
  `resourceUuid` varchar(32) NOT NULL,
  `resourceType` varchar(32) NOT NULL,
  `accountUuid` varchar(32) NOT NULL,
  `direction` varchar(16) NOT NULL,
  `connectionType` varchar(32) NOT NULL,
  `connectionUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  CONSTRAINT fkHybridConnectionRefVOConnectionRelationShipVO FOREIGN KEY (connectionUuid) REFERENCES ConnectionRelationShipVO (uuid) ON DELETE CASCADE,
  PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS AvailableIdentityZonesVO;
ALTER TABLE EcsVpcVO CHANGE COLUMN `vpcName` `name` varchar(128) NOT NULL;
ALTER TABLE EcsVSwitchVO CHANGE COLUMN `vSwitchName` `name` varchar(128) NOT NULL;
ALTER TABLE EcsSecurityGroupVO CHANGE COLUMN `securityGroupName` `name` varchar(128) NOT NULL;
ALTER TABLE VpcVirtualRouterVO CHANGE COLUMN `vRouterName` `name` varchar(128) NOT NULL;

CREATE TABLE `MonitorTriggerVO` (
  `uuid` varchar(32) NOT NULL UNIQUE,
  `name` varchar(255) NOT NULL,
  `expression` varchar(2048) NOT NULL,
  `recoveryExpression` varchar(2048) DEFAULT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `contextData` text DEFAULT NULL,
  `duration` int unsigned NOT NULL,
  `status` varchar(64) NOT NULL,
  `state` varchar(64) NOT NULL,
  `targetResourceUuid` varchar(32) NOT NULL,
  `lastStatusChangeTime` timestamp DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp,
  PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `MonitorTriggerActionVO` (
  `uuid` varchar(32) NOT NULL UNIQUE,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `type` varchar(64) NOT NULL,
  `state` varchar(64) NOT NULL,
  `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp,
  PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `EmailTriggerActionVO` (
  `uuid` varchar(32) NOT NULL UNIQUE,
  `email` varchar(512) NOT NULL,
  `mediaUuid` varchar(32) NOT NULL,
  CONSTRAINT fkEmailTriggerActionVOMonitorTriggerActionVO FOREIGN KEY (uuid) REFERENCES MonitorTriggerActionVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE,
  PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `MediaVO` (
  `uuid` varchar(32) NOT NULL UNIQUE,
  `name` varchar(255) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `type` varchar(64) NOT NULL,
  `state` varchar(64) NOT NULL,
  `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp,
  PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `EmailMediaVO` (
  `uuid` varchar(32) NOT NULL UNIQUE,
  `smtpServer` varchar(512) NOT NULL,
  `smtpPort` int unsigned NOT NULL,
  `username` varchar(512) DEFAULT NULL,
  `password` varchar(512) DEFAULT NULL,
  CONSTRAINT fkEmailMediaVOMediaVO FOREIGN KEY (uuid) REFERENCES MediaVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE,
  PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `MonitorTriggerActionRefVO` (
  `actionUuid` varchar(32) NOT NULL,
  `triggerUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp,
  CONSTRAINT fkMonitorTriggerActionRefVOMonitorTriggerActionVO FOREIGN KEY (actionUuid) REFERENCES MonitorTriggerActionVO (uuid) ON DELETE CASCADE,
  CONSTRAINT fkMonitorTriggerActionRefVOMonitorTriggerVO FOREIGN KEY (triggerUuid) REFERENCES MonitorTriggerVO (uuid) ON DELETE CASCADE,
  PRIMARY KEY  (`actionUuid`, `triggerUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE IF EXISTS AlertLabelVO;
DROP TABLE IF EXISTS AlertTimestampVO;
DROP TABLE IF EXISTS AlertVO;
CREATE TABLE `AlertVO` (
  `uuid` varchar(32) NOT NULL UNIQUE,
  `targetResourceUuid` varchar(32) NOT NULL,
  `triggerUuid` varchar(32) NOT NULL,
  `triggerStatus` varchar(64) NOT NULL,
  `content` text DEFAULT NULL,
  `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp,
  PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 0;
ALTER TABLE EcsInstanceVO DROP FOREIGN KEY fkEcsInstanceVOEcsVpcVO;
ALTER TABLE EcsInstanceVO DROP KEY fkEcsInstanceVOEcsVpcVO;
ALTER TABLE EcsInstanceVO DROP COLUMN ecsVpcUuid;
ALTER TABLE EcsInstanceVO DROP COLUMN ecsEipUuid;

ALTER TABLE EcsInstanceVO MODIFY COLUMN ecsVSwitchUuid varchar(32) NOT NULL;
ALTER TABLE EcsInstanceVO MODIFY COLUMN ecsSecurityGroupUuid varchar(32) NOT NULL;

ALTER TABLE EcsInstanceVO DROP FOREIGN KEY fkEcsInstanceVOEcsVSwitchVO;
ALTER TABLE EcsInstanceVO DROP FOREIGN KEY fkEcsInstanceVOIdentityZoneVO;
ALTER TABLE EcsInstanceVO DROP FOREIGN KEY fkEcsInstanceVOEcsSecurityGroupVO;

ALTER TABLE EcsInstanceVO ADD CONSTRAINT fkEcsInstanceVOEcsVSwitchVO FOREIGN KEY (ecsVSwitchUuid) REFERENCES EcsVSwitchVO (uuid) ON DELETE RESTRICT;
ALTER TABLE EcsInstanceVO ADD CONSTRAINT fkEcsInstanceVOEcsSecurityGroupVO FOREIGN KEY (ecsSecurityGroupUuid) REFERENCES EcsSecurityGroupVO (uuid) ON DELETE RESTRICT;
ALTER TABLE EcsInstanceVO ADD CONSTRAINT fkEcsInstanceVOIdentityZoneVO FOREIGN KEY (identityZoneUuid) REFERENCES IdentityZoneVO (uuid) ON DELETE RESTRICT;

ALTER TABLE HybridEipAddressVO ADD COLUMN `name` varchar(128) NOT NULL DEFAULT 'Unknown';
ALTER TABLE HybridEipAddressVO ADD COLUMN dataCenterUuid varchar(32) NOT NULL;
ALTER TABLE HybridEipAddressVO ADD COLUMN chargeType varchar(32) NOT NULL default "PayByTraffic";
ALTER TABLE HybridEipAddressVO ADD COLUMN allocateTime timestamp DEFAULT '0000-00-00 00:00:00';
ALTER TABLE HybridEipAddressVO ADD CONSTRAINT fkHybridEipAddressVODataCenterVO FOREIGN KEY (dataCenterUuid) REFERENCES DataCenterVO (uuid) ON DELETE RESTRICT;
SET FOREIGN_KEY_CHECKS = 1;

ALTER TABLE `zstack`.`ImageEO` ADD COLUMN exportMd5Sum varchar(255) DEFAULT NULL;
DROP VIEW IF EXISTS `zstack`.`ImageVO`;
CREATE VIEW `zstack`.`ImageVO` AS SELECT uuid, name, description, status, state, size, actualSize, md5Sum, exportMd5Sum, platform, type, format, url, system, mediaType, createDate, lastOpDate, guestOsType, exportUrl FROM `zstack`.`ImageEO` WHERE deleted IS NULL;

ALTER TABLE HostCapacityVO MODIFY availableCpu bigint(20) NOT NULL COMMENT 'used cpu of host in HZ';

UPDATE PrimaryStorageCapacityVO t0,
(SELECT SUM(systemUsedCapacity) ps_systemUsedCapacity , primaryStorageUuid FROM LocalStorageHostRefVO GROUP BY primaryStorageUuid) t1
SET t0.systemUsedCapacity = t1.ps_systemUsedCapacity
WHERE t0.uuid = t1.primaryStorageUuid;

ALTER TABLE SecurityGroupRuleVO ADD COLUMN `remoteSecurityGroupUuid` varchar(255) DEFAULT NULL;
ALTER TABLE SecurityGroupRuleVO ADD CONSTRAINT fkSecurityGroupRuleVORemoteSecurityGroupVO FOREIGN KEY (remoteSecurityGroupUuid) REFERENCES SecurityGroupVO (uuid) ON DELETE CASCADE ;

-- ----------------------------
--  Table structure for `BaremetalHardwareInfoVO`
-- ----------------------------
CREATE TABLE `BaremetalHardwareInfoVO` (
  `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'uuid',
  `chassisUuid` varchar(32) NOT NULL COMMENT 'baremetal chassis uuid',
  `type` varchar(255) DEFAULT NULL,
  `content` text DEFAULT NULL,
  `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp,
  PRIMARY KEY  (`uuid`),
  CONSTRAINT `fkBaremetalHardwareInfoVOBaremetalChassisVO` FOREIGN KEY (`chassisUuid`) REFERENCES `BaremetalChassisVO` (`uuid`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
--  Table structure for `BaremetalConsoleProxyVO`
-- ----------------------------
CREATE TABLE `BaremetalConsoleProxyVO` (
  `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'uuid',
  `chassisUuid` varchar(32) NOT NULL COMMENT 'baremetal chassis uuid',
  `token` varchar(255) NOT NULL COMMENT 'baremetal console proxy token',
  PRIMARY KEY  (`uuid`),
  CONSTRAINT `fkBaremetalConsoleProxyVOBaremetalChassisVO` FOREIGN KEY (`chassisUuid`) REFERENCES `BaremetalChassisVO` (`uuid`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
--  Table structure for `BaremetalHostBondingVO`
-- ----------------------------
CREATE TABLE `BaremetalHostBondingVO` (
  `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'uuid',
  `hostCfgUuid` varchar(32) NOT NULL COMMENT 'baremetal hostcfg uuid',
  `name` varchar(255) NOT NULL COMMENT 'bond name',
  `slaves` varchar(1024) NOT NULL COMMENT 'bond slaves',
  `mode` varchar(32)  NOT NULL COMMENT 'bond slaves',
  `ip` varchar(32) DEFAULT NULL COMMENT 'bond ip',
  `netmask` varchar(32) DEFAULT NULL COMMENT 'bond netmask',
  `gateway` varchar(32) DEFAULT NULL COMMENT 'bond gateway',
  `dns` varchar(32) DEFAULT NULL COMMENT 'bond dns',
  `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp,
  PRIMARY KEY  (`uuid`),
  CONSTRAINT `fkBaremetalHostBondingVOBaremetalHostCfgVO` FOREIGN KEY (`hostCfgUuid`) REFERENCES `BaremetalHostCfgVO` (`uuid`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 0;
DELETE FROM ResourceVO WHERE resourceType="BaremetalHostNicCfgVO";
DELETE FROM ResourceVO WHERE resourceType="BaremetalHostCfgVO";
ALTER TABLE BaremetalHostNicCfgVO DROP INDEX ip;
ALTER TABLE BaremetalHostNicCfgVO MODIFY ip varchar(32) DEFAULT NULL;
ALTER TABLE BaremetalHostNicCfgVO MODIFY netmask varchar(32) DEFAULT NULL;
DROP TRIGGER IF EXISTS trigger_attach_eip_for_ecsinstance;

ALTER TABLE OssBucketVO ADD description varchar(1024) DEFAULT NULL;
ALTER TABLE OssBucketVO MODIFY COLUMN bucketName varchar(64);
ALTER TABLE OssBucketVO ADD COLUMN regionName varchar(64) DEFAULT NULL;
ALTER TABLE OssBucketVO ADD COLUMN dataCenterUuid varchar(32) NOT NULL;
ALTER TABLE OssBucketVO ADD CONSTRAINT fkOssBucketVODataCenterVO FOREIGN KEY (dataCenterUuid) REFERENCES DataCenterVO (uuid) ON DELETE RESTRICT;
UPDATE OssBucketVO SET dataCenterUuid =(SELECT b.uuid FROM DataCenterVO b, (SELECT * FROM OssBucketVO) a WHERE b.regionId=a.regionId limit 1);
ALTER TABLE OssBucketVO DROP COLUMN regionId;
ALTER TABLE OssBucketVO ADD COLUMN `current` varchar(32) DEFAULT "false";
DROP TABLE IF EXISTS OssBucketEcsDataCenterRefVO;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE `PciDeviceOfferingVO` (
  `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'uuid',
  `type` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `vendorId` varchar(64) NOT NULL,
  `deviceId` varchar(64) NOT NULL,
  `subvendorId` varchar(64) DEFAULT NULL,
  `subdeviceId` varchar(64) DEFAULT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PciDeviceVO` (
  `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'uuid',
  `description` varchar(2048) DEFAULT NULL,
  `hostUuid` varchar(32) NOT NULL,
  `vmInstanceUuid` varchar(32) DEFAULT NULL,
  `type` varchar(32) NOT NULL,
  `status` varchar(32) NOT NULL,
  `state` varchar(32) NOT NULL,
  `pciDeviceAddress` varchar(32) NOT NULL,
  `vendorId` varchar(64) NOT NULL,
  `deviceId` varchar(64) NOT NULL,
  `subvendorId` varchar(64) DEFAULT NULL,
  `subdeviceId` varchar(64) DEFAULT NULL,
  `metadata` varchar(4096) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  CONSTRAINT fkPciDeviceVOHostEO FOREIGN KEY (`hostUuid`) REFERENCES `zstack`.`HostEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PciDeviceOfferingInstanceOfferingRefVO` (
  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
  `pciDeviceOfferingUuid` varchar(32) NOT NULL,
  `instanceOfferingUuid` varchar(32) NOT NULL,
  `metadata` varchar(4096) DEFAULT NULL,
  `pciDeviceCount` int DEFAULT 1,
  CONSTRAINT `PciDeviceOfferingInstanceOfferingRefVOPciDeviceOfferingVO` FOREIGN KEY (`pciDeviceOfferingUuid`) REFERENCES `zstack`.`PciDeviceOfferingVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `PciDeviceOfferingInstanceOfferingRefVOInstanceOfferingEO` FOREIGN KEY (`instanceOfferingUuid`) REFERENCES `zstack`.`InstanceOfferingEO` (`uuid`) ON DELETE CASCADE,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PciDevicePciDeviceOfferingRefVO` (
  `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
  `pciDeviceUuid` varchar(32) NOT NULL,
  `pciDeviceOfferingUuid` varchar(32) NOT NULL,
  CONSTRAINT `PciDeviceUsageVOPciDeviceVO` FOREIGN KEY (`pciDeviceUuid`) REFERENCES `zstack`.`PciDeviceVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `PciDevicePciDeviceOfferingVO` FOREIGN KEY (`pciDeviceOfferingUuid`) REFERENCES `zstack`.`PciDeviceOfferingVO` (`uuid`) ON DELETE CASCADE,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 0;
ALTER TABLE DataCenterVO DROP FOREIGN KEY fkDataCenterVOEcsVpcVO;
ALTER TABLE DataCenterVO DROP COLUMN defaultVpcUuid;
ALTER TABLE IdentityZoneVO DROP FOREIGN KEY fkIdentityZoneVOEcsVSwitchVO;
ALTER TABLE IdentityZoneVO DROP COLUMN defaultVSwitchUuid;
ALTER TABLE VpcVirtualRouteEntryVO CHANGE COLUMN nextHopVRiUuid nextHopId varchar(128) DEFAULT NULL;
ALTER TABLE AvailableInstanceTypesVO MODIFY COLUMN instanceType varchar(4096) DEFAULT NULL;
DROP TABLE IF EXISTS EcsImageMd5SumMappingVO;
UPDATE EcsImageVO SET type = 'custom' WHERE type = 'aliyun';
ALTER TABLE EcsImageVO MODIFY ossMd5Sum varchar(128) DEFAULT NULL;
ALTER TABLE EcsImageVO MODIFY ecsImageId varchar(128) NOT NULL;
SET FOREIGN_KEY_CHECKS = 1;

# add default SecurityGroupRule for ZSTAC-5386
DELIMITER $$
CREATE PROCEDURE securityGroupRule()
BEGIN
    DECLARE in_rule_uuid varchar(32);
    DECLARE out_rule_uuid varchar(32);
    DECLARE sgUuid varchar(32);
    DECLARE done INT DEFAULT FALSE;
    DECLARE cur CURSOR FOR SELECT uuid FROM zstack.SecurityGroupVO;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    OPEN cur;
    read_loop: LOOP
    FETCH cur INTO sgUuid;
    IF done THEN
        LEAVE read_loop;
    END IF;
    SET in_rule_uuid = REPLACE(UUID(), '-', '');
    SET out_rule_uuid = REPLACE(UUID(), '-', '');

    INSERT zstack.ResourceVO(uuid, resourceType) value (in_rule_uuid, 'SecurityGroupRuleVO');
    INSERT zstack.SecurityGroupRuleVO(uuid, securityGroupUuid, type, protocol, allowedCidr, startPort, endPort, state, remoteSecurityGroupUuid, lastOpDate, createDate)
    value (in_rule_uuid, sgUuid, 'Ingress', 'ALL', '0.0.0.0/0', -1, -1, 'Enabled', sgUuid, NOW(), NOW());

    INSERT zstack.ResourceVO(uuid, resourceType) value (out_rule_uuid, 'SecurityGroupRuleVO');
    INSERT zstack.SecurityGroupRuleVO(uuid, securityGroupUuid, type, protocol, allowedCidr, startPort, endPort, state, remoteSecurityGroupUuid, lastOpDate, createDate)
    value (out_rule_uuid, sgUuid, 'Egress', 'ALL', '0.0.0.0/0', -1, -1, 'Enabled', sgUuid, NOW(), NOW());
    END LOOP;
    CLOSE cur;
    # work around a bug of mysql : jira.mariadb.org/browse/MDEV-4602
    SELECT CURTIME();
END $$
DELIMITER ;

CALL securityGroupRule();
DROP PROCEDURE IF EXISTS securityGroupRule;

DELIMITER $$

DROP FUNCTION IF EXISTS `Json_getKeyValue` $$

CREATE FUNCTION `Json_getKeyValue`(
    in_JsonArray VARCHAR(4096),
    in_KeyName VARCHAR(64)
) RETURNS VARCHAR(4096) CHARSET utf8

BEGIN
    DECLARE vs_return, vs_JsonArray, vs_JsonString, vs_Json, vs_KeyName VARCHAR(4096);
    DECLARE vi_pos1, vi_pos2 SMALLINT UNSIGNED;

    SET vs_JsonArray = TRIM(in_JsonArray);
    SET vs_KeyName = TRIM(in_KeyName);

    IF vs_JsonArray = '' OR vs_JsonArray IS NULL
        OR vs_KeyName = '' OR vs_KeyName IS NULL
    THEN
        SET vs_return = NULL;
    ELSE
        SET vs_JsonArray = REPLACE(REPLACE(vs_JsonArray, '[', ''), ']', '');
        SET vs_JsonString = CONCAT("'", vs_JsonArray, "'");
        SET vs_json = SUBSTRING_INDEX(SUBSTRING_INDEX(vs_JsonString,'}',1), '{', -1);

        IF vs_json = '' OR vs_json IS NULL THEN
            SET vs_return = NULL;
        ELSE
            SET vs_KeyName = CONCAT('"', vs_KeyName, '":');
            SET vi_pos1 = INSTR(vs_json, vs_KeyName);

            IF vi_pos1 > 0 THEN
                SET vi_pos1 = vi_pos1 + CHAR_LENGTH(vs_KeyName);
                SET vi_pos2 = LOCATE('","', vs_json, vi_pos1);

                IF vi_pos2 = 0 THEN
                    SET vi_pos2 = CHAR_LENGTH(vs_json) + 1;
                END IF;

            SET vs_return = REPLACE(MID(vs_json, vi_pos1, vi_pos2 - vi_pos1), '"', '');
            END IF;
        END IF;
    END IF;


    RETURN(vs_return);
END$$

DELIMITER  ;

DELIMITER $$

DROP FUNCTION IF EXISTS `Upgrade_Scheduler` $$

CREATE FUNCTION `Upgrade_Scheduler` (
    uuid varchar(32),
    targetResourceUuid varchar(32),
    schedulerName varchar(255),
    schedulerDescription varchar(2048),
    schedulerType varchar(255),
    schedulerInterval int unsigned,
    repeatCount int unsigned,
    cronScheduler varchar(255),
    jobClassName varchar(255),
    jobData varchar(65535),
    state varchar(255),
    startTime timestamp,
    stopTime timestamp,
    createDate timestamp
) RETURNS VARCHAR(512) CHARSET utf8

BEGIN
    DECLARE trigger_uuid, job_uuid varchar(32);
    DECLARE job_data text;
    DECLARE job_class_name varchar(255);
    DECLARE current_time_stamp timestamp;

    SET job_class_name = REPLACE(REPLACE(jobClassName,'compute','scheduler'), 'storage', 'scheduler');

    SET job_data = CONCAT('{"targetResourceUuid":"', Json_getKeyValue(jobData, 'targetResourceUuid'), '",'
                    ,'"name":"', Json_getKeyValue(jobData, 'schedulerName'),'",'
                    ,'"createDate":"', Json_getKeyValue(jobData, 'createDate'), '",'
                    ,'"accountUuid":"', Json_getKeyValue(jobData, 'accountUuid'),'"}');

    SET trigger_uuid = REPLACE(UUID(),'-','');
    SET job_uuid = REPLACE(UUID(),'-','');
    SET current_time_stamp = current_timestamp();
    INSERT INTO SchedulerJobVO (`uuid`, `targetResourceUuid`, `name`, `description`, `jobClassName`, `jobData`, `managementNodeUuid`, `state`, `lastOpDate`, `createDate`) VALUES (job_uuid, targetResourceUuid, schedulerName, schedulerDescription, job_class_name, job_data, NULL, state, current_time_stamp, current_time_stamp);
    INSERT INTO ResourceVO (`uuid`, `resourceName`, `resourceType`) VALUES (job_uuid, schedulerName, 'SchedulerJobVO');
    INSERT INTO SchedulerTriggerVO (`uuid`, `name`, `description`, `schedulerType`, `schedulerInterval`, `repeatCount`, `managementNodeUuid`, `startTime`, `stopTime`, `lastOpDate`, `createDate`) VALUES (trigger_uuid, schedulerName, schedulerDescription, schedulerType, schedulerInterval, repeatCount, NULL, startTime, stopTime, current_time_stamp, current_time_stamp);
    INSERT INTO ResourceVO (`uuid`, `resourceName`, `resourceType`) VALUES (trigger_uuid, schedulerName, 'SchedulerTriggerVO');
    INSERT INTO SchedulerJobSchedulerTriggerRefVO (`uuid`, `schedulerJobUuid`, `schedulerTriggerUuid`, `lastOpDate`, `createDate`) VALUES (REPLACE(UUID(),'-',''), job_uuid, trigger_uuid, current_time_stamp, current_time_stamp);
    RETURN(trigger_uuid);
END$$

DELIMITER  ;

select Upgrade_Scheduler(uuid, targetResourceUuid, schedulerName, schedulerDescription, schedulerType, schedulerInterval, repeatCount, cronScheduler, jobClassName, jobData, state, startTime, stopTime, createDate) from SchedulerVO;

insert into NetworkServiceTypeVO (`networkServiceProviderUuid`, `type`) select uuid, 'CentralizedDNS' from NetworkServiceProviderVO where type='vrouter';
insert into NetworkServiceTypeVO (`networkServiceProviderUuid`, `type`) select uuid, 'VipQos' from NetworkServiceProviderVO where type='vrouter';
insert into NetworkServiceTypeVO (`networkServiceProviderUuid`, `type`) select uuid, 'VRouterRoute' from NetworkServiceProviderVO where type='vrouter';

alter table `CephPrimaryStorageMonVO` ADD UNIQUE KEY (`hostname`, `primaryStorageUuid`);
