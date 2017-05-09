SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `IdentityZoneVO`
-- ----------------------------
CREATE TABLE `IdentityZoneVO` (
	  `uuid` varchar(32) UNIQUE NOT NULL,
	  `zoneId` varchar(32) NOT NULL,
	  `dataCenterUuid` varchar(32) NOT NULL,
	  `type` varchar(32) NOT NULL,
	  `zoneName` varchar(128) NOT NULL,
	  `deleted` varchar(1) DEFAULT NULL,
	  `defaultVSwitchUuid` varchar(32) DEFAULT NULL,
	  `description` varchar(1024) DEFAULT NULL,
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`uuid`),
	  CONSTRAINT fkIdentityZoneVODataCenterVO FOREIGN KEY (dataCenterUuid) REFERENCES DataCenterVO (uuid) ON DELETE RESTRICT,
	  CONSTRAINT fkIdentityZoneVOEcsVSwitchVO FOREIGN KEY (defaultVSwitchUuid) REFERENCES EcsVSwitchVO (uuid) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;


SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `EcsVSwitchVO`
-- ----------------------------
CREATE TABLE `EcsVSwitchVO` (
	  `uuid` varchar(32) UNIQUE NOT NULL,
	  `vSwitchId` varchar(32) NOT NULL,
	  `status` varchar(32) NOT NULL,
	  `cidrBlock` varchar(32) NOT NULL,
	  `availableIpAddressCount` int(10) NOT NULL,
	  `description` varchar(1024) DEFAULT NULL,
	  `vSwitchName` varchar(128) NOT NULL,
	  `ecsVpcUuid` varchar(32) NOT NULL,
	  `identityZoneUuid` varchar(32) NOT NULL,
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`uuid`),
	  CONSTRAINT fkEcsVSwitchVOEcsVpcVO FOREIGN KEY (ecsVpcUuid) REFERENCES EcsVpcVO (uuid) ON DELETE RESTRICT,
	  CONSTRAINT fkEcsVSwitchVOIdentityZoneVO FOREIGN KEY (identityZoneUuid) REFERENCES IdentityZoneVO (uuid) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;

SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `EcsVpcVO`
-- ----------------------------
CREATE TABLE `EcsVpcVO` (
	  `uuid` varchar(32) UNIQUE NOT NULL,
	  `ecsVpcId` varchar(32) NOT NULL,
	  `dataCenterUuid` varchar(32) NOT NULL,
	  `status` varchar(32) NOT NULL,
	  `deleted` varchar(1) DEFAULT NULL,
	  `vpcName` varchar(128) NOT NULL,
	  `cidrBlock` varchar(32) NOT NULL,
	  `vRouterId` varchar(32) NOT NULL,
	  `description` varchar(1024) DEFAULT NULL,
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`uuid`),
	  CONSTRAINT fkEcsVpcVODataCenterVO FOREIGN KEY (dataCenterUuid) REFERENCES DataCenterVO (uuid) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;


SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `EcsSecurityGroupRuleVO`
-- ----------------------------
CREATE TABLE `EcsSecurityGroupRuleVO` (
	  `uuid` varchar(32) UNIQUE NOT NULL,
	  `ecsSecurityGroupUuid` varchar(32) NOT NULL,
	  `portRange` varchar(32) NOT NULL,
	  `cidrIp` varchar(32) NOT NULL,
	  `protocol` varchar(32) NOT NULL,
	  `nicType` varchar(32) NOT NULL,
	  `policy` varchar(32) NOT NULL,
	  `sourceGroupId` varchar(128) NOT NULL,
	  `direction` varchar(128) NOT NULL,
	  `priority` varchar(128) NOT NULL,
	  `description` varchar(1024) DEFAULT NULL,
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`uuid`),
	  CONSTRAINT fkEcsSecurityGroupRuleVOEcsSecurityGroupVO FOREIGN KEY (ecsSecurityGroupUuid) REFERENCES EcsSecurityGroupVO (uuid) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;

SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `EcsSecurityGroupVO`
-- ----------------------------
CREATE TABLE `EcsSecurityGroupVO` (
	  `uuid` varchar(32) UNIQUE NOT NULL,
	  `ecsVpcUuid` varchar(32) NOT NULL,
	  `securityGroupId` varchar(32) NOT NULL,
	  `securityGroupName` varchar(128) NOT NULL,
	  `description` varchar(1024) DEFAULT NULL,
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`uuid`),
	  UNIQUE KEY `ukEcsVpcUuidSecurityGroupId` (`ecsVpcUuid`,`securityGroupId`) USING BTREE,
	  CONSTRAINT fkEcsSecurityGroupVOEcsVpcVO FOREIGN KEY (ecsVpcUuid) REFERENCES EcsVpcVO (uuid) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;


SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `EcsInstanceConsoleProxyVO`
-- ----------------------------
CREATE TABLE `EcsInstanceConsoleProxyVO` (
	  `uuid` varchar(32) UNIQUE NOT NULL,
	  `ecsInstanceUuid` varchar(32) NOT NULL,
	  `vncUrl` varchar(256) DEFAULT NULL,
	  `vncPassword` varchar(32) DEFAULT NULL,
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`uuid`),
	  UNIQUE KEY `ecsInstanceUuid` (`ecsInstanceUuid`),
	  CONSTRAINT fkEcsInstanceConsoleProxyVOEcsInstanceVO FOREIGN KEY (ecsInstanceUuid) REFERENCES EcsInstanceVO(uuid) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;

SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `EcsInstanceVO`
-- ----------------------------
CREATE TABLE `EcsInstanceVO` (
	  `uuid` varchar(32) UNIQUE NOT NULL,
	  `localVmInstanceUuid` varchar(32) DEFAULT NULL,
	  `ecsInstanceId` varchar(32) NOT NULL,
	  `name` varchar(128) NOT NULL,
	  `ecsStatus` varchar(16) NOT NULL,
	  `ecsInstanceRootPassword` varchar(32) NOT NULL,
	  `cpuCores` int(10) NOT NULL,
	  `memorySize` bigint(20) NOT NULL,
	  `ecsInstanceType` varchar(32) NOT NULL,
	  `ecsBandWidth` bigint(20) NOT NULL,
	  `ecsRootVolumeId` varchar(32) NOT NULL,
	  `ecsRootVolumeCategory` varchar(32) NOT NULL,
	  `ecsRootVolumeSize` bigint(20) NOT NULL,
	  `privateIpAddress` varchar(32) NOT NULL,
	  `ecsEipUuid` varchar(32) DEFAULT NULL,
	  `ecsVpcUuid` varchar(32) DEFAULT NULL,
	  `ecsVSwitchUuid` varchar(32) DEFAULT NULL,
	  `ecsImageUuid` varchar(32) DEFAULT NULL,
	  `ecsSecurityGroupUuid` varchar(32) DEFAULT NULL,
	  `identityZoneUuid` varchar(32) NOT NULL,
	  `description` varchar(1024) DEFAULT NULL,
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  PRIMARY KEY (`uuid`),
      KEY `fkEcsInstanceVOEcsImageVO` (`ecsImageUuid`),
      KEY `fkEcsInstanceVOEcsSecurityGroupVO` (`ecsSecurityGroupUuid`),
      KEY `fkEcsInstanceVOEcsVSwitchVO` (`ecsVSwitchUuid`),
      KEY `fkEcsInstanceVOEcsVpcVO` (`ecsVpcUuid`),
      KEY `fkEcsInstanceVOIdentityZoneVO` (`identityZoneUuid`),
      KEY `fkEcsInstanceVOVmInstanceEO` (`localVmInstanceUuid`),
      CONSTRAINT `fkEcsInstanceVOEcsImageVO` FOREIGN KEY (`ecsImageUuid`) REFERENCES `EcsImageVO` (`uuid`) ON DELETE SET NULL,
      CONSTRAINT `fkEcsInstanceVOEcsSecurityGroupVO` FOREIGN KEY (`ecsSecurityGroupUuid`) REFERENCES `EcsSecurityGroupVO` (`uuid`) ON DELETE RESTRICT,
      CONSTRAINT `fkEcsInstanceVOEcsVpcVO` FOREIGN KEY (`ecsVpcUuid`) REFERENCES `EcsVpcVO` (`uuid`) ON DELETE RESTRICT,
      CONSTRAINT `fkEcsInstanceVOEcsVSwitchVO` FOREIGN KEY (`ecsVSwitchUuid`) REFERENCES `EcsVSwitchVO` (`uuid`) ON DELETE RESTRICT,
      CONSTRAINT `fkEcsInstanceVOIdentityZoneVO` FOREIGN KEY (`identityZoneUuid`) REFERENCES `IdentityZoneVO` (`uuid`) ON DELETE RESTRICT,
      CONSTRAINT `fkEcsInstanceVOVmInstanceEO` FOREIGN KEY (`localVmInstanceUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;

SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `EcsImageVO`
-- ----------------------------
CREATE TABLE `EcsImageVO` (
	  `uuid` varchar(32) UNIQUE NOT NULL,
	  `localImageUuid` varchar(32) DEFAULT NULL,
	  `ecsImageId` varchar(32) NOT NULL,
	  `dataCenterUuid` varchar(32) DEFAULT NULL,
	  `name` varchar(128) NOT NULL,
	  `ecsImageSize` bigint(20) NOT NULL,
	  `platform` varchar(32) NOT NULL,
	  `type` varchar(32) NOT NULL,
	  `ossMd5Sum` varchar(128) NOT NULL,
	  `format` varchar(32) NOT NULL,
	  `description` varchar(1024) DEFAULT NULL,
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`uuid`),
	  CONSTRAINT fkEcsImageVOImageEO FOREIGN KEY (localImageUuid) REFERENCES ImageEO (uuid) ON DELETE SET NULL,
	  CONSTRAINT fkEcsImageVODataCenterVO FOREIGN KEY (dataCenterUuid) REFERENCES DataCenterVO (uuid) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;

SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `HybridEipAddressVO`
-- ----------------------------
CREATE TABLE `HybridEipAddressVO` (
	  `uuid` varchar(32) UNIQUE NOT NULL,
	  `eipId` varchar(32) NOT NULL,
	  `bandWidth` varchar(32) NOT NULL,
	  `eipAddress` varchar(32) NOT NULL,
	  `allocateResourceUuid` varchar(32) DEFAULT NULL,
	  `allocateResourceType` varchar(32) DEFAULT NULL,
	  `status` varchar(16) NOT NULL,
	  `eipType` varchar(32) NOT NULL,
	  `description` varchar(1024) DEFAULT NULL,
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;

SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `EcsImageMd5SumMappingVO`
-- ----------------------------
CREATE TABLE `EcsImageMd5SumMappingVO` (
  `id` bigint(20) unsigned UNIQUE NOT NULL AUTO_INCREMENT,
  `qcow2Md5Sum` varchar(128) NOT NULL,
  `rawMd5Sum` varchar(128) NOT NULL,
  `ossBucketName` varchar(32) NOT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;

SET FOREIGN_KEY_CHECKS = 0;
-- ----------------------------
--  Table structure for `OssBucketVO`
-- ----------------------------
CREATE TABLE `OssBucketVO` (
	  `uuid` varchar(32) UNIQUE NOT NULL,
	  `bucketName` varchar(32) NOT NULL,
	  `regionId` varchar(32) NOT NULL,
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;

SET FOREIGN_KEY_CHECKS = 0;
-- ----------------------------
--  Table structure for `OssBucketEcsDataCenterRefVO`
-- ----------------------------
CREATE TABLE `OssBucketEcsDataCenterRefVO` (
	  `id` bigint(20) unsigned UNIQUE NOT NULL AUTO_INCREMENT,
	  `ossBucketUuid` varchar(32) NOT NULL,
	  `dataCenterUuid` varchar(32) NOT NULL,
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`id`),
	  CONSTRAINT `fkOssBucketEcsDataCenterRefVOOssBucketVO` FOREIGN KEY (`ossBucketUuid`) REFERENCES `zstack`.`OssBucketVO` (`uuid`) ON DELETE CASCADE,
	  CONSTRAINT `fkOssBucketEcsDataCenterRefVODataCenterVO` FOREIGN KEY (`dataCenterUuid`) REFERENCES `zstack`.`DataCenterVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;

SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `DataCenterVO`
-- ----------------------------
CREATE TABLE `DataCenterVO` (
	  `uuid` varchar(32) UNIQUE NOT NULL,
	  `deleted` varchar(1) DEFAULT NULL,
	  `regionName` varchar(1024) NOT NULL,
	  `dcType` varchar(32) NOT NULL,
	  `defaultVpcUuid` varchar(32) DEFAULT NULL,
	  `regionId` varchar(32) NOT NULL,
	  `description` varchar(1024) DEFAULT NULL,
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`uuid`),
	  CONSTRAINT fkDataCenterVOEcsVpcVO FOREIGN KEY (defaultVpcUuid) REFERENCES EcsVpcVO (uuid) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;

SET FOREIGN_KEY_CHECKS = 0;
-- ----------------------------
--  Table structure for `AvailableInstanceTypesVO`
-- ----------------------------
CREATE TABLE `AvailableInstanceTypesVO` (
	  `uuid` bigint(20) unsigned UNIQUE NOT NULL AUTO_INCREMENT,
	  `accountUuid` varchar(32) NOT NULL,
	  `instanceType` varchar(4096) NOT NULL,
	  `diskCategories` varchar(256) NOT NULL,
	  `resourceType` varchar(256) NOT NULL,
	  `izUuid` varchar(32) NOT NULL,
	  `description` varchar(1024) DEFAULT NULL,
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`uuid`),
	  UNIQUE KEY `ukAccountUuidizUuid` (`accountUuid`,`izUuid`) USING BTREE,
	  CONSTRAINT fkAvailableInstanceTypesVOAccountVO FOREIGN KEY (accountUuid) REFERENCES AccountVO (uuid) ON DELETE CASCADE,
	  CONSTRAINT fkAvailableInstanceTypesVOIdentityZoneVO FOREIGN KEY (izUuid) REFERENCES IdentityZoneVO (uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;

SET FOREIGN_KEY_CHECKS = 0;
-- ----------------------------
--  Table structure for `AvailableIdentityZonesVO`
-- ----------------------------
CREATE TABLE `AvailableIdentityZonesVO` (
	  `uuid` bigint(20) unsigned UNIQUE NOT NULL AUTO_INCREMENT,
	  `accountUuid` varchar(32) NOT NULL,
	  `dataCenterUuid` varchar(32) NOT NULL,
	  `type` varchar(32) NOT NULL,
	  `zoneId` varchar(32) NOT NULL,
	  `description` varchar(1024) DEFAULT NULL,
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`uuid`),
	  UNIQUE KEY `ukAccountUuidizUuid` (`accountUuid`,`dataCenterUuid`,`zoneId`) USING BTREE,
	  CONSTRAINT fkAvailableIdentityZonesVOAccountVO FOREIGN KEY (accountUuid) REFERENCES AccountVO (uuid) ON DELETE CASCADE,
	  CONSTRAINT fkAvailableIdentityZonesVODataCenterVO FOREIGN KEY (dataCenterUuid) REFERENCES DataCenterVO (uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;

SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `HybridAccountVO`
-- ----------------------------
CREATE TABLE `HybridAccountVO` (
  `uuid` varchar(32) UNIQUE NOT NULL,
  `name` varchar(32) UNIQUE NOT NULL,
  `accountUuid` varchar(32) NOT NULL,
  `userUuid` varchar(32) DEFAULT NULL,
  `type` varchar(32) NOT NULL,
  `akey` varchar(32) NOT NULL,
  `secret` varchar(64) NOT NULL,
  `current` varchar(64) NOT NULL DEFAULT 'false',
  `description` varchar(1024) DEFAULT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uniqAccountUuid` (`accountUuid`,`akey`),
  CONSTRAINT `fkHybridAccountVOAccountVO` FOREIGN KEY (`accountUuid`) REFERENCES `zstack`.`AccountVO` (`uuid`) ON DELETE RESTRICT,
  CONSTRAINT `fkHybridAccountVOUserVO` FOREIGN KEY (`userUuid`) REFERENCES `zstack`.`UserVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;

# sync EcsVSwitchVO availableIpAddressCount while EcsInstanceVO update
DROP TRIGGER IF EXISTS trigger_decrease_vswitch_for_create_ecs;
DELIMITER $$
CREATE TRIGGER trigger_decrease_vswitch_for_create_ecs after insert ON `EcsInstanceVO`
FOR EACH ROW
BEGIN
update `EcsVSwitchVO` set `availableIpAddressCount`=`availableIpAddressCount`-1 where `uuid`=NEW.`ecsVSwitchUuid`;
END$$
DELIMITER ;



DROP TRIGGER IF EXISTS trigger_attach_eip_for_ecsinstance;
DELIMITER $$
CREATE TRIGGER trigger_attach_eip_for_ecsinstance AFTER UPDATE ON `HybridEipAddressVO`
FOR EACH ROW
    BEGIN
        IF (NEW.`allocateResourceUuid` = NULL) THEN
            update `EcsInstanceVO` set `ecsEipUuid`=NULL
                    where `ecsEipUuid`=OLD.`uuid`
                    and OLD.`eipType`='aliyun'
                    and OLD.`allocateResourceType`='EcsInstanceVO';
        ELSE
            update `EcsInstanceVO` set `ecsEipUuid`=NEW.`uuid`
                                where NEW.`allocateResourceUuid`=`uuid`
                                and NEW.`eipType`='aliyun'
                                and NEW.`allocateResourceType`='EcsInstanceVO';
        END IF;

    END$$
DELIMITER ;


# VxlanNetwork
CREATE TABLE `zstack`.`VxlanNetworkPoolVO` (
  `uuid` varchar(32) NOT NULL UNIQUE,
  PRIMARY KEY  (`uuid`),
	CONSTRAINT fkVxlanNetworkPoolVOL2NetworkEO FOREIGN KEY (uuid) REFERENCES L2NetworkEO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `zstack`.`VtepVO` (
  `uuid` varchar(32) NOT NULL UNIQUE,
  `hostUuid` varchar(32) NOT NULL,
  `vtepIp` varchar(32) NOT NULL,
  `port` int NOT NULL,
  `clusterUuid` varchar(32) NOT NULL,
  `type` varchar(32) NOT NULL,
  `poolUuid` varchar(32) NOT NULL,
	`createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	`lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY  (`uuid`),
	CONSTRAINT fkVtepVOHostEO FOREIGN KEY (hostUuid) REFERENCES HostEO (uuid) ON DELETE CASCADE,
	CONSTRAINT fkVtepVOClusterEO FOREIGN KEY (clusterUuid) REFERENCES ClusterEO (uuid) ON DELETE CASCADE,
	UNIQUE KEY `ukVtepIpPoolUuid` (`vtepIp`,`poolUuid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `zstack`.`VxlanNetworkVO` (
  `uuid` varchar(32) NOT NULL UNIQUE,
  `vni` int NOT NULL,
  `poolUuid` varchar(32),
  PRIMARY KEY  (`uuid`),
	CONSTRAINT fkVxlanNetworkVOL2NetworkEO FOREIGN KEY (uuid) REFERENCES L2NetworkEO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE,
	CONSTRAINT fkVxlanNetworkVOVxlanNetworkPoolVO FOREIGN KEY (poolUuid) REFERENCES VxlanNetworkPoolVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE,
	UNIQUE KEY `ukVniPoolUuid` (`vni`,`poolUuid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `zstack`.`VniRangeVO` (
  `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'uuid',
  `name` varchar(255) DEFAULT NULL COMMENT 'name',
  `description` varchar(2048) DEFAULT NULL COMMENT 'description',
  `l2NetworkUuid` varchar(32) NOT NULL COMMENT 'l3 network uuid',
  `startVni` INT NOT NULL COMMENT 'start vni',
  `endVni` INT NOT NULL COMMENT 'end vni',
	`createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	`lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY  (`uuid`),
	CONSTRAINT fkVniRangeVOL2NetworkEO  FOREIGN KEY (l2NetworkUuid) REFERENCES L2NetworkEO (uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE  `zstack`.`TaskProgressVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `apiId` varchar(32) NOT NULL,
    `taskUuid` varchar(32) NOT NULL,
    `parentUuid` varchar(32) DEFAULT NULL,
    `taskName` varchar(1024) NOT NULL,
    `managementUuid` varchar(32) DEFAULT NULL,
    `type` varchar(255) NOT NULL,
    `content` text DEFAULT NULL,
    `arguments` text DEFAULT NULL,
    `opaque` text DEFAULT NULL,
    `time` bigint unsigned NOT NULL,
    `timeToDelete` bigint unsigned DEFAULT NULL,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# Foreign keys for table TaskProgressVO
ALTER TABLE TaskProgressVO ADD CONSTRAINT fkTaskProgressVOManagementNodeVO FOREIGN KEY (managementUuid) REFERENCES ManagementNodeVO (uuid) ON DELETE SET NULL;

DROP TABLE IF EXISTS ProgressVO;

CREATE TABLE  `zstack`.`NotificationVO` (
    `uuid` varchar(32) NOT NULL,
    `name` varchar(1024) NOT NULL,
    `content` text NOT NULL,
    `arguments` text DEFAULT NULL,
    `sender` varchar(1024) NOT NULL,
    `status` varchar(255) NOT NULL,
    `type` varchar(255) NOT NULL,
    `resourceUuid` varchar(255) DEFAULT NULL,
    `resourceType` varchar(255) DEFAULT NULL,
    `opaque` text DEFAULT NULL,
    `time` bigint unsigned DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    `dateTime` datetime,
    UNIQUE KEY `uuid` (`uuid`, `dateTime`),
    PRIMARY KEY  (`uuid`, `dateTime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 PARTITION BY RANGE( YEAR(dateTime) ) (
    PARTITION p2017 VALUES LESS THAN (2018),
    PARTITION p2018 VALUES LESS THAN (2019),
    PARTITION p2019 VALUES LESS THAN (2020),
    PARTITION p2020 VALUES LESS THAN (2021),
    PARTITION p2021 VALUES LESS THAN (2022),
    PARTITION p2022 VALUES LESS THAN (2023),
    PARTITION p2023 VALUES LESS THAN (2024),
    PARTITION p2024 VALUES LESS THAN (2025),
    PARTITION p2025 VALUES LESS THAN (2026),
    PARTITION p2026 VALUES LESS THAN (2027),
    PARTITION p2027 VALUES LESS THAN (2028),
    PARTITION p9999 VALUES LESS THAN MAXVALUE
);

CREATE TABLE  `zstack`.`NotificationSubscriptionVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(1024) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `notificationName` varchar(1024) NOT NULL,
    `filter` varchar(2048) DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


ALTER TABLE LocalStorageResourceRefVO DROP FOREIGN KEY `fkLocalStorageResourceRefVOHostEO`;

ALTER TABLE VCenterVO ADD port int DEFAULT NULL;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `VpcVirtualRouterVO`
-- ----------------------------
CREATE TABLE `VpcVirtualRouterVO` (
	  `uuid` varchar(32) UNIQUE NOT NULL,
	  `vrId` varchar(32) NOT NULL,
	  `vpcUuid` varchar(32) NOT NULL,
	  `vRouterName` varchar(32) NOT NULL,
	  `description` varchar(1024) DEFAULT NULL,
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`uuid`),
	  CONSTRAINT `fkVpcVirtualRouterVOEcsVpcVO` FOREIGN KEY (`vpcUuid`) REFERENCES `zstack`.`EcsVpcVO` (`uuid`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;

SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `VirtualRouterInterfaceVO`
-- ----------------------------
CREATE TABLE `VirtualRouterInterfaceVO` (
	  `uuid` varchar(32) UNIQUE NOT NULL,
	  `dataCenterUuid` varchar(32) NOT NULL,
	  `routerInterfaceId` varchar(64) NOT NULL,
	  `virtualRouterUuid` varchar(32) NOT NULL,
	  `accessPointUuid` varchar(32) DEFAULT NULL,
	  `vRouterType` varchar(16) NOT NULL,
	  `role` varchar(16) NOT NULL,
	  `spec` varchar(32) NOT NULL,
	  `name` varchar(64) NOT NULL,
	  `status` varchar(32) NOT NULL,
	  `oppositeInterfaceUuid` varchar(32) NOT NULL,
	  `description` varchar(128) DEFAULT NULL,
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`uuid`),
	  CONSTRAINT `fkVirtualRouterInterfaceVOConnectionAccessPointVO` FOREIGN KEY (`accessPointUuid`) REFERENCES `zstack`.`ConnectionAccessPointVO` (`uuid`) ON DELETE RESTRICT,
	  CONSTRAINT `fkVirtualRouterInterfaceVODataCenterVO` FOREIGN KEY (`dataCenterUuid`) REFERENCES `zstack`.`DataCenterVO` (`uuid`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;


SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `VpcVirtualRouteEntryVO`
-- ----------------------------
CREATE TABLE `VpcVirtualRouteEntryVO` (
	  `uuid` varchar(32) UNIQUE NOT NULL,
	  `destinationCidrBlock` varchar(64) NOT NULL,
	  `nextHopVRiUuid` varchar(32) DEFAULT NULL,
	  `type` varchar(32) NOT NULL,
	  `status` varchar(32) NOT NULL,
	  `nextHopType` varchar(32) NOT NULL,
	  `vRouterType` varchar(16) NOT NULL,
	  `virtualRouterUuid` varchar(32) NOT NULL,
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;

SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `ConnectionAccessPointVO`
-- ----------------------------
CREATE TABLE `ConnectionAccessPointVO` (
	  `uuid` varchar(32) UNIQUE NOT NULL,
	  `accessPointId` varchar(64) NOT NULL,
	  `type` varchar(32) NOT NULL,
	  `name` varchar(64) NOT NULL,
	  `dataCenterUuid` varchar(32) NOT NULL,
	  `status` varchar(32) NOT NULL,
	  `hostOperator` varchar(32) NOT NULL,
	  `description` varchar(128) DEFAULT NULL,
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`uuid`),
	  CONSTRAINT `fkConnectionAccessPointVODataCenterVO` FOREIGN KEY (`dataCenterUuid`) REFERENCES `zstack`.`DataCenterVO` (`uuid`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;

SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `VirtualBorderRouterVO`
-- ----------------------------
CREATE TABLE `VirtualBorderRouterVO` (
	  `uuid` varchar(32) UNIQUE NOT NULL,
	  `vbrId` varchar(32) NOT NULL,
	  `vlanInterfaceId` varchar(64) NOT NULL,
	  `status` varchar(16) NOT NULL,
	  `name` varchar(64) NOT NULL,
	  `dataCenterUuid` varchar(32) NOT NULL,
	  `vlanId` varchar(64) NOT NULL,
	  `circuitCode` varchar(32) NOT NULL,
	  `localGatewayIp` varchar(32) NOT NULL,
	  `peerGatewayIp` varchar(32) NOT NULL,
	  `physicalConnectionStatus` varchar(32) NOT NULL,
	  `peeringSubnetMask` varchar(32) NOT NULL,
	  `physicalConnectionId` varchar(32) NOT NULL,
	  `accessPointUuid` varchar(32) NOT NULL,
	  `description` varchar(128) DEFAULT NULL,
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`uuid`),
	  CONSTRAINT `fkVirtualBorderRouterVODataCenterVO` FOREIGN KEY (`dataCenterUuid`) REFERENCES `zstack`.`DataCenterVO` (`uuid`) ON DELETE RESTRICT,
	  CONSTRAINT `fkVirtualBorderRouterVOConnectionAccessPointVO` FOREIGN KEY (`accessPointUuid`) REFERENCES `zstack`.`ConnectionAccessPointVO` (`uuid`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;

ALTER TABLE QuotaVO MODIFY COLUMN id INT;
ALTER TABLE QuotaVO DROP PRIMARY KEY;
ALTER TABLE QuotaVO DROP id;
ALTER TABLE QuotaVO ADD uuid varchar(32);
UPDATE QuotaVO SET uuid = REPLACE(UUID(),'-','') WHERE uuid IS NULL;
ALTER TABLE QuotaVO MODIFY uuid varchar(32) UNIQUE NOT NULL PRIMARY KEY;

ALTER TABLE `zstack`.`JsonLabelVO` modify column resourceUuid varchar(32) DEFAULT NULL;
ALTER TABLE `zstack`.`VipVO` ADD UNIQUE INDEX(`ipRangeUuid`,`ip`);

CREATE TABLE `ResourceVO` (
  `uuid` varchar(32) NOT NULL UNIQUE,
  `resourceName` varchar(255) DEFAULT NULL,
  `resourceType` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT IGNORE INTO ResourceVO (uuid,resourceType) SELECT t.resourceUuid , t.resourceType FROM AccountResourceRefVO t;
INSERT IGNORE INTO ResourceVO (uuid,resourceType) SELECT t.resourceUuid , t.resourceType FROM SystemTagVO t;
INSERT IGNORE INTO ResourceVO (uuid,resourceType) SELECT t.resourceUuid , t.resourceType FROM UserTagVO t;
UPDATE JsonLabelVO SET resourceUuid=UUID();
UPDATE JsonLabelVO SET resourceUuid=REPLACE(resourceUuid,'-','7');
INSERT IGNORE INTO ResourceVO (uuid) SELECT t.resourceUuid FROM JsonLabelVO t;


ALTER TABLE AccountResourceRefVO ADD CONSTRAINT fkAccountResourceRefVOResourceVO FOREIGN KEY (resourceUuid) REFERENCES ResourceVO (uuid) ON DELETE CASCADE;
ALTER TABLE SystemTagVO ADD CONSTRAINT fkSystemTagVOResourceVO FOREIGN KEY (resourceUuid) REFERENCES ResourceVO (uuid) ON DELETE CASCADE;
ALTER TABLE UserTagVO ADD CONSTRAINT fkUserTagVOResourceVO FOREIGN KEY (resourceUuid) REFERENCES ResourceVO (uuid) ON DELETE CASCADE;
ALTER TABLE JsonLabelVO ADD CONSTRAINT fkJsonLabelVOResourceVO FOREIGN KEY (resourceUuid) REFERENCES ResourceVO (uuid) ON DELETE CASCADE;

# fkVolumeEOVmInstanceEO was wrongly added
ALTER TABLE VolumeEO DROP FOREIGN KEY fkVolumeEOVmInstanceEO;

DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_VolumeEO;
DROP TRIGGER IF EXISTS trigger_cleanup_for_VolumeEO_hard_delete;
DROP TRIGGER IF EXISTS trigger_cleanup_for_VmInstanceEO_hard_delete;
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_DiskOfferingEO;
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_EipVO;
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_ImageEO;
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_InstanceOfferingEO;
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_IpRangeEO;
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_L3NetworkEO;
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_LoadBalancerListenerVO;
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_LoadBalancerVO;
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_PolicyVO;
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_PortForwardingRuleVO;
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_QuotaVO;
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_SchedulerVO;
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_SecurityGroupVO;
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_UserGroupVO;
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_UserVO;
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_VipVO;
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_VmInstanceEO;
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_VmNicVO;
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_VolumeEO;
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_VolumeSnapshotEO;



INSERT IGNORE INTO ResourceVO (uuid, resourceType) SELECT t.uuid, "DataCenterVO" FROM DataCenterVO t;
INSERT IGNORE INTO ResourceVO (uuid, resourceType) SELECT t.uuid, "EcsImageVO" FROM EcsImageVO t;
INSERT IGNORE INTO ResourceVO (uuid, resourceType) SELECT t.uuid, "EcsSecurityGroupRuleVO" FROM EcsSecurityGroupRuleVO t;
INSERT IGNORE INTO ResourceVO (uuid, resourceType) SELECT t.uuid, "EcsInstanceConsoleProxyVO" FROM EcsInstanceConsoleProxyVO t;
INSERT IGNORE INTO ResourceVO (uuid, resourceType) SELECT t.uuid, "EcsSecurityGroupVO" FROM EcsSecurityGroupVO t;
INSERT IGNORE INTO ResourceVO (uuid, resourceName, resourceType) SELECT t.uuid, t.vRouterName, "VpcVirtualRouterVO" FROM VpcVirtualRouterVO t;
INSERT IGNORE INTO ResourceVO (uuid, resourceName, resourceType) SELECT t.uuid, t.vSwitchName, "EcsVSwitchVO" FROM EcsVSwitchVO t;
INSERT IGNORE INTO ResourceVO (uuid, resourceType) SELECT t.uuid, "EcsVpcVO" FROM EcsVpcVO t;
INSERT IGNORE INTO ResourceVO (uuid, resourceType) SELECT t.uuid, "EcsInstanceVO" FROM EcsInstanceVO t;
INSERT IGNORE INTO ResourceVO (uuid, resourceName, resourceType) SELECT t.uuid, t.name, "HybridAccountVO" FROM HybridAccountVO t;
INSERT IGNORE INTO ResourceVO (uuid, resourceType) SELECT t.uuid, "HybridEipAddressVO" FROM HybridEipAddressVO t;
INSERT IGNORE INTO ResourceVO (uuid, resourceName, resourceType) SELECT t.uuid, t.bucketName, "OssBucketVO" FROM OssBucketVO t;
INSERT IGNORE INTO ResourceVO (uuid, resourceType) SELECT t.uuid, "IdentityZoneVO" FROM IdentityZoneVO t;
INSERT IGNORE INTO ResourceVO (uuid, resourceType) SELECT t.uuid, "ConnectionAccessPointVO" FROM ConnectionAccessPointVO t;
INSERT IGNORE INTO ResourceVO (uuid, resourceName, resourceType) SELECT t.uuid, t.name, "VirtualBorderRouterVO" FROM VirtualBorderRouterVO t;
INSERT IGNORE INTO ResourceVO (uuid, resourceName, resourceType) SELECT t.uuid, t.name, "VirtualRouterInterfaceVO" FROM VirtualRouterInterfaceVO t;
INSERT IGNORE INTO ResourceVO (uuid, resourceType) SELECT t.uuid, "VpcVirtualRouteEntryVO" FROM VpcVirtualRouteEntryVO t;




INSERT IGNORE INTO ResourceVO (uuid, resourceName, resourceType) SELECT t.uuid, t.name, "EipVO" FROM EipVO t;
INSERT IGNORE INTO ResourceVO (uuid, resourceName, resourceType) SELECT t.uuid, t.name, "AccountVO" FROM AccountVO t;
INSERT IGNORE INTO ResourceVO (uuid, resourceName, resourceType) SELECT t.uuid, t.name, "BackupStorageEO" FROM BackupStorageEO t;
INSERT IGNORE INTO ResourceVO (uuid, resourceType) SELECT t.uuid, "CephBackupStorageMonVO" FROM CephBackupStorageMonVO t;
INSERT IGNORE INTO ResourceVO (uuid, resourceType) SELECT t.uuid, "CephPrimaryStorageMonVO" FROM CephPrimaryStorageMonVO t;
INSERT IGNORE INTO ResourceVO (uuid, resourceType) SELECT t.uuid, "CephPrimaryStoragePoolVO" FROM CephPrimaryStoragePoolVO t;
INSERT IGNORE INTO ResourceVO (uuid, resourceName, resourceType) SELECT t.uuid, t.name, "ClusterEO" FROM ClusterEO t;
INSERT IGNORE INTO ResourceVO (uuid, resourceType) SELECT t.uuid, "ConsoleProxyVO" FROM ConsoleProxyVO t;
INSERT IGNORE INTO ResourceVO (uuid, resourceName, resourceType) SELECT t.uuid, t.name, "DiskOfferingEO" FROM DiskOfferingEO t;
INSERT IGNORE INTO ResourceVO (uuid, resourceName, resourceType) SELECT t.uuid, t.name, "EipVO" FROM EipVO t;
INSERT IGNORE INTO ResourceVO (uuid, resourceName, resourceType) SELECT t.uuid, t.name, "GarbageCollectorVO" FROM GarbageCollectorVO t;
INSERT IGNORE INTO ResourceVO (uuid, resourceName, resourceType) SELECT t.uuid, t.name, "HostEO" FROM HostEO t;
INSERT IGNORE INTO ResourceVO (uuid, resourceName, resourceType) SELECT t.uuid, t.name, "ImageEO" FROM ImageEO t;
INSERT IGNORE INTO ResourceVO (uuid, resourceName, resourceType) SELECT t.uuid, t.name, "InstanceOfferingEO" FROM InstanceOfferingEO t;
INSERT IGNORE INTO ResourceVO (uuid, resourceName, resourceType) SELECT t.uuid, t.name, "IpRangeEO" FROM IpRangeEO t;
INSERT IGNORE INTO ResourceVO (uuid, resourceName, resourceType) SELECT t.uuid, t.name, "L2NetworkEO" FROM L2NetworkEO t;
INSERT IGNORE INTO ResourceVO (uuid, resourceName, resourceType) SELECT t.uuid, t.name, "L3NetworkEO" FROM L3NetworkEO t;
INSERT IGNORE INTO ResourceVO (uuid, resourceName, resourceType) SELECT t.uuid, t.name, "LdapServerVO" FROM LdapServerVO t;
INSERT IGNORE INTO ResourceVO (uuid, resourceName, resourceType) SELECT t.uuid, t.name, "LoadBalancerListenerVO" FROM LoadBalancerListenerVO t;
INSERT IGNORE INTO ResourceVO (uuid, resourceName, resourceType) SELECT t.uuid, t.name, "LoadBalancerVO" FROM LoadBalancerVO t;
INSERT IGNORE INTO ResourceVO (uuid, resourceName, resourceType) SELECT t.uuid, t.name, "PolicyVO" FROM PolicyVO t;
INSERT IGNORE INTO ResourceVO (uuid, resourceName, resourceType) SELECT t.uuid, t.name, "PortForwardingRuleVO" FROM PortForwardingRuleVO t;
INSERT IGNORE INTO ResourceVO (uuid, resourceName, resourceType) SELECT t.uuid, t.name, "PrimaryStorageEO" FROM PrimaryStorageEO t;
INSERT IGNORE INTO ResourceVO (uuid, resourceName, resourceType) SELECT t.uuid, t.name, "QuotaVO" FROM QuotaVO t;
INSERT IGNORE INTO ResourceVO (uuid, resourceName, resourceType) SELECT t.uuid, t.jobName, "SchedulerVO" FROM SchedulerVO t;
INSERT IGNORE INTO ResourceVO (uuid, resourceType) SELECT t.uuid, "SecurityGroupRuleVO" FROM SecurityGroupRuleVO t;
INSERT IGNORE INTO ResourceVO (uuid, resourceName, resourceType) SELECT t.uuid, t.name, "SecurityGroupVO" FROM SecurityGroupVO t;
INSERT IGNORE INTO ResourceVO (uuid, resourceName, resourceType) SELECT t.uuid, t.name, "UserGroupVO" FROM UserGroupVO t;
INSERT IGNORE INTO ResourceVO (uuid, resourceName, resourceType) SELECT t.uuid, t.name, "UserVO" FROM UserVO t;
INSERT IGNORE INTO ResourceVO (uuid, resourceName, resourceType) SELECT t.uuid, t.name, "VCenterDatacenterVO" FROM VCenterDatacenterVO t;
INSERT IGNORE INTO ResourceVO (uuid, resourceName, resourceType) SELECT t.uuid, t.name, "VCenterVO" FROM VCenterVO t;
INSERT IGNORE INTO ResourceVO (uuid, resourceName, resourceType) SELECT t.uuid, t.name, "VipVO" FROM VipVO t;
INSERT IGNORE INTO ResourceVO (uuid, resourceName, resourceType) SELECT t.uuid, t.name, "VmInstanceEO" FROM VmInstanceEO t;
INSERT IGNORE INTO ResourceVO (uuid, resourceType) SELECT t.uuid, "VmNicVO" FROM VmNicVO t;
INSERT IGNORE INTO ResourceVO (uuid, resourceName, resourceType) SELECT t.uuid, t.name, "VniRangeVO" FROM VniRangeVO t;
INSERT IGNORE INTO ResourceVO (uuid, resourceName, resourceType) SELECT t.uuid, t.name, "VolumeEO" FROM VolumeEO t;
INSERT IGNORE INTO ResourceVO (uuid, resourceName, resourceType) SELECT t.uuid, t.name, "VolumeSnapshotEO" FROM VolumeSnapshotEO t;
INSERT IGNORE INTO ResourceVO (uuid, resourceType) SELECT t.uuid, "VolumeSnapshotTreeEO" FROM VolumeSnapshotTreeEO t;
INSERT IGNORE INTO ResourceVO (uuid, resourceType) SELECT t.uuid, "VtepVO" FROM VtepVO t;
INSERT IGNORE INTO ResourceVO (uuid, resourceName, resourceType) SELECT t.uuid, t.name, "ZoneEO" FROM ZoneEO t;

# Foreign keys for table VirtualRouterLoadBalancerRefVO

ALTER TABLE `zstack`.`VirtualRouterLoadBalancerRefVO` ADD CONSTRAINT fkVirtualRouterLoadBalancerRefVOVirtualRouterVmVO FOREIGN KEY (virtualRouterVmUuid) REFERENCES VirtualRouterVmVO (uuid) ON DELETE CASCADE;
ALTER TABLE `zstack`.`VirtualRouterLoadBalancerRefVO` ADD CONSTRAINT fkVirtualRouterLoadBalancerRefVOLoadBalancerVO FOREIGN KEY (loadBalancerUuid) REFERENCES LoadBalancerVO (uuid) ON DELETE CASCADE;
ALTER TABLE `zstack`.`VirtualRouterLoadBalancerRefVO` ADD UNIQUE INDEX(`virtualRouterVmUuid`,`loadBalancerUuid`);


UPDATE InstanceOfferingVO SET allocatorStrategy="LeastVmPreferredHostAllocatorStrategy" WHERE allocatorStrategy="Mevoco";

UPDATE VmInstanceVO SET allocatorStrategy="LeastVmPreferredHostAllocatorStrategy" WHERE allocatorStrategy="Mevoco";

CREATE TABLE  `zstack`.`WebhookVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `url` varchar(2048) DEFAULT NULL,
    `type` varchar(255) NOT NULL,
    `opaque` text DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# Foreign keys for table LoadBalancerListenerVO
ALTER TABLE LoadBalancerListenerVO DROP FOREIGN KEY fkLoadBalancerListenerVOLoadBalancerVO;
ALTER TABLE LoadBalancerListenerVO ADD CONSTRAINT fkLoadBalancerListenerVOLoadBalancerVO FOREIGN KEY (loadBalancerUuid) REFERENCES LoadBalancerVO (uuid) ON DELETE RESTRICT ;