SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `IdentityZoneVO`
-- ----------------------------
CREATE TABLE `IdentityZoneVO` (
	  `uuid` varchar(32) NOT NULL,
	  `zoneId` varchar(32) NOT NULL,
	  `dataCenterUuid` varchar(32) NOT NULL,
	  `type` varchar(32) NOT NULL,
	  `zoneName` varchar(128) NOT NULL,
	  `deleted` varchar(1) DEFAULT NULL,
	  `defaultVSwitchUuid` varchar(32) DEFAULT NULL,
	  `description` varchar(1024) DEFAULT NULL,
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;

SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `EcsVSwitchVO`
-- ----------------------------
CREATE TABLE `EcsVSwitchVO` (
	  `uuid` varchar(32) NOT NULL,
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
	  PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;



SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `EcsVpcVO`
-- ----------------------------
CREATE TABLE `EcsVpcVO` (
	  `uuid` varchar(32) NOT NULL,
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
	  PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;

SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `EcsSecurityGroupRuleVO`
-- ----------------------------
CREATE TABLE `EcsSecurityGroupRuleVO` (
	  `uuid` varchar(32) NOT NULL,
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
	  CONSTRAINT `fkEcsSecurityGroupRuleVOEcsSecurityGroupVO` FOREIGN KEY (`ecsSecurityGroupUuid`) REFERENCES `zstack`.`EcsSecurityGroupVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;

SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `EcsSecurityGroupVO`
-- ----------------------------
CREATE TABLE `EcsSecurityGroupVO` (
	  `uuid` varchar(32) NOT NULL,
	  `ecsVpcUuid` varchar(32) NOT NULL,
	  `securityGroupId` varchar(32) NOT NULL,
	  `securityGroupName` varchar(128) NOT NULL,
	  `description` varchar(1024) DEFAULT NULL,
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`uuid`),
	  UNIQUE KEY `ukEcsVpcUuidSecurityGroupId` (`ecsVpcUuid`,`securityGroupId`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;


SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `EcsInstanceConsoleProxyVO`
-- ----------------------------
CREATE TABLE `EcsInstanceConsoleProxyVO` (
	  `uuid` varchar(32) NOT NULL,
	  `ecsInstanceUuid` varchar(32) NOT NULL,
	  `vncUrl` varchar(256) DEFAULT NULL,
	  `vncPassword` varchar(32) DEFAULT NULL,
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`uuid`),
	  CONSTRAINT `fkEcsInstanceConsoleProxyVOEcsInstanceVO` FOREIGN KEY (`ecsInstanceUuid`) REFERENCES `zstack`.`EcsInstanceVO` (`uuid`) ON DELETE CASCADE,
	  UNIQUE KEY `ecsInstanceUuid` (`ecsInstanceUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;

SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `EcsInstanceVO`
-- ----------------------------
CREATE TABLE `EcsInstanceVO` (
	  `uuid` varchar(32) NOT NULL,
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
      CONSTRAINT `fkEcsInstanceVOEcsSecurityGroupVO` FOREIGN KEY (`ecsSecurityGroupUuid`) REFERENCES `EcsSecurityGroupVO` (`uuid`) ON DELETE SET NULL,
      CONSTRAINT `fkEcsInstanceVOEcsVpcVO` FOREIGN KEY (`ecsVpcUuid`) REFERENCES `EcsVpcVO` (`uuid`) ON DELETE SET NULL,
      CONSTRAINT `fkEcsInstanceVOEcsVSwitchVO` FOREIGN KEY (`ecsVSwitchUuid`) REFERENCES `EcsVSwitchVO` (`uuid`) ON DELETE SET NULL,
      CONSTRAINT `fkEcsInstanceVOIdentityZoneVO` FOREIGN KEY (`identityZoneUuid`) REFERENCES `IdentityZoneVO` (`uuid`) ON DELETE CASCADE,
      CONSTRAINT `fkEcsInstanceVOVmInstanceEO` FOREIGN KEY (`localVmInstanceUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;

SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `EcsImageVO`
-- ----------------------------
CREATE TABLE `EcsImageVO` (
	  `uuid` varchar(32) NOT NULL,
	  `localImageUuid` varchar(32) DEFAULT NULL,
	  `ecsImageId` varchar(32) NOT NULL,
	  `dataCenterUuid` varchar(32) DEFAULT NULL,
	  `name` varchar(128) NOT NULL,
	  `ecsImageSize` bigint(20) NOT NULL,
	  `platform` varchar(32) NOT NULL,
	  `type` varchar(32) NOT NULL,
	  `ossMd5Sum` varchar(32) NOT NULL,
	  `format` varchar(32) NOT NULL,
	  `description` varchar(1024) DEFAULT NULL,
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;

SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `HybridEipAddressVO`
-- ----------------------------
CREATE TABLE `HybridEipAddressVO` (
	  `uuid` varchar(32) NOT NULL,
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
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `qcow2Md5Sum` varchar(128) NOT NULL,
  `rawMd5Sum` varchar(128) NOT NULL,
  `ossBucketName` varchar(32) NOT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `rawMd5Sum` (`rawMd5Sum`),
  UNIQUE KEY `ossBucketName` (`ossBucketName`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;

SET FOREIGN_KEY_CHECKS = 0;
-- ----------------------------
--  Table structure for `OssBucketVO`
-- ----------------------------
CREATE TABLE `OssBucketVO` (
	  `uuid` varchar(32) NOT NULL,
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
	  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
	  `ossBucketUuid` varchar(32) NOT NULL,
	  `dataCenterUuid` varchar(32) NOT NULL,
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`id`),
	  CONSTRAINT `fkOssBucketEcsDataCenterRefVOOssBucketVO` FOREIGN KEY (`ossBucketUuid`) REFERENCES `zstack`.`OssBucketVO` (`uuid`) ON DELETE CASCADE,
      CONSTRAINT `fkOssBucketEcsDataCenterRefVODataCenterVO` FOREIGN KEY (`dataCenterUuid`) REFERENCES `zstack`.`DataCenterVO` (`uuid`) ON DELETE CASCADE,
      UNIQUE KEY `dataCenterUuid` (`dataCenterUuid`),
      UNIQUE KEY `ossBucketUuid` (`ossBucketUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;

SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `DataCenterVO`
-- ----------------------------
CREATE TABLE `DataCenterVO` (
	  `uuid` varchar(32) NOT NULL,
	  `deleted` varchar(1) DEFAULT NULL,
	  `regionName` varchar(1024) NOT NULL,
	  `dcType` varchar(32) NOT NULL,
	  `defaultVpcUuid` varchar(32) DEFAULT NULL,
	  `regionId` varchar(32) NOT NULL,
	  `description` varchar(1024) DEFAULT NULL,
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;

SET FOREIGN_KEY_CHECKS = 0;
-- ----------------------------
--  Table structure for `AvailableInstanceTypesVO`
-- ----------------------------
CREATE TABLE `AvailableInstanceTypesVO` (
	  `uuid` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
	  `accountUuid` varchar(32) NOT NULL,
	  `instanceType` varchar(4096) NOT NULL,
	  `diskCategories` varchar(256) NOT NULL,
	  `resourceType` varchar(256) NOT NULL,
	  `izUuid` varchar(32) NOT NULL,
	  `description` varchar(1024) DEFAULT NULL,
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`uuid`),
	  UNIQUE KEY `ukAccountUuidizUuid` (`accountUuid`,`izUuid`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;

SET FOREIGN_KEY_CHECKS = 0;
-- ----------------------------
--  Table structure for `AvailableIdentityZonesVO`
-- ----------------------------
CREATE TABLE `AvailableIdentityZonesVO` (
	  `uuid` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
	  `accountUuid` varchar(32) NOT NULL,
	  `dataCenterUuid` varchar(32) NOT NULL,
	  `type` varchar(32) NOT NULL,
	  `zoneId` varchar(32) NOT NULL,
	  `description` varchar(1024) DEFAULT NULL,
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`uuid`),
	  UNIQUE KEY `ukAccountUuidizUuid` (`accountUuid`,`dataCenterUuid`,`zoneId`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
SET FOREIGN_KEY_CHECKS = 1;

SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `HybridAccountVO`
-- ----------------------------
CREATE TABLE `HybridAccountVO` (
  `uuid` varchar(32) NOT NULL,
  `accountUuid` varchar(32) NOT NULL,
  `userUuid` varchar(32) DEFAULT NULL,
  `type` varchar(32) NOT NULL,
  `akey` varchar(32) NOT NULL,
  `secret` varchar(64) NOT NULL,
  `description` varchar(1024) DEFAULT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `idxHybridAccountVOKey` (`akey`) USING BTREE,
  UNIQUE KEY `accountUuid` (`accountUuid`) USING BTREE,
  KEY `userUuid` (`userUuid`),
  CONSTRAINT `fkHybridAccountVOAccountVO` FOREIGN KEY (`accountUuid`) REFERENCES `zstack`.`AccountVO` (`uuid`) ON DELETE CASCADE,
  CONSTRAINT `fkHybridAccountVOUserVO` FOREIGN KEY (`userUuid`) REFERENCES `zstack`.`UserVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;

CREATE UNIQUE INDEX uniqAccountUuid on  HybridAccountVO(accountUuid);
ALTER TABLE HybridAccountVO drop index accountUuid;

CREATE UNIQUE INDEX uniqUserUuid on  HybridAccountVO(userUuid);
ALTER TABLE HybridAccountVO drop index userUuid;

# Foreign keys for table DataCenterVO

ALTER TABLE DataCenterVO ADD CONSTRAINT fkDataCenterVOEcsVpcVO FOREIGN KEY (defaultVpcUuid) REFERENCES EcsVpcVO (uuid) ON DELETE SET NULL;

# Foreign keys for table EcsImageVO

ALTER TABLE EcsImageVO ADD CONSTRAINT fkEcsImageVOImageEO FOREIGN KEY (localImageUuid) REFERENCES ImageEO (uuid) ON DELETE SET NULL;
ALTER TABLE EcsImageVO ADD CONSTRAINT fkEcsImageVODataCenterVO FOREIGN KEY (dataCenterUuid) REFERENCES DataCenterVO (uuid) ON DELETE SET NULL;

# Foreign keys for table AvailableInstanceTypesVO

ALTER TABLE AvailableInstanceTypesVO ADD CONSTRAINT fkAvailableInstanceTypesVOAccountVO FOREIGN KEY (accountUuid) REFERENCES AccountVO (uuid) ON DELETE CASCADE;
ALTER TABLE AvailableInstanceTypesVO ADD CONSTRAINT fkAvailableInstanceTypesVOIdentityZoneVO FOREIGN KEY (izUuid) REFERENCES IdentityZoneVO (uuid) ON DELETE CASCADE;

# Foreign keys for table AvailableIdentityZonesVO

ALTER TABLE AvailableIdentityZonesVO ADD CONSTRAINT fkAvailableIdentityZonesVOAccountVO FOREIGN KEY (accountUuid) REFERENCES AccountVO (uuid) ON DELETE CASCADE;
ALTER TABLE AvailableIdentityZonesVO ADD CONSTRAINT fkAvailableIdentityZonesVODataCenterVO FOREIGN KEY (dataCenterUuid) REFERENCES DataCenterVO (uuid) ON DELETE CASCADE;

# Foreign keys for table IdentityZoneVO

ALTER TABLE IdentityZoneVO ADD CONSTRAINT fkIdentityZoneVODataCenterVO FOREIGN KEY (dataCenterUuid) REFERENCES DataCenterVO (uuid) ON DELETE CASCADE;
ALTER TABLE IdentityZoneVO ADD CONSTRAINT fkIdentityZoneVOEcsVSwitchVO FOREIGN KEY (defaultVSwitchUuid) REFERENCES EcsVSwitchVO (uuid) ON DELETE SET NULL;

# Foreign keys for table EcsSecurityGroupVO

ALTER TABLE EcsSecurityGroupVO ADD CONSTRAINT fkEcsSecurityGroupVOEcsVpcVO FOREIGN KEY (ecsVpcUuid) REFERENCES EcsVpcVO (uuid) ON DELETE CASCADE;

# Foreign keys for table EcsVpcVO

ALTER TABLE EcsVpcVO ADD CONSTRAINT fkEcsVpcVODataCenterVO FOREIGN KEY (dataCenterUuid) REFERENCES DataCenterVO (uuid) ON DELETE CASCADE;

# Foreign keys for table EcsVSwitchVO

ALTER TABLE EcsVSwitchVO ADD CONSTRAINT fkEcsVSwitchVOEcsVpcVO FOREIGN KEY (ecsVpcUuid) REFERENCES EcsVpcVO (uuid) ON DELETE CASCADE;
ALTER TABLE EcsVSwitchVO ADD CONSTRAINT fkEcsVSwitchVOIdentityZoneVO FOREIGN KEY (identityZoneUuid) REFERENCES IdentityZoneVO (uuid) ON DELETE CASCADE;


# sync EcsVSwitchVO availableIpAddressCount while EcsInstanceVO update
DROP TRIGGER IF EXISTS trigger_decrease_vswitch_for_create_ecs;
DELIMITER $$
CREATE TRIGGER trigger_decrease_vswitch_for_create_ecs after insert ON `EcsInstanceVO`
FOR EACH ROW
BEGIN
update `EcsVSwitchVO` set `availableIpAddressCount`=`availableIpAddressCount`-1 where `uuid`=NEW.`ecsVSwitchUuid`;
END$$
DELIMITER ;

DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_EcsInstanceVO;
DELIMITER $$
CREATE TRIGGER trigger_clean_AccountResourceRefVO_for_EcsInstanceVO AFTER DELETE ON `EcsInstanceVO`
FOR EACH ROW
    BEGIN
        delete from `AccountResourceRefVO` where `resourceUuid`=OLD.`uuid` and `resourceType`='EcsInstanceVO';
        update `EcsVSwitchVO` set `availableIpAddressCount`=`availableIpAddressCount`+1 where `uuid`=OLD.`ecsVSwitchUuid`;
    END$$
DELIMITER ;

DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_IdentityZoneVO;
DELIMITER $$
CREATE TRIGGER trigger_clean_AccountResourceRefVO_for_IdentityZoneVO AFTER DELETE ON `IdentityZoneVO`
FOR EACH ROW
    BEGIN
        delete from `AccountResourceRefVO` where `resourceUuid`=OLD.`uuid` and `resourceType`='IdentityZoneVO';
    END$$
DELIMITER ;

DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_DataCenterVO;
DELIMITER $$
CREATE TRIGGER trigger_clean_AccountResourceRefVO_for_DataCenterVO AFTER DELETE ON `DataCenterVO`
FOR EACH ROW
    BEGIN
        delete from `AccountResourceRefVO` where `resourceUuid`=OLD.`uuid` and `resourceType`='DataCenterVO';
    END$$
DELIMITER ;

DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_EcsVpcVO;
DELIMITER $$
CREATE TRIGGER trigger_clean_AccountResourceRefVO_for_EcsVpcVO AFTER DELETE ON `EcsVpcVO`
FOR EACH ROW
    BEGIN
        delete from `AccountResourceRefVO` where `resourceUuid`=OLD.`uuid` and `resourceType`='EcsVpcVO';
    END$$
DELIMITER ;

DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_EcsVSwitchVO;
DELIMITER $$
CREATE TRIGGER trigger_clean_AccountResourceRefVO_for_EcsVSwitchVO AFTER DELETE ON `EcsVSwitchVO`
FOR EACH ROW
    BEGIN
        delete from `AccountResourceRefVO` where `resourceUuid`=OLD.`uuid` and `resourceType`='EcsVSwitchVO';
    END$$
DELIMITER ;

DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_EcsImageVO;
DELIMITER $$
CREATE TRIGGER trigger_clean_AccountResourceRefVO_for_EcsImageVO AFTER DELETE ON `EcsImageVO`
FOR EACH ROW
    BEGIN
        delete from `AccountResourceRefVO` where `resourceUuid`=OLD.`uuid` and `resourceType`='EcsImageVO';
    END$$
DELIMITER ;

DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_EcsSecurityGroupVO;
DELIMITER $$
CREATE TRIGGER trigger_clean_AccountResourceRefVO_for_EcsSecurityGroupVO AFTER DELETE ON `EcsSecurityGroupVO`
FOR EACH ROW
    BEGIN
        delete from `AccountResourceRefVO` where `resourceUuid`=OLD.`uuid` and `resourceType`='EcsSecurityGroupVO';
    END$$
DELIMITER ;

DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_HybridEipAddressVO;
DELIMITER $$
CREATE TRIGGER trigger_clean_AccountResourceRefVO_for_HybridEipAddressVO AFTER DELETE ON `HybridEipAddressVO`
FOR EACH ROW
    BEGIN
        delete from `AccountResourceRefVO` where `resourceUuid`=OLD.`uuid` and `resourceType`='HybridEipAddressVO';
    END$$
DELIMITER ;

DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_OssBucketVO;
DELIMITER $$
CREATE TRIGGER trigger_clean_AccountResourceRefVO_for_OssBucketVO AFTER DELETE ON `OssBucketVO`
FOR EACH ROW
    BEGIN
        delete from `AccountResourceRefVO` where `resourceUuid`=OLD.`uuid` and `resourceType`='OssBucketVO';
    END$$
DELIMITER ;

DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_EcsInstanceConsoleProxyVO;
DELIMITER $$
CREATE TRIGGER trigger_clean_AccountResourceRefVO_for_EcsInstanceConsoleProxyVO AFTER DELETE ON `EcsInstanceConsoleProxyVO`
FOR EACH ROW
    BEGIN
        delete from `AccountResourceRefVO` where `resourceUuid`=OLD.`uuid` and `resourceType`='EcsInstanceConsoleProxyVO';
    END$$
DELIMITER ;

DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_HybridAccountVO;
DELIMITER $$
CREATE TRIGGER trigger_clean_AccountResourceRefVO_for_HybridAccountVO AFTER DELETE ON `HybridAccountVO`
FOR EACH ROW
    BEGIN
        delete from `AccountResourceRefVO` where `resourceUuid`=OLD.`uuid` and `resourceType`='HybridAccountVO';
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
  PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `zstack`.`VtepVO` (
  `uuid` varchar(32) NOT NULL UNIQUE,
  `hostUuid` varchar(32) NOT NULL,
  `vtepIp` varchar(32) NOT NULL,
  `port` int NOT NULL,
  `clusterUuid` varchar(32) NOT NULL,
  `type` varchar(32) NOT NULL,
  `poolUuid` varchar(32) NOT NULL,
  PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `zstack`.`VxlanNetworkVO` (
  `uuid` varchar(32) NOT NULL UNIQUE,
  `vni` int NOT NULL,
  `poolUuid` varchar(32),
  PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `zstack`.`VniRangeVO` (
  `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'uuid',
  `name` varchar(255) DEFAULT NULL COMMENT 'name',
  `description` varchar(2048) DEFAULT NULL COMMENT 'description',
  `l2NetworkUuid` varchar(32) NOT NULL COMMENT 'l3 network uuid',
  `startVni` INT NOT NULL COMMENT 'start vni',
  `endVni` INT NOT NULL COMMENT 'end vni',
  PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE VxlanNetworkVO ADD CONSTRAINT fkVxlanNetworkVOL2NetworkEO FOREIGN KEY (uuid) REFERENCES L2NetworkEO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE;
ALTER TABLE VxlanNetworkVO ADD CONSTRAINT fkVxlanNetworkVOVxlanNetworkPoolVO FOREIGN KEY (poolUuid) REFERENCES VxlanNetworkPoolVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE;
ALTER TABLE VxlanNetworkPoolVO ADD CONSTRAINT fkVxlanNetworkPoolVOL2NetworkEO FOREIGN KEY (uuid) REFERENCES L2NetworkEO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE;

ALTER TABLE VtepVO ADD CONSTRAINT fkVtepVOHostEO FOREIGN KEY (hostUuid) REFERENCES HostEO (uuid) ON DELETE RESTRICT;
ALTER TABLE VtepVO ADD CONSTRAINT fkVtepVOClusterEO FOREIGN KEY (clusterUuid) REFERENCES ClusterEO (uuid) ON DELETE RESTRICT;

ALTER TABLE VniRangeVO ADD CONSTRAINT fkVniRangeVOL2NetworkEO  FOREIGN KEY (l2NetworkUuid) REFERENCES L2NetworkEO (uuid) ON DELETE CASCADE;

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
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# Foreign keys for table TaskProgressVO
ALTER TABLE TaskProgressVO ADD COLUMN createDate timestamp;
ALTER TABLE TaskProgressVO ADD CONSTRAINT fkTaskProgressVOManagementNodeVO FOREIGN KEY (managementUuid) REFERENCES ManagementNodeVO (uuid) ON DELETE SET NULL;

CREATE TABLE  `zstack`.`TaskStepVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `taskName` varchar(1024) NOT NULL,
    `content` text DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP TABLE ProgressVO;

CREATE TABLE  `zstack`.`NotificationVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
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
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

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

ALTER TABLE VipVO ADD CONSTRAINT fkUsedIpVO FOREIGN KEY (`usedIpUuid`) REFERENCES `UsedIpVO` (`uuid`) ON DELETE CASCADE;

ALTER TABLE VCenterVO ADD port int DEFAULT NULL;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `EcsSecurityGroupRuleVO`
-- ----------------------------
CREATE TABLE `VpcVirtualRouterVO` (
	  `uuid` varchar(32) NOT NULL,
	  `vrId` varchar(32) NOT NULL,
	  `vpcUuid` varchar(32) NOT NULL,
	  `vRouterName` varchar(32) NOT NULL,
	  `description` varchar(1024) NOT NULL,
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`uuid`),
	  CONSTRAINT `fkVpcVirtualRouterVOEcsVpcVO` FOREIGN KEY (`vpcUuid`) REFERENCES `zstack`.`EcsVpcVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;

SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `EcsSecurityGroupRuleVO`
-- ----------------------------
CREATE TABLE `VirtualRouterInterfaceVO` (
	  `uuid` varchar(32) NOT NULL,
	  `routerInterfaceId` varchar(64) NOT NULL,
	  `virtualRouterUuid` varchar(32) NOT NULL,
	  `accessPointUuid` varchar(32) NOT NULL,
	  `vRouterType` varchar(16) NOT NULL,
	  `role` varchar(16) NOT NULL,
	  `spec` varchar(32) NOT NULL,
	  `name` varchar(64) NOT NULL,
	  `status` varchar(32) NOT NULL,
	  `oppositeInterfaceUuid` varchar(32) NOT NULL,
	  `description` varchar(128) NOT NULL,
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`uuid`),
	  CONSTRAINT `fkVirtualRouterInterfaceVOConnectionAccessPointVO` FOREIGN KEY (`accessPointUuid`) REFERENCES `zstack`.`ConnectionAccessPointVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;


SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `EcsSecurityGroupRuleVO`
-- ----------------------------
CREATE TABLE `VpcVirtualRouteEntryVO` (
	  `uuid` varchar(32) NOT NULL,
	  `destinationCidrBlock` varchar(64) NOT NULL,
	  `nextHopUuid` varchar(32) NOT NULL,
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
	  `uuid` varchar(32) NOT NULL,
	  `accessPointId` varchar(64) NOT NULL,
	  `type` varchar(32) NOT NULL,
	  `name` varchar(64) NOT NULL,
	  `dataCenterUuid` varchar(32) NOT NULL,
	  `status` varchar(32) NOT NULL,
	  `hostOperator` varchar(32) NOT NULL,
	  `description` varchar(128) NOT NULL,
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`uuid`),
	  CONSTRAINT `fkConnectionAccessPointVODataCenterVO` FOREIGN KEY (`dataCenterUuid`) REFERENCES `zstack`.`DataCenterVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;

SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
--  Table structure for `EcsSecurityGroupRuleVO`
-- ----------------------------
CREATE TABLE `VirtualBorderRouterVO` (
	  `uuid` varchar(32) NOT NULL,
	  `vrId` varchar(32) NOT NULL,
	  `vlanInterfaceId` varchar(64) NOT NULL,
	  `vRouterName` varchar(64) NOT NULL,
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
	  `description` varchar(128) NOT NULL,
	  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
	  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
	  PRIMARY KEY (`uuid`),
	  CONSTRAINT `fkVirtualBorderRouterVODataCenterVO` FOREIGN KEY (`dataCenterUuid`) REFERENCES `zstack`.`DataCenterVO` (`uuid`) ON DELETE CASCADE,
	  CONSTRAINT `fkVirtualBorderRouterVOConnectionAccessPointVO` FOREIGN KEY (`accessPointUuid`) REFERENCES `zstack`.`ConnectionAccessPointVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

SET FOREIGN_KEY_CHECKS = 1;


DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_VpcVirtualRouteEntryVO;
DELIMITER $$
CREATE TRIGGER trigger_clean_AccountResourceRefVO_for_VpcVirtualRouteEntryVO AFTER DELETE ON `VpcVirtualRouteEntryVO`
FOR EACH ROW
    BEGIN
        delete from `AccountResourceRefVO` where `resourceUuid`=OLD.`uuid` and `resourceType`='VpcVirtualRouteEntryVO';
    END$$
DELIMITER ;

DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_ConnectionAccessPointVO;
DELIMITER $$
CREATE TRIGGER trigger_clean_AccountResourceRefVO_for_ConnectionAccessPointVO AFTER DELETE ON `ConnectionAccessPointVO`
FOR EACH ROW
    BEGIN
        delete from `AccountResourceRefVO` where `resourceUuid`=OLD.`uuid` and `resourceType`='ConnectionAccessPointVO';
    END$$
DELIMITER ;


DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_VirtualRouterInterfaceVO;
DELIMITER $$
CREATE TRIGGER trigger_clean_AccountResourceRefVO_for_VirtualRouterInterfaceVO AFTER DELETE ON `VirtualRouterInterfaceVO`
FOR EACH ROW
    BEGIN
        delete from `AccountResourceRefVO` where `resourceUuid`=OLD.`uuid` and `resourceType`='VirtualRouterInterfaceVO';
    END$$
DELIMITER ;
