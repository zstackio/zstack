ALTER TABLE `AlarmVO`  ADD COLUMN `type` varchar(32) NOT NULL;
UPDATE `AlarmVO` SET `type` = 'Any';

CREATE TABLE IF NOT EXISTS `V2VConversionCacheVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `conversionHostUuid` varchar(32) NOT NULL,
    `srcVmUrl` varchar(255) NOT NULL,
    `installPath` varchar(255) NOT NULL,
    `deviceId` int unsigned NOT NULL,
    `virtualSize` bigint unsigned NOT NULL,
    `actualSize` bigint unsigned NOT NULL,
    `bootMode` varchar(64) DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE UNIQUE INDEX `type` ON NetworkServiceProviderVO(`type`);
CREATE INDEX idxVmUsageVOaccountUuid ON VmUsageVO(accountUuid, dateInLong);

DROP PROCEDURE IF EXISTS updateClusterHostCpuModelCheckTag;
DELIMITER $$
CREATE PROCEDURE updateClusterHostCpuModelCheckTag()
    BEGIN
        DECLARE clusterUuid VARCHAR(32);
        DECLARE tagUuid VARCHAR(32);
        DECLARE done INT DEFAULT FALSE;
        DECLARE cur CURSOR FOR SELECT uuid FROM ClusterVO;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
        OPEN cur;
        read_loop: LOOP
            FETCH cur INTO clusterUuid;
            IF done THEN
                LEAVE read_loop;
            END IF;

            SET tagUuid = REPLACE(UUID(), '-', '');

            IF (select count(*) from SystemTagVO systemTag where systemTag.type = 'System' and systemTag.tag like '%clusterKVMCpuModel::%') != 0 THEN
            BEGIN
            INSERT INTO zstack.SystemTagVO (`uuid`, `resourceUuid`, `resourceType`, `inherent`, `type`, `tag`, `lastOpDate`, `createDate`)
                    values (tagUuid, clusterUuid, 'ClusterVO', 0, 'System', 'check::cluster::cpu::model::true', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());
            END;
            END IF;
        END LOOP;
        CLOSE cur;
        # work around a bug of mysql : jira.mariadb.org/browse/MDEV-4602
        SELECT CURTIME();
    END $$
DELIMITER ;

CALL updateClusterHostCpuModelCheckTag();
DROP PROCEDURE IF EXISTS updateClusterHostCpuModelCheckTag;

ALTER TABLE `zstack`.`LongJobVO` MODIFY COLUMN `jobData` mediumtext NOT NULL;
ALTER TABLE `zstack`.`LongJobVO` MODIFY COLUMN `jobResult` mediumtext DEFAULT NULL;

CREATE TABLE `AutoScalingGroupVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `name` VARCHAR(256) NOT NULL,
    `scalingResourceType` VARCHAR(256) NOT NULL,
    `removalPolicy` VARCHAR(256) NOT NULL,
    `minResourceSize` int(10) NOT NULL,
    `maxResourceSize` int(10) NOT NULL,
    `state` VARCHAR(256) NOT NUll,
    `defaultCooldown` LONG NOT NULL,
    `description` VARCHAR(256) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AutoScalingTemplateVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `name` VARCHAR(256) NOT NULL,
    `type` VARCHAR(256) NOT NULL,
    `state` VARCHAR(256) NOT NULL,
    `description` VARCHAR(256) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AutoScalingTemplateGroupRefVO` (
    `groupUuid` varchar(32) NOT NULL UNIQUE,
    `templateUuid` varchar(32) NOT NULL,
    `lastOpDate` TIMESTAMP NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` TIMESTAMP NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`groupUuid`),
    CONSTRAINT `fkAutoScalingTemplateGroupRefVOAutoScalingGroupVO` FOREIGN KEY (`groupUuid`) REFERENCES `AutoScalingGroupVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkAutoScalingTemplateGroupRefVOAutoScalingTemplateVO` FOREIGN KEY (`templateUuid`) REFERENCES `AutoScalingTemplateVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AutoScalingVmTemplateVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `vmInstanceName` VARCHAR(256) NOT NULL,
    `vmInstanceDescription` VARCHAR(256) DEFAULT NULL,
    `vmInstanceType` VARCHAR(256) NOT NULL,
    `vmInstanceOfferingUuid` VARCHAR(32) NOT NULL,
    `imageUuid` VARCHAR(32) NOT NULL,
    `l3NetworkUuids` text DEFAULT NULL,
    `rootDiskOfferingUuid` VARCHAR(32) DEFAULT NULL,
    `dataDiskOfferingUuids` text DEFAULT NULL,
    `vmInstanceZoneUuid` VARCHAR(32) DEFAULT NULL,
    `vmInstanceClusterUuid` VARCHAR(32) DEFAULT NULL,
    `hostUuid` VARCHAR(32) DEFAULT NULL,
    `primaryStorageUuidForRootVolume` VARCHAR(32) DEFAULT NULL,
    `defaultL3NetworkUuid` VARCHAR(32) DEFAULT NULL,
    `strategy` VARCHAR(32) DEFAULT NULL,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AutoScalingRuleVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `name` VARCHAR(256) NOT NULL,
    `scalingGroupUuid` VARCHAR(32) NOT NULL,
    `type` VARCHAR(256) NOT NULL,
    `description` VARCHAR(256) DEFAULT NULL,
    `cooldown` LONG DEFAULT NULL,
    `state` VARCHAR(256) NOT NULL,
    `status` VARCHAR(256) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkAutoScalingRuleVOAutoScalingGroupVO` FOREIGN KEY (`scalingGroupUuid`) REFERENCES `AutoScalingGroupVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AutoScalingGroupActivityVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `scalingGroupUuid` VARCHAR(32) NOT NULL,
    `activityAction` VARCHAR(128) NOT NULL,
    `scalingGroupRuleUuid` VARCHAR(32) DEFAULT NULL,
    `name` VARCHAR(256) NOT NULL,
    `cause` VARCHAR(128) NOT NULL,
    `status` VARCHAR(128) NOT NULL,
    `activityActionResultMessage` text DEFAULT NULL,
    `description` VARCHAR(256) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `endDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkAutoScalingGroupActivityVOAutoScalingGroupVO` FOREIGN KEY (`scalingGroupUuid`) REFERENCES `AutoScalingGroupVO` (`uuid`),
    CONSTRAINT `fkAutoScalingGroupActivityVOAutoScalingRuleVO` FOREIGN KEY (`scalingGroupRuleUuid`) REFERENCES `AutoScalingRuleVO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AutoScalingGroupInstanceVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `instanceUuid` VARCHAR(32) NOT NULL UNIQUE,
    `scalingGroupUuid` VARCHAR(32) NOT NULL,
    `templateUuid` VARCHAR(32) DEFAULT NULL,
    `scalingGroupActivityUuid` VARCHAR(32) NOT NULL,
    `status` VARCHAR(64) NOT NULL,
    `healthStatus` VARCHAR(64) NOT NULL,
    `lastOpDate` TIMESTAMP NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` TIMESTAMP NOT NULL DEFAULT '0000-00-00 00:00:00',
    `description` VARCHAR(256) DEFAULT NULL,
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkAutoScalingGroupInstanceVOAutoScalingGroupVO` FOREIGN KEY (`scalingGroupUuid`) REFERENCES `AutoScalingGroupVO` (`uuid`),
    CONSTRAINT `fkAutoScalingGroupInstanceVOAutoScalingTemplateVO` FOREIGN KEY (`templateUuid`) REFERENCES `AutoScalingTemplateVO` (`uuid`) ON DELETE SET NULL,
    CONSTRAINT `fkAutoScalingGroupInstanceVOAutoScalingGroupActivityVO` FOREIGN KEY (`scalingGroupActivityUuid`) REFERENCES `AutoScalingGroupActivityVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AddingNewInstanceRuleVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `AdjustmentType` VARCHAR(256) NOT NULL,
    `adjustmentValue` int(10) NOT NULL,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `RemovalInstanceRuleVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `AdjustmentType` VARCHAR(256) NOT NULL,
    `adjustmentValue` int(10) NOT NULL,
    `removalPolicy` VARCHAR(256) NOT NULL,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AutoScalingRuleTriggerVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `name` VARCHAR(256) NOT NULL,
    `ruleUuid` VARCHAR(32) NOT NULL,
    `type` VARCHAR(256) NOT NULL,
    `description` VARCHAR(256) DEFAULT NULL,
    `state` VARCHAR(256) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkAutoScalingRuleTriggerVOAutoScalingRuleVO` FOREIGN KEY (`ruleUuid`) REFERENCES `AutoScalingRuleVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AutoScalingRuleAlarmTriggerVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `alarmUuid` VARCHAR(32) NOT NULL UNIQUE,
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkAutoScalingRuleInstanceAlarmVO` FOREIGN KEY (`alarmUuid`) REFERENCES `AlarmVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`IpRangeEO` ADD COLUMN `ipVersion` int(10) unsigned DEFAULT 4;
ALTER TABLE `zstack`.`IpRangeEO` ADD COLUMN `addressMode` varchar(64) DEFAULT NULL;
ALTER TABLE `zstack`.`IpRangeEO` ADD COLUMN `prefixLen` int(10) unsigned DEFAULT NULL;
DROP VIEW IF EXISTS `zstack`.`IpRangeVO`;
CREATE VIEW `zstack`.`IpRangeVO` AS SELECT uuid, l3NetworkUuid, name, description, startIp, endIp, netmask, gateway, networkCidr, createDate, lastOpDate, ipVersion, addressMode, prefixLen FROM `zstack`.`IpRangeEO` WHERE deleted IS NULL;

ALTER TABLE `zstack`.`UsedIpVO` ADD COLUMN `ipVersion` int(10) unsigned DEFAULT 4;
ALTER TABLE `zstack`.`UsedIpVO` ADD COLUMN `vmNicUuid` varchar(32) DEFAULT NULL;
ALTER TABLE `zstack`.`UsedIpVO` ADD CONSTRAINT fkUsedIpVOVmNicVO FOREIGN KEY (vmNicUuid) REFERENCES VmNicVO (uuid) ON DELETE SET NULL;


ALTER TABLE `zstack`.`L3NetworkEO` ADD COLUMN `ipVersion` int(10) unsigned DEFAULT 4;
DROP VIEW IF EXISTS `zstack`.`L3NetworkVO`;
CREATE VIEW `zstack`.`L3NetworkVO` AS SELECT uuid, name, description, state, type, zoneUuid, l2NetworkUuid, system, dnsDomain, createDate, lastOpDate, category, ipVersion FROM `zstack`.`L3NetworkEO` WHERE deleted IS NULL;

ALTER TABLE `zstack`.`VmNicVO` ADD COLUMN `ipVersion` int(10) unsigned DEFAULT 4;

ALTER TABLE `zstack`.`SecurityGroupVO` ADD COLUMN `ipVersion` int(10) unsigned DEFAULT 4;
ALTER TABLE `zstack`.`SecurityGroupRuleVO` ADD COLUMN `ipVersion` int(10) unsigned NOT NULL DEFAULT 4;
ALTER TABLE `zstack`.`VipVO` ADD COLUMN `prefixLen` int(10) unsigned DEFAULT NULL;

ALTER TABLE `zstack`.`LongJobVO`  ADD COLUMN `executeTime` int unsigned DEFAULT NULL;
UPDATE `zstack`.`LongJobVO` job SET job.`executeTime` = TIMESTAMPDIFF(SECOND, job.createDate, job.lastOpDate);

CREATE TABLE `ScsiLunVO` (
    `name` VARCHAR(256) DEFAULT NULL,
    `uuid` VARCHAR(32) NOT NULL,
    `wwid` VARCHAR(256) NOT NULL,
    `vendor` VARCHAR(256) DEFAULT NULL,
    `model` VARCHAR(256) DEFAULT NULL,
    `wwn` VARCHAR(256) DEFAULT NULL,
    `serial` VARCHAR(256) DEFAULT NULL,
    `hctl` VARCHAR(64) DEFAULT NULL,
    `type` VARCHAR(128) NOT NULL,
    `path` VARCHAR(128) DEFAULT NULL,
    `source` VARCHAR(128) DEFAULT NULL,
    `size` bigint unsigned NOT NULL,
    `state` VARCHAR(64) DEFAULT NULL,
    `multipathDeviceUuid` VARCHAR(32) DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
)  ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ScsiLunHostRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `hostUuid` varchar(32) NOT NULL,
    `scsiLunUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`id`),
    CONSTRAINT `fkScsiLunHostRefVOScsiLunVO` FOREIGN KEY (`scsiLunUuid`) REFERENCES ScsiLunVO (`uuid`),
    CONSTRAINT `fkScsiLunHostRefVOHostVO` FOREIGN KEY (`hostUuid`) REFERENCES HostEO (`uuid`) ON DELETE CASCADE
)  ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `ScsiLunVmInstanceRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `vmInstanceUuid` varchar(32) NOT NULL,
    `scsiLunUuid` varchar(32) NOT NULL,
    `deviceId` int unsigned DEFAULT NULL,
    `attachMultipath` boolean NOT NULL DEFAULT TRUE,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`id`),
    CONSTRAINT `fkScsiLunVmInstanceRefVOScsiLunVO` FOREIGN KEY (`scsiLunUuid`) REFERENCES ScsiLunVO (`uuid`),
    CONSTRAINT `fkScsiLunVmInstanceRefVOVmInstanceVO` FOREIGN KEY (`vmInstanceUuid`) REFERENCES VmInstanceEO (`uuid`) ON DELETE CASCADE
)  ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `FiberChannelStorageVO` (
    `name` VARCHAR(256) DEFAULT NULL,
    `uuid` VARCHAR(32) NOT NULL,
    `wwnn` VARCHAR(256) NOT NULL,
    `state` VARCHAR(64) DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
)  ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `FiberChannelLunVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `fiberChannelStorageUuid` VARCHAR(32) NOT NULL,
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkFiberChannelLunVOFiberChannelStorageVO` FOREIGN KEY (`fiberChannelStorageUuid`) REFERENCES FiberChannelStorageVO (`uuid`)
)  ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP PROCEDURE IF EXISTS migrateIscsiLunVOToScsiLunVO;
DELIMITER $$
CREATE PROCEDURE migrateIscsiLunVOToScsiLunVO()
    BEGIN
        DECLARE done INT DEFAULT FALSE;
        DECLARE name VARCHAR(256);
        DECLARE uuid VARCHAR(32);
        DECLARE wwid VARCHAR(256);
        DECLARE vendor VARCHAR(256);
        DECLARE model VARCHAR(256);
        DECLARE wwn VARCHAR(256);
        DECLARE serial VARCHAR(256);
        DECLARE hctl VARCHAR(64);
        DECLARE type VARCHAR(128);
        DECLARE path VARCHAR(128);
        DECLARE source VARCHAR(128);
        DECLARE size bigint unsigned;
        DECLARE state VARCHAR(64);
        DECLARE multipathDeviceUuid VARCHAR(32);
        DECLARE lastOpDate timestamp;
        DECLARE createDate timestamp;
        DECLARE cur CURSOR FOR SELECT i.uuid, i.wwid, i.vendor, i.model, i.wwn, i.serial, i.hctl, i.type, i.path, i.size, i.multipathDeviceUuid, i.lastOpDate, i.createDate FROM zstack.IscsiLunVO i;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
        OPEN cur;
        read_loop: LOOP
            FETCH cur INTO uuid, wwid, vendor, model, wwn, serial, hctl, type, path, size, multipathDeviceUuid, lastOpDate, createDate;
            IF done THEN
                LEAVE read_loop;
            END IF;

            set name = concat('iscsi-lun-', wwid);
            set source = 'iSCSI';
            set state = 'Enabled';

            INSERT INTO zstack.ScsiLunVO (name, uuid, wwid, vendor, model, wwn, serial, hctl, type, path, source, state, multipathDeviceUuid, size, lastOpDate, createDate)
            values (name, uuid, wwid, vendor, model, wwn, serial, hctl, type, path, source, state, multipathDeviceUuid, size, lastOpDate, createDate);

        end loop;
        close cur;
        select curtime();
    end $$
DELIMITER ;

call migrateIscsiLunVOToScsiLunVO();
alter table IscsiLunVO drop column wwid, drop vendor, drop model, drop wwn, drop serial, drop hctl, drop type, drop path, drop multipathDeviceUuid, drop size, drop lastOpDate, drop createDate;

update SystemTagVO a, VolumeVO b set a.resourceType='VolumeVO' where a.resourceType='InstanceOfferingVO' and a.resourceUuid=b.uuid;