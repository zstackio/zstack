CREATE TABLE IF NOT EXISTS `NvmeTargetVO` (
    `name` VARCHAR(256) DEFAULT NULL,
    `uuid` VARCHAR(32) NOT NULL,
    `nqn` VARCHAR(256) NOT NULL,
    `state` VARCHAR(64) DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
)  ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `NvmeLunVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `nvmeTargetUuid` VARCHAR(32) NOT NULL,
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkNvmeLunVONvmeTargetVO` FOREIGN KEY (`nvmeTargetUuid`) REFERENCES NvmeTargetVO (`uuid`)
)  ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `NvmeLunHostRefVO` (
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `hostUuid` varchar(32) NOT NULL,
    `nvmeLunUuid` varchar(32) NOT NULL,
    `hctl` VARCHAR(64) DEFAULT NULL,
    `path` VARCHAR(128) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`id`),
    CONSTRAINT `fkNvmeLunHostRefVONvmeLunVO` FOREIGN KEY (`nvmeLunUuid`) REFERENCES `NvmeLunVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkNvmeLunHostRefVOHostVO` FOREIGN KEY (`hostUuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `ScsiLunVO` RENAME AS LunVO;

# for compatibility
CREATE VIEW `ScsiLunVO` AS SELECT uuid, name, wwid, vendor, model, wwn, serial, type, hctl, path, size, state, source, multipathDeviceUuid, createDate, lastOpDate FROM `LunVO` WHERE source IN ('iSCSI', 'fiberChannel');
INSERT INTO `NvmeLunHostRefVO` (hostUuid, nvmeLunUuid, hctl, path, lastOpDate, createDate) SELECT hostUuid, scsiLunUuid, hctl, path, lastOpDate, createDate FROM `ScsiLunHostRefVO` WHERE scsiLunUuid NOT IN (SELECT uuid FROM `ScsiLunVO`);
DELETE FROM `ScsiLunHostRefVO` WHERE scsiLunUuid not in (SELECT uuid FROM `ScsiLunVO`);

