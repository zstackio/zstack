CREATE TABLE IF NOT EXISTS `zstack`.`SharedBlockCapacityVO` (
    `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'shared block uuid',
    `totalCapacity` bigint unsigned NOT NULL DEFAULT 0 COMMENT 'total capacity of shared block in bytes',
    `availableCapacity` bigint unsigned NOT NULL DEFAULT 0 COMMENT 'available capacity of shared block in bytes',
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkSharedBlockCapacityVOSharedBlockVO` FOREIGN KEY (`uuid`) REFERENCES `zstack`.`SharedBlockVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`CdpPolicyEO` ADD COLUMN `dailyRPSinceDay` int unsigned DEFAULT 0;
ALTER TABLE `zstack`.`CdpPolicyEO` ADD COLUMN `expireTime` int unsigned DEFAULT 0;
ALTER TABLE `zstack`.`CdpPolicyEO` ADD COLUMN `fullBackupInterval` int unsigned DEFAULT 1;

DROP VIEW IF EXISTS `zstack`.`CdpPolicyVO`;
CREATE VIEW `zstack`.`CdpPolicyVO` AS SELECT uuid, name, description, retentionTimePerDay, dailyRPSinceDay, expireTime, recoveryPointPerSecond, fullBackupInterval, state, lastOpDate, createDate FROM `zstack`.`CdpPolicyEO` WHERE deleted IS NULL;

ALTER TABLE `zstack`.`CdpTaskVO` ADD COLUMN `maxLatency` bigint(20) unsigned DEFAULT 600000;
ALTER TABLE `zstack`.`CdpTaskVO` ADD COLUMN `lastLatency` bigint(20) unsigned DEFAULT 0;

DROP PROCEDURE IF EXISTS `Alter_SCSI_Table`;
DELIMITER $$
CREATE PROCEDURE Alter_SCSI_Table()
    BEGIN
        IF NOT EXISTS( SELECT NULL
                       FROM INFORMATION_SCHEMA.COLUMNS
                       WHERE table_name = 'ScsiLunHostRefVO'
                             AND table_schema = 'zstack'
                             AND column_name = 'path')  THEN

            ALTER TABLE `zstack`.`ScsiLunHostRefVO`
                ADD COLUMN `hctl` VARCHAR(64) DEFAULT NULL,
                ADD COLUMN `path` VARCHAR(128) DEFAULT NULL;

            UPDATE `zstack`.`ScsiLunHostRefVO` ref
                INNER JOIN `zstack`.`ScsiLunVO` lun ON ref.scsiLunUuid = lun.uuid
            SET ref.path = lun.path, ref.hctl = lun.hctl;

        END IF;
    END $$
DELIMITER ;

CALL Alter_SCSI_Table();
DROP PROCEDURE Alter_SCSI_Table;

DELIMITER $$
CREATE PROCEDURE Update_Vip_Account()
    BEGIN
        DECLARE vipUuid VARCHAR(32);
        DECLARE eipAccountUuid VARCHAR(32);
        DECLARE vipAccountUuid VARCHAR(32);
        DECLARE done INT DEFAULT FALSE;
        DECLARE cur CURSOR FOR SELECT v.uuid, a.accountUuid, b.accountUuid FROM zstack.EipVO e, zstack.VipVO v, zstack.AccountResourceRefVO a, zstack.AccountResourceRefVO b
                                        WHERE e.vipUuid = v.uuid AND a.resourceUuid = e.uuid AND b.resourceUuid = v.uuid AND a.accountUuid != b.accountUuid;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
        OPEN cur;
        read_loop: LOOP
            FETCH cur INTO vipUuid, eipAccountUuid, vipAccountUuid;
            IF done THEN
                LEAVE read_loop;
            END IF;
            UPDATE zstack.AccountResourceRefVO set accountUuid = eipAccountUuid WHERE accountUuid = vipAccountUuid AND resourceUuid = vipUuid;
        END LOOP;
        CLOSE cur;
    END $$
DELIMITER ;

CALL Update_Vip_Account();
DROP PROCEDURE IF EXISTS Update_Vip_Account;

UPDATE `zstack`.`VmInstanceVO` t1, `zstack`.`HostVO` t2 set t1.`architecture` = t2.`architecture` where t1.`type` = 'ApplianceVm' and t1.`hostUuid` = t2.`uuid`;
UPDATE `zstack`.`VmInstanceVO` t1, `zstack`.`HostVO` t2 set t1.`architecture` = t2.`architecture` where t1.`type` = 'ApplianceVm' and t1.`architecture` IS NULL and t1.`lastHostUuid` = t2.`uuid`;

CREATE TABLE `zstack`.`BareMetal2BondingVO` (
    `uuid` varchar(32) NOT NULL,
    `name` varchar(255) NOT NULL,
    `slaves` varchar(255) NOT NULL,
    `opts` varchar(255) DEFAULT NULL,
    `chassisUuid` varchar(32) NOT NULL,
    `mode` varchar(255) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    KEY `fkchassisUuid` (`chassisUuid`),
    CONSTRAINT `fkchassisUuid` FOREIGN KEY (`chassisUuid`) REFERENCES `BareMetal2ChassisVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `zstack`.`BareMetal2BondingNicRefVO` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `nicUuid` varchar(32) NOT NULL,
    `instanceUuid` varchar(32) NOT NULL,
    `bondingUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`id`),
    KEY `fkinstance` (`instanceUuid`),
    KEY `fknic` (`nicUuid`),
    KEY `fkbonding` (`bondingUuid`),
    CONSTRAINT `fkinstance` FOREIGN KEY (`instanceUuid`) REFERENCES `BareMetal2InstanceVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fknic` FOREIGN KEY (`nicUuid`) REFERENCES `VmNicVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkbonding` FOREIGN KEY (`bondingUuid`) REFERENCES `BareMetal2BondingVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`BareMetal2InstanceVO` add column agentVersion varchar(32) DEFAULT NULL;
ALTER TABLE `zstack`.`BareMetal2InstanceVO` add column isLatestAgent tinyint(1) unsigned DEFAULT 0;
ALTER TABLE `zstack`.`BareMetal2ChassisNicVO` add column nicName varchar(255) DEFAULT NULL;

ALTER TABLE `zstack`.`IPsecConnectionVO` ADD COLUMN `ikeVersion` varchar(16) NOT NULL DEFAULT 'ikev1';
ALTER TABLE `zstack`.`IPsecConnectionVO` ADD COLUMN `idType` varchar(16) DEFAULT NULL;
ALTER TABLE `zstack`.`IPsecConnectionVO` ADD COLUMN `remoteId` varchar(128) DEFAULT NULL;
ALTER TABLE `zstack`.`IPsecConnectionVO` ADD COLUMN `localId` varchar(128) DEFAULT NULL;
ALTER TABLE `zstack`.`IPsecConnectionVO` ADD COLUMN `ikeLifeTime` int(10) DEFAULT 0;
ALTER TABLE `zstack`.`IPsecConnectionVO` ADD COLUMN `lifeTime` int(10) DEFAULT 0;

CREATE TABLE IF NOT EXISTS `zstack`.`VirtualRouterSoftwareVersionVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `softwareName` varchar(32) NOT NULL,
    `currentVersion` varchar(32) DEFAULT NULL,
    `latestVersion` varchar(32) DEFAULT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkVirtualRouterSoftwareVersionVOVirtualRouterVmVO` FOREIGN KEY (`uuid`) REFERENCES `VirtualRouterVmVO` (`uuid`) ON UPDATE RESTRICT ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`VmSchedHistoryVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `vmInstanceUuid` char(32) NOT NULL,
    `zoneUuid` char(32) DEFAULT NULL,
    `accountUuid` char(32) NOT NULL,
    `schedType` varchar(32) NOT NULL,
    `success` tinyint(1),
    `lastHostUuid` char(32) DEFAULT NULL,
    `destHostUuid` char(32) DEFAULT NULL,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    INDEX idxVmSchedHistoryVOVmInstanceUuid (vmInstanceUuid),
    INDEX idxVmSchedHistoryVOZoneUuid (zoneUuid),
    INDEX idxVmSchedHistoryVOSchedType (schedType),
    CONSTRAINT fkVmSchedHistoryVOZoneEO FOREIGN KEY (zoneUuid) REFERENCES ZoneEO (uuid) ON DELETE SET NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`VmInstanceDeviceAddressVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `resourceUuid` char(32) NOT NULL,
    `vmInstanceUuid` char(32) NOT NULL,
    `deviceAddress` varchar(128) DEFAULT NULL,
    `metadata` text DEFAULT NULL,
    `metadataClass` varchar(128) DEFAULT NULL,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `fkVmInstanceDeviceAddressVOVmInstanceEO` FOREIGN KEY (`vmInstanceUuid`) REFERENCES VmInstanceEO(`uuid`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`VmInstanceDeviceAddressGroupVO` (
    `uuid` char(32) NOT NULL UNIQUE,
    `resourceUuid` char(32) NOT NULL,
    `vmInstanceUuid` char(32) NOT NULL,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkVmInstanceDeviceAddressGroupVOVmInstanceEO` FOREIGN KEY (`vmInstanceUuid`) REFERENCES VmInstanceEO(`uuid`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`VmInstanceDeviceAddressArchiveVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `resourceUuid` char(32) NOT NULL,
    `vmInstanceUuid` char(32) NOT NULL,
    `addressGroupUuid` char(32) NOT NULL,
    `deviceAddress` varchar(128) DEFAULT NULL,
    `metadata` text DEFAULT NULL,
    `metadataClass` varchar(128) DEFAULT NULL,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `fkVmInstanceDeviceAddressArchiveVOVmInstanceEO` FOREIGN KEY (`vmInstanceUuid`) REFERENCES VmInstanceEO(`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkVmInstanceDeviceAddressArchiveVOVmInstanceDeviceAddressGroupVO` FOREIGN KEY (`addressGroupUuid`) REFERENCES VmInstanceDeviceAddressGroupVO (uuid) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`AiSiNoSecretResourcePoolVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `managementIp` varchar(32) NOT NULL,
    `port` int unsigned NOT NULL,
    `route` varchar(32) NOT NULL,
    `clientID` varchar(32) NOT NULL,
    `clientSecrete` varchar(32) NOT NULL,
    `appId` varchar(8) NOT NULL,
    `keyNumSM2` varchar(8) NOT NULL,
    `keyNumSM4` varchar(8) NOT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT fkAiSiNoSecretResourcePoolVOSecretResourcePoolVO FOREIGN KEY (uuid) REFERENCES SecretResourcePoolVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;