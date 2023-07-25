CREATE TABLE IF NOT EXISTS `zstack`.`RaidLunVO` (
    `uuid` char(32) NOT NULL UNIQUE,
    `controllerUuid` char(32) DEFAULT NULL,
    `diskGroup` smallint DEFAULT NULL,
    `healthState` varchar(32) NOT NULL DEFAULT 'Unknown',
    PRIMARY KEY (`uuid`),
    CONSTRAINT fkRaidLunVORaidControllerVO FOREIGN KEY (controllerUuid) REFERENCES RaidControllerVO (uuid) ON DELETE SET NULL,
    CONSTRAINT fkRaidLunVOLunVO FOREIGN KEY (uuid) REFERENCES LunVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`LocalLunVO` (
    `uuid` char(32) NOT NULL UNIQUE,
    `slotNumber` smallint(6) NOT NULL UNIQUE,
    PRIMARY KEY (`uuid`),
    CONSTRAINT fkLocalLunVOLunVO FOREIGN KEY (uuid) REFERENCES LunVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

RENAME TABLE `NvmeLunHostRefVO` to `LunHostRefVO`;
ALTER TABLE `LunHostRefVO` DROP FOREIGN KEY `fkNvmeLunHostRefVONvmeLunVO`;
ALTER TABLE `LunHostRefVO` CHANGE `nvmeLunUuid` `lunUuid` char(32) NOT NULL;
ALTER TABLE `LunHostRefVO` ADD CONSTRAINT `fkLunHostRefVOLunVO` FOREIGN KEY (lunUuid) REFERENCES LunVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE;
ALTER TABLE `LunHostRefVO` ADD COLUMN `diskUsage` varchar(32) DEFAULT NULL;
ALTER TABLE `LunHostRefVO` ADD COLUMN `locate` varchar(16) DEFAULT 'Remote';

CREATE TABLE IF NOT EXISTS `zstack`.`LunVolumeRefVO` (
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `lunUuid` char(32) NOT NULL,
    `volumeUuid` char(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`id`),
    CONSTRAINT fkLunVolumeRefVOLunVO FOREIGN KEY (lunUuid) REFERENCES LunVO (uuid) ON DELETE CASCADE,
    CONSTRAINT fkLunVolumeRefVOVolumeVO FOREIGN KEY (volumeUuid) REFERENCES VolumeEO (uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `RaidPhysicalDriveVO` ADD COLUMN `lunUuid` char(32) DEFAULT NULL;
ALTER TABLE `RaidPhysicalDriveVO` ADD CONSTRAINT `fkRaidPhysicalDriveVOLunVO` FOREIGN KEY (lunUuid) REFERENCES LunVO (uuid) ON DELETE CASCADE;
