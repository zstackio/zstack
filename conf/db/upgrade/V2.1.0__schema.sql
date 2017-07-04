alter table `EcsSecurityGroupRuleVO` add `externalGroupId` varchar(128) NOT NULL;
alter table `EcsSecurityGroupRuleVO` DROP COLUMN `sourceGroupId`;

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

alter table OssBucketVO add description varchar(1024) DEFAULT NULL;

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
	  `psk` varchar(32) NOT NULL,
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
	  UNIQUE KEY `ukVpcVpnIkeConfigVO` (`name`,`accountName`) USING BTREE,
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
	  UNIQUE KEY `ukVpcVpnIpSecConfigVO` (`name`,`accountName`) USING BTREE,
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

ALTER TABLE OssBucketVO MODIFY COLUMN bucketName varchar(64);
ALTER TABLE OssBucketVO ADD COLUMN regionName varchar(64) DEFAULT NULL;
ALTER TABLE OssBucketVO modify COLUMN bucketName varchar(64);
-- ----------------------------
--  Table structure for `SchedulerJobVO`
-- ----------------------------
CREATE TABLE  `zstack`.`SchedulerJobVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `targetResourceUuid` varchar(32) NOT NULL,
    `name` varchar(255) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `jobClassName` varchar(255),
    `jobData` varchar(2048),
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
    `jobGroup` varchar(255),
    `triggerGroup` varchar(255),
    `taskData` varchar(2048),
    `taskClassName` varchar(255),
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
  `partNumber` varchar(32) NOT NULL,
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

CREATE TABLE `ConnectionRelationShipVO` (
  `uuid` varchar(32) NOT NULL UNIQUE,
  `name` varchar(128) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `relationShips` varchar(32) NOT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `HybridConnectionRefVO` (
  `uuid` varchar(32) NOT NULL UNIQUE,
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
ALTER TABLE HybridEipAddressVO ADD COLUMN `name` varchar(128) NOT NULL DEFAULT 'Unknown';

CREATE TABLE `MonitorTriggerVO` (
  `uuid` varchar(32) NOT NULL UNIQUE,
  `name` varchar(255) NOT NULL,
  `expression` varchar(2048) NOT NULL,
  `recoveryExpression` varchar(2048) DEFAULT NULL,
  `description` varchar(2048) DEFAULT NULL,
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
  PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `MonitorTriggerActionRefVO` (
  `actionUuid` varchar(32) NOT NULL,
  `triggerUuid` varchar(32) NOT NULL,
  `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp,
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
  `rawData` text DEFAULT NULL,
  `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp,
  PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# Foreign keys for table EmailTriggerActionVO

ALTER TABLE EmailTriggerActionVO ADD CONSTRAINT fkEmailTriggerActionVOMonitorTriggerActionVO FOREIGN KEY (uuid) REFERENCES MonitorTriggerActionVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE;

# Foreign keys for table EmailMediaVO

ALTER TABLE EmailMediaVO ADD CONSTRAINT fkEmailMediaVOMediaVO FOREIGN KEY (uuid) REFERENCES MediaVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE;

# Foreign keys for table MonitorTriggerActionRefVO

ALTER TABLE MonitorTriggerActionRefVO ADD CONSTRAINT fkMonitorTriggerActionRefVOMonitorTriggerActionVO FOREIGN KEY (actionUuid) REFERENCES MonitorTriggerActionVO (uuid) ON DELETE CASCADE;
ALTER TABLE MonitorTriggerActionRefVO ADD CONSTRAINT fkMonitorTriggerActionRefVOMonitorTriggerVO FOREIGN KEY (triggerUuid) REFERENCES MonitorTriggerVO (uuid) ON DELETE CASCADE;

# Foreign keys for table MonitorTriggerVO

ALTER TABLE MonitorTriggerVO ADD CONSTRAINT fkMonitorTriggerVOResourceVO FOREIGN KEY (targetResourceUuid) REFERENCES ResourceVO (uuid) ON DELETE CASCADE;
ALTER TABLE EcsSecurityGroupRuleVO DROP COLUMN externalGroupId;


SET FOREIGN_KEY_CHECKS = 0;
alter table EcsInstanceVO drop foreign key fkEcsInstanceVOEcsVpcVO;
alter table EcsInstanceVO drop key fkEcsInstanceVOEcsVpcVO;
alter table EcsInstanceVO drop column ecsVpcUuid;

alter table EcsInstanceVO modify column ecsVSwitchUuid varchar(32) NOT NULL;
alter table EcsInstanceVO modify column ecsSecurityGroupUuid varchar(32) NOT NULL;

alter table EcsInstanceVO drop foreign key fkEcsInstanceVOEcsVSwitchVO;
alter table EcsInstanceVO drop foreign key fkEcsInstanceVOIdentityZoneVO;
alter table EcsInstanceVO drop foreign key fkEcsInstanceVOEcsSecurityGroupVO;
alter table EcsInstanceVO drop foreign key fkEcsInstanceVOEcsImageVO;

alter table EcsInstanceVO add CONSTRAINT fkEcsInstanceVOEcsVSwitchVO foreign key (ecsVSwitchUuid) references EcsVSwitchVO (uuid) on delete restrict;
alter table EcsInstanceVO add CONSTRAINT fkEcsInstanceVOEcsSecurityGroupVO foreign key (ecsSecurityGroupUuid) references EcsSecurityGroupVO (uuid) on delete restrict;
alter table EcsInstanceVO add CONSTRAINT fkEcsInstanceVOIdentityZoneVO foreign key (identityZoneUuid) references IdentityZoneVO (uuid) on delete restrict;
alter table EcsInstanceVO add CONSTRAINT fkEcsInstanceVOEcsImageVO foreign key (ecsImageUuid) references EcsImageVO (uuid) on delete restrict;

alter table EcsInstanceVO modify column ecsImageUuid varchar(32) NOT NULL;
alter table HybridEipAddressVO add column dataCenterUuid varchar(32) not null;
alter table HybridEipAddressVO add column chargeType varchar(32) not null default "PayByTraffic";
alter table HybridEipAddressVO add column allocateTime timestamp DEFAULT '0000-00-00 00:00:00';
alter table HybridEipAddressVO add CONSTRAINT fkHybridEipAddressVODataCenterVO foreign key (dataCenterUuid) references DataCenterVO (uuid) on delete restrict;
SET FOREIGN_KEY_CHECKS = 1;

ALTER TABLE `zstack`.`ImageEO` ADD COLUMN exportMd5Sum varchar(255) DEFAULT NULL;
DROP VIEW IF EXISTS `zstack`.`ImageVO`;
CREATE VIEW `zstack`.`ImageVO` AS SELECT uuid, name, description, status, state, size, actualSize, md5Sum, exportMd5Sum, platform, type, format, url, system, mediaType, createDate, lastOpDate, guestOsType, exportUrl FROM `zstack`.`ImageEO` WHERE deleted IS NULL;

ALTER TABLE HostCapacityVO MODIFY availableCpu bigint(20) NOT NULL COMMENT 'used cpu of host in HZ';

UPDATE PrimaryStorageCapacityVO t0,
(SELECT SUM(systemUsedCapacity) ps_systemUsedCapacity , primaryStorageUuid FROM LocalStorageHostRefVO GROUP BY primaryStorageUuid) t1
SET t0.systemUsedCapacity = t1.ps_systemUsedCapacity
WHERE t0.uuid = t1.primaryStorageUuid;

ALTER TABLE HostCapacityVO MODIFY availableCpu bigint(20) NOT NULL COMMENT 'used cpu of host in HZ';

ALTER TABLE SecurityGroupRuleVO ADD COLUMN `remoteSecurityGroupUuid` varchar(255) DEFAULT NULL;
ALTER TABLE SecurityGroupRuleVO ADD CONSTRAINT fkSecurityGroupRuleVORemoteSecurityGroupVO FOREIGN KEY (remoteSecurityGroupUuid) REFERENCES SecurityGroupVO (uuid) ON DELETE CASCADE ;

-- ----------------------------
--  Table structure for `BaremetalHardwareInfoVO`
-- ----------------------------
CREATE TABLE `BaremetalHardwareInfoVO` (
  `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'uuid',
  `ipmiAddress` varchar(32) NOT NULL COMMENT 'baremetal chassis ipmi address',
  `type` varchar(255) DEFAULT NULL,
  `content` varchar(2048) DEFAULT NULL,
  `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp,
  PRIMARY KEY  (`uuid`),
  CONSTRAINT `fkBaremetalHardwareInfoVOBaremetalChassisVO` FOREIGN KEY (`ipmiAddress`) REFERENCES `BaremetalChassisVO` (`ipmiAddress`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
--  Table structure for `BaremetalHostBondingVO`
-- ----------------------------
CREATE TABLE `BaremetalHostBondingVO` (
  `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'uuid',
  `hostCfgUuid` varchar(32) NOT NULL COMMENT 'baremetal hostcfg uuid',
  `name` varchar(255) NOT NULL COMMENT 'bond name',
  `slaves` varchar(1024) NOT NULL COMMENT 'bond slaves',
  `mode` varchar(32)  DEFAULT '4' COMMENT 'bond slaves',
  `ip` varchar(32) DEFAULT NULL COMMENT 'bond ip',
  `netmask` varchar(32) DEFAULT NULL COMMENT 'bond netmask',
  `gateway` varchar(32) DEFAULT NULL COMMENT 'bond gateway',
  `dns` varchar(32) DEFAULT NULL COMMENT 'bond dns',
  `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp,
  PRIMARY KEY  (`uuid`),
  CONSTRAINT `fkBaremetalHostBondingVOBaremetalHostCfgVO` FOREIGN KEY (`hostCfgUuid`) REFERENCES `BaremetalHostCfgVO` (`uuid`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DELETE FROM ResourceVO WHERE resourceType="BaremetalHostNicCfgVO";
DELETE FROM ResourceVO WHERE resourceType="BaremetalHostCfgVO";
ALTER TABLE BaremetalHostNicCfgVO DROP INDEX ip;
ALTER TABLE BaremetalHostNicCfgVO MODIFY ip varchar(32) DEFAULT NULL;
ALTER TABLE BaremetalHostNicCfgVO MODIFY netmask varchar(32) DEFAULT NULL;
