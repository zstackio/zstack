ALTER TABLE `zstack`.`ConsoleProxyVO` ADD COLUMN `version` varchar(32) DEFAULT NULL;

DELIMITER $$
CREATE PROCEDURE migrateClockTrackSystemTagToGlobalConfig()
BEGIN
    DECLARE vmInstanceUuid VARCHAR(32);
    DECLARE clockTrackTag VARCHAR(32);
    DECLARE clockTrack VARCHAR(32);
    DECLARE ruuid VARCHAR(32);
    DECLARE done INT DEFAULT FALSE;
    DECLARE cur CURSOR FOR SELECT systemTag.tag, systemTag.resourceUuid FROM `zstack`.`SystemTagVO` systemTag
     where `tag` like 'clockTrack::%' and `resourceType`='VmInstanceVO';
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO clockTrackTag, vmInstanceUuid;
        IF done THEN
            LEAVE read_loop;
        END IF;

        SET ruuid = REPLACE(UUID(), '-', '');
        SET clockTrack = SUBSTRING_INDEX(clockTrackTag, '::', -1);
        INSERT INTO zstack.ResourceConfigVO (uuid, name, description, category, value, resourceUuid, resourceType, lastOpDate, createDate)
         values(ruuid, "vm.clock.track", "vm.clock.track", "vm", clockTrack, vmInstanceUuid, "VmInstanceVO", CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());
    END LOOP;
    CLOSE cur;
    SELECT CURTIME();
END $$
DELIMITER ;

call migrateClockTrackSystemTagToGlobalConfig();
DROP PROCEDURE IF EXISTS migrateClockTrackSystemTagToGlobalConfig;

DELETE FROM `zstack`.`SystemTagVO` where `tag` like 'clockTrack::%' and `resourceType`='VmInstanceVO';

ALTER TABLE `zstack`.`SlbVmInstanceVO` DROP FOREIGN KEY `fkSlbVmInstanceVOSlbGroupVO`;
ALTER TABLE `zstack`.`SlbVmInstanceVO` DROP KEY `fkSlbVmInstanceVOSlbGroupVO`;
ALTER TABLE `zstack`.`SlbVmInstanceVO` MODIFY COLUMN `slbGroupUuid` varchar(32) DEFAULT NULL;
ALTER TABLE `zstack`.`SlbVmInstanceVO` ADD CONSTRAINT `fkSlbVmInstanceVOVmInstanceEO` FOREIGN KEY (`uuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE CASCADE;
ALTER TABLE `zstack`.`SlbVmInstanceVO` ADD CONSTRAINT `fkSlbVmInstanceVOSlbGroupVO` FOREIGN KEY (`slbGroupUuid`) REFERENCES `SlbGroupVO` (`uuid`) ON DELETE SET NULL;

create table if not exists `zstack`.`HostAllocatedCpuVO` (
    `id` bigint not null auto_increment,
    `hostUuid` varchar(32) not null,
    `allocatedCPU` smallint not null,
    constraint HostAllocatedCpuVO_pk
    primary key (`id`),
    constraint HostAllocatedCpuVO_UniqueIndex_HostUuid_CPUID
    unique (hostUuid, allocatedCPU),
    constraint HostAllocatedCpuVO_HostEO_uuid_fk
    foreign key (`hostUuid`) references `zstack`.`HostEO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table if not exists `zstack`.`VmInstanceNumaNodeVO` (
    `id` bigint not null auto_increment,
    `vmUuid` varchar(32) not null,
    `vNodeID` int not null,
    `vNodeCPUs` varchar(512) not null,
    `vNodeMemSize` bigint not null,
    `vNodeDistance` varchar(512) not null,
    `pNodeID` int not null,
    constraint VmInstanceNumaNodeVO_pk
    primary key (`id`),
    constraint VmInstanceNumaNodeVO_VmInstanceEO_uuid_fk
    foreign key (`vmUuid`) references `zstack`.`VmInstanceEO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

create table if not exists `zstack`.`HostNumaNodeVO` (
    `id` bigint not null auto_increment,
    `hostUuid` varchar(32) not null,
    `nodeID` int not null,
    `nodeCPUs` varchar(512) not null,
    `nodeMemSize` bigint not null,
    `nodeDistance` varchar(512) not null,
    constraint HostNumaNodeVO_pk
    primary key (`id`),
    constraint HostNumaNodeVO_HostEO_uuid_fk
    foreign key (`hostUuid`) references `zstack`.`HostEO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`PrimaryStorageHostRefVO` ADD UNIQUE INDEX(`primaryStorageUuid`, `hostUuid`);

ALTER TABLE `BareMetal2ChassisVO` ADD COLUMN `provisionType` varchar(32) NOT NULL DEFAULT 'Remote';
ALTER TABLE `BareMetal2InstanceVO` ADD COLUMN `provisionType` varchar(32) NOT NULL DEFAULT 'Remote';
ALTER TABLE `BareMetal2ChassisOfferingVO` ADD COLUMN `provisionType` varchar(32) NOT NULL DEFAULT 'Remote';
ALTER TABLE `BareMetal2ChassisDiskVO` ADD COLUMN `wwn` varchar(128) DEFAULT NULL;

CREATE TABLE IF NOT EXISTS `zstack`.`CCSCertificateVO` (
    `uuid`        varchar(32) NOT NULL UNIQUE,
    `algorithm`   varchar(10) NOT NULL DEFAULT 'SM2',
    `format`      char(3) NOT NULL DEFAULT 'CER',
    `issuerDN`    varchar(64) NOT NULL,
    `subjectDN`   varchar(64) NOT NULL,
    `serNumber`   bigint unsigned NOT NULL,
    `effectiveTime`  bigint unsigned NOT NULL DEFAULT 0,
    `expirationTime` bigint unsigned NOT NULL DEFAULT 0,
    `createDate`  timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate`  timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `subjectDNAndSerNumber` (`subjectDN`, `serNumber`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`CCSCertificateUserRefVO` (
    `id`          bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `userUuid`    char(32) NOT NULL,
    `certificateUuid` char(32) NOT NULL,
    `state`       varchar(10) NOT NULL DEFAULT 'Disabled',
    `createDate`  timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate`  timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `fkCCSCertificateUserRefVOCertificateUuid` FOREIGN KEY (`certificateUuid`) REFERENCES `zstack`.`CCSCertificateVO` (`uuid`) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`SecretResourcePoolVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `zoneUuid` varchar(32) NOT NULL,
    `name` varchar(255) NOT NULL,
    `type` varchar(255) NOT NULL,
    `state` varchar(32) NOT NULL,
    `model` varchar(32) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `createDate`  timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate`  timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY  (`uuid`),
    INDEX `idxSecretResourcePoolVOUuid` (`uuid`),
    CONSTRAINT fkSecretResourcePoolVOZoneEO FOREIGN KEY (zoneUuid) REFERENCES ZoneEO (uuid) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`SecurityMachineVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `zoneUuid` varchar(32) NOT NULL,
    `secretResourcePoolUuid` varchar(32) NOT NULL,
    `name` varchar(255) NOT NULL,
    `state` varchar(32) NOT NULL,
    `status` varchar(32) NOT NULL,
    `model` varchar(32) NOT NULL,
    `type` varchar(32) NOT NULL,
    `managementIp` varchar(255) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `createDate`  timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate`  timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY  (`uuid`),
    INDEX `idxSecurityMachineVOUuid` (`uuid`),
    INDEX `idxSecurityMachineVOSecretResourcePoolUuid` (`secretResourcePoolUuid`),
    CONSTRAINT fkSecurityMachineVOSecretResourcePoolVO FOREIGN KEY (secretResourcePoolUuid) REFERENCES SecretResourcePoolVO (uuid) ON DELETE RESTRICT,
    CONSTRAINT fkSecurityMachineVOZoneEO FOREIGN KEY (zoneUuid) REFERENCES ZoneEO (uuid) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`InfoSecSecretResourcePoolVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `connectionMode` int unsigned NOT NULL DEFAULT 2,
    `autoCheck` boolean NOT NULL DEFAULT TRUE,
    `connectTimeOut` bigint unsigned NOT NULL DEFAULT 30000,
    `checkInterval` bigint unsigned NOT NULL DEFAULT 30000,
    `activatedToken` varchar(32) DEFAULT NULL,
    `protectToken` varchar(32) DEFAULT NULL,
    `hmacToken` varchar(32) DEFAULT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT fkInfoSecSecretResourcePoolVOSecretResourcePoolVO FOREIGN KEY (uuid) REFERENCES SecretResourcePoolVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`InfoSecSecurityMachineVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `port` int unsigned NOT NULL,
    `password` varchar(255) DEFAULT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT fkInfoSecSecurityMachineVOSecurityMachineVO FOREIGN KEY (uuid) REFERENCES SecurityMachineVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;