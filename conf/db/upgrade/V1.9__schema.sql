ALTER TABLE `zstack`.`PriceVO` modify price DOUBLE(9,5) DEFAULT NULL;

ALTER TABLE `VipVO` DROP FOREIGN KEY `fkVipVOL3NetworkEO1`;
ALTER TABLE VipVO ADD CONSTRAINT fkVipVOL3NetworkEO1 FOREIGN KEY (peerL3NetworkUuid) REFERENCES L3NetworkEO (uuid) ON DELETE SET NULL;

CREATE TABLE `zstack`.`VCenterDatacenterVO` (
    `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'vcenter data-center uuid',
    `vCenterUuid` varchar(32) NOT NULL COMMENT 'vcenter uuid',
    `name` varchar(255) NOT NULL COMMENT 'data-center name',
    `morval` varchar(64) NOT NULL COMMENT 'MOR value',
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# Foreign keys for table VCenterDatacenterVO
ALTER TABLE VCenterDatacenterVO ADD CONSTRAINT fkVCenterDatacenterVOVCenterVO FOREIGN KEY (vCenterUuid) REFERENCES VCenterVO (uuid) ON DELETE CASCADE;

-- ----------------------------
--  Table structure for `ProgressVO`
-- ----------------------------
CREATE TABLE `zstack`.`ProgressVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `resourceUuid` varchar(255) NOT NULL,
  `processType` varchar(1024) NOT NULL,
  `progress` varchar(32) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`IPsecConnectionVO` ADD COLUMN `state` varchar(255) NOT NULL DEFAULT "Enabled";
ALTER TABLE `zstack`.`IPsecConnectionVO` ADD COLUMN `status` varchar(255) NOT NULL DEFAULT "Ready";

ALTER TABLE `zstack`.`VolumeEO` ADD COLUMN `isShareable` boolean NOT NULL DEFAULT FALSE;
DROP VIEW IF EXISTS `zstack`.`VolumeVO`;
CREATE VIEW `zstack`.`VolumeVO` AS SELECT uuid, name, description, primaryStorageUuid, vmInstanceUuid, diskOfferingUuid, rootImageUuid, installPath, type, status, size, actualSize, deviceId, format, state, createDate, lastOpDate, isShareable FROM `zstack`.`VolumeEO` WHERE deleted IS NULL;

CREATE TABLE `zstack`.`ShareableVolumeVmInstanceRefVO`(
    `uuid` varchar(32) NOT NULL UNIQUE,
    `volumeUuid` varchar(32) NOT NULL,
    `vmInstanceUuid` varchar(255) NOT NULL,
    `deviceId` int unsigned NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`AsyncRestVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `requestData` text DEFAULT NULL,
    `state` varchar(64) NOT NULL,
    `result` text DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


# DiskOfferingEO
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_DiskOfferingEO;
DELIMITER $$
CREATE TRIGGER trigger_clean_AccountResourceRefVO_for_DiskOfferingEO AFTER UPDATE ON `DiskOfferingEO`
FOR EACH ROW
    BEGIN
        IF OLD.`deleted` IS NULL AND NEW.`deleted` IS NOT NULL THEN
            DELETE FROM `AccountResourceRefVO` WHERE `resourceUuid` = OLD.`uuid` AND `resourceType` = 'DiskOfferingVO';
        END IF;
    END$$
DELIMITER ;

# EipVO
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_EipVO;
DELIMITER $$
CREATE TRIGGER trigger_clean_AccountResourceRefVO_for_EipVO AFTER DELETE ON `EipVO`
FOR EACH ROW
    BEGIN
        DELETE FROM `AccountResourceRefVO` WHERE `resourceUuid` = OLD.`uuid` AND `resourceType` = 'EipVO';
    END$$
DELIMITER ;

# ImageEO
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_ImageEO;
DELIMITER $$
CREATE TRIGGER trigger_clean_AccountResourceRefVO_for_ImageEO AFTER UPDATE ON `ImageEO`
FOR EACH ROW
    BEGIN
        IF OLD.`deleted` IS NULL AND NEW.`deleted` IS NOT NULL THEN
            DELETE FROM `AccountResourceRefVO` WHERE `resourceUuid` = OLD.`uuid` AND `resourceType` = 'ImageVO';
        END IF;
    END$$
DELIMITER ;

# InstanceOfferingEO
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_InstanceOfferingEO;
DELIMITER $$
CREATE TRIGGER trigger_clean_AccountResourceRefVO_for_InstanceOfferingEO AFTER UPDATE ON `InstanceOfferingEO`
FOR EACH ROW
    BEGIN
        IF OLD.`deleted` IS NULL AND NEW.`deleted` IS NOT NULL THEN
            DELETE FROM `AccountResourceRefVO` WHERE `resourceUuid` = OLD.`uuid` AND `resourceType` = 'InstanceOfferingVO';
        END IF;
    END$$
DELIMITER ;

# IpRangeEO
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_IpRangeEO;
DELIMITER $$
CREATE TRIGGER trigger_clean_AccountResourceRefVO_for_IpRangeEO AFTER UPDATE ON `IpRangeEO`
FOR EACH ROW
    BEGIN
        IF OLD.`deleted` IS NULL AND NEW.`deleted` IS NOT NULL THEN
            DELETE FROM `AccountResourceRefVO` WHERE `resourceUuid` = OLD.`uuid` AND `resourceType` = 'IpRangeVO';
        END IF;
    END$$
DELIMITER ;

# L3NetworkEO
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_L3NetworkEO;
DELIMITER $$
CREATE TRIGGER trigger_clean_AccountResourceRefVO_for_L3NetworkEO AFTER UPDATE ON `L3NetworkEO`
FOR EACH ROW
    BEGIN
        IF OLD.`deleted` IS NULL AND NEW.`deleted` IS NOT NULL THEN
            DELETE FROM `AccountResourceRefVO` WHERE `resourceUuid` = OLD.`uuid` AND `resourceType` = 'L3NetworkVO';
        END IF;
    END$$
DELIMITER ;

# LoadBalancerListenerVO
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_LoadBalancerListenerVO;
DELIMITER $$
CREATE TRIGGER trigger_clean_AccountResourceRefVO_for_LoadBalancerListenerVO AFTER DELETE ON `LoadBalancerListenerVO`
FOR EACH ROW
    BEGIN
        DELETE FROM `AccountResourceRefVO` WHERE `resourceUuid` = OLD.`uuid` AND `resourceType` = 'LoadBalancerListenerVO';
    END$$
DELIMITER ;

# LoadBalancerVO
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_LoadBalancerVO;
DELIMITER $$
CREATE TRIGGER trigger_clean_AccountResourceRefVO_for_LoadBalancerVO AFTER DELETE ON `LoadBalancerVO`
FOR EACH ROW
    BEGIN
        DELETE FROM `AccountResourceRefVO` WHERE `resourceUuid` = OLD.`uuid` AND `resourceType` = 'LoadBalancerVO';
    END$$
DELIMITER ;

# PolicyVO
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_PolicyVO;
DELIMITER $$
CREATE TRIGGER trigger_clean_AccountResourceRefVO_for_PolicyVO AFTER DELETE ON `PolicyVO`
FOR EACH ROW
    BEGIN
        DELETE FROM `AccountResourceRefVO` WHERE `resourceUuid` = OLD.`uuid` AND `resourceType` = 'PolicyVO';
    END$$
DELIMITER ;

# PortForwardingRuleVO
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_PortForwardingRuleVO;
DELIMITER $$
CREATE TRIGGER trigger_clean_AccountResourceRefVO_for_PortForwardingRuleVO AFTER DELETE ON `PortForwardingRuleVO`
FOR EACH ROW
    BEGIN
        DELETE FROM `AccountResourceRefVO` WHERE `resourceUuid` = OLD.`uuid` AND `resourceType` = 'PortForwardingRuleVO';
    END$$
DELIMITER ;

# QuotaVO
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_QuotaVO;
DELIMITER $$
CREATE TRIGGER trigger_clean_AccountResourceRefVO_for_QuotaVO AFTER DELETE ON `QuotaVO`
FOR EACH ROW
    BEGIN
        DELETE FROM `AccountResourceRefVO` WHERE `resourceUuid` = OLD.`id` AND `resourceType` = 'QuotaVO';
    END$$
DELIMITER ;

# SchedulerVO
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_SchedulerVO;
DELIMITER $$
CREATE TRIGGER trigger_clean_AccountResourceRefVO_for_SchedulerVO AFTER DELETE ON `SchedulerVO`
FOR EACH ROW
    BEGIN
        DELETE FROM `AccountResourceRefVO` WHERE `resourceUuid` = OLD.`uuid` AND `resourceType` = 'SchedulerVO';
    END$$
DELIMITER ;

# SecurityGroupVO
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_SecurityGroupVO;
DELIMITER $$
CREATE TRIGGER trigger_clean_AccountResourceRefVO_for_SecurityGroupVO AFTER DELETE ON `SecurityGroupVO`
FOR EACH ROW
    BEGIN
        DELETE FROM `AccountResourceRefVO` WHERE `resourceUuid` = OLD.`uuid` AND `resourceType` = 'SecurityGroupVO';
    END$$
DELIMITER ;

# UserGroupVO
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_UserGroupVO;
DELIMITER $$
CREATE TRIGGER trigger_clean_AccountResourceRefVO_for_UserGroupVO AFTER DELETE ON `UserGroupVO`
FOR EACH ROW
    BEGIN
        DELETE FROM `AccountResourceRefVO` WHERE `resourceUuid` = OLD.`uuid` AND `resourceType` = 'UserGroupVO';
    END$$
DELIMITER ;

# UserVO
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_UserVO;
DELIMITER $$
CREATE TRIGGER trigger_clean_AccountResourceRefVO_for_UserVO AFTER DELETE ON `UserVO`
FOR EACH ROW
    BEGIN
        DELETE FROM `AccountResourceRefVO` WHERE `resourceUuid` = OLD.`uuid` AND `resourceType` = 'UserVO';
    END$$
DELIMITER ;

# VipVO
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_VipVO;
DELIMITER $$
CREATE TRIGGER trigger_clean_AccountResourceRefVO_for_VipVO AFTER DELETE ON `VipVO`
FOR EACH ROW
    BEGIN
        DELETE FROM `AccountResourceRefVO` WHERE `resourceUuid` = OLD.`uuid` AND `resourceType` = 'VipVO';
    END$$
DELIMITER ;

# VmInstanceEO
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_VmInstanceEO;
DELIMITER $$
CREATE TRIGGER trigger_clean_AccountResourceRefVO_for_VmInstanceEO AFTER UPDATE ON `VmInstanceEO`
FOR EACH ROW
    BEGIN
        IF OLD.`deleted` IS NULL AND NEW.`deleted` IS NOT NULL THEN
            DELETE FROM `AccountResourceRefVO` WHERE `resourceUuid` = OLD.`uuid` AND `resourceType` = 'VmInstanceVO';
        END IF;
    END$$
DELIMITER ;

# VmNicVO
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_VmNicVO;
DELIMITER $$
CREATE TRIGGER trigger_clean_AccountResourceRefVO_for_VmNicVO AFTER DELETE ON `VmNicVO`
FOR EACH ROW
    BEGIN
        DELETE FROM `AccountResourceRefVO` WHERE `resourceUuid` = OLD.`uuid` AND `resourceType` = 'VmNicVO';
    END$$
DELIMITER ;

# VolumeEO
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_VolumeEO;
DELIMITER $$
CREATE TRIGGER trigger_clean_AccountResourceRefVO_for_VolumeEO AFTER UPDATE ON `VolumeEO`
FOR EACH ROW
    BEGIN
        IF OLD.`deleted` IS NULL AND NEW.`deleted` IS NOT NULL THEN
            DELETE FROM `AccountResourceRefVO` WHERE `resourceUuid` = OLD.`uuid` AND `resourceType` = 'VolumeVO';
        END IF;
    END$$
DELIMITER ;

# VolumeSnapshotEO
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_VolumeSnapshotEO;
DELIMITER $$
CREATE TRIGGER trigger_clean_AccountResourceRefVO_for_VolumeSnapshotEO AFTER UPDATE ON `VolumeSnapshotEO`
FOR EACH ROW
    BEGIN
        IF OLD.`deleted` IS NULL AND NEW.`deleted` IS NOT NULL THEN
            DELETE FROM `AccountResourceRefVO` WHERE `resourceUuid` = OLD.`uuid` AND `resourceType` = 'VolumeSnapshotVO';
        END IF;
    END$$
DELIMITER ;


ALTER TABLE `zstack`.`SharedResourceVO` ADD UNIQUE INDEX(`ownerAccountUuid`,`receiverAccountUuid`,`resourceUuid`);
ALTER TABLE `zstack`.`ShareableVolumeVmInstanceRefVO` ADD UNIQUE INDEX(`volumeUuid`,`vmInstanceUuid`);

# Foreign keys for table ShareableVolumeVmInstanceRefVO

ALTER TABLE ShareableVolumeVmInstanceRefVO ADD CONSTRAINT fkShareableVolumeVmInstanceRefVOVmInstanceEO FOREIGN KEY (vmInstanceUuid) REFERENCES VmInstanceEO (uuid) ON DELETE CASCADE;
ALTER TABLE ShareableVolumeVmInstanceRefVO ADD CONSTRAINT fkShareableVolumeVmInstanceRefVOVolumeEO FOREIGN KEY (volumeUuid) REFERENCES VolumeEO (uuid) ON DELETE CASCADE;


ALTER TABLE `LocalStorageResourceRefVO` DROP INDEX `resourceUuid`;
ALTER TABLE `LocalStorageResourceRefVO` DROP PRIMARY KEY;
ALTER TABLE `LocalStorageResourceRefVO` ADD CONSTRAINT `pkLocalStorageResourceRefVO` PRIMARY KEY (`resourceUuid`,`hostUuid`,`primaryStorageUuid`);
