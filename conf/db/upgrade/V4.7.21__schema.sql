CREATE TABLE IF NOT EXISTS `zstack`.`RaidLunVO` (
    `uuid` char(32) NOT NULL UNIQUE,
    `controllerUuid` char(32) DEFAULT NULL,
    `diskGroup` smallint DEFAULT NULL,
    `healthState` varchar(32) NOT NULL DEFAULT 'Unknown',
    PRIMARY KEY (`uuid`),
    CONSTRAINT fkRaidLunVORaidControllerVO FOREIGN KEY (controllerUuid) REFERENCES RaidControllerVO (uuid) ON DELETE SET NULL,
    CONSTRAINT fkRaidLunVOLunVO FOREIGN KEY (uuid) REFERENCES LunVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
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
AlTER TABLE `RaidControllerVO` ADD COLUMN `raidTool` char(32) DEFAULT 'STORCLI';

CREATE TABLE IF NOT EXISTS `zstack`.`PhysicalDriveVO` (
    `uuid` varchar(32) not null unique,
    `name`  varchar(255) default null,
    `hostUuid` varchar(32) default null,
    `description`  varchar(255) default null,
    `wwn`  varchar(255) default null,
    `serialNumber`  varchar(255) default null,
    `deviceModel`  varchar(255) default null,
    `size`  bigint(20) default null,
    `driveType`  varchar(255) default null,
    `protocol`  varchar(255) default null,
    `mediaType`  varchar(255) default null,
    `createDate` timestamp not null default '0000-00-00 00:00:00',
    `lastOpDate` timestamp not null default '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fkPhysicalDriveVOHostVO FOREIGN KEY (hostUuid) REFERENCES HostEO (uuid) ON DELETE CASCADE,
    PRIMARY KEY (`uuid`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;


DELIMITER $$
CREATE PROCEDURE moveRaidPhysicalDriveToPhysicalDriveVO()
    moveRaidPhysicalDriveToPhysicalDriveVO:BEGIN
        DECLARE field_count INT;

        SELECT COUNT(*) INTO field_count
        FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = 'zstack'
          AND TABLE_NAME = 'RaidPhysicalDriveVO'
          AND COLUMN_NAME = 'wwn';

        IF (field_count = 0) THEN
            LEAVE moveRaidPhysicalDriveToPhysicalDriveVO;
        END IF;

        INSERT INTO PhysicalDriveVO (`uuid`, `name`, `hostUuid`, `description`, `wwn`, `serialNumber`, `deviceModel`, `size`, `driveType`, `protocol`, `mediaType`, `createDate`, `lastOpDate`)
        SELECT r.uuid, r.name, c.hostUuid, r.description, r.wwn, r.serialNumber, r.deviceModel, r.size, r.driveType, 'SCSI', r.mediaType, r.createDate, r.lastOpDate
        FROM RaidPhysicalDriveVO r, RaidControllerVO c
        WHERE c.uuid=r.raidControllerUuid;

        ALTER TABLE `RaidPhysicalDriveVO`
        DROP COLUMN `name`,
        DROP COLUMN `description`,
        DROP COLUMN `wwn`,
        DROP COLUMN `serialNumber`,
        DROP COLUMN `deviceModel`,
        DROP COLUMN `size`,
        DROP COLUMN `driveType`,
        DROP COLUMN `mediaType`,
        DROP COLUMN `createDate`,
        DROP COLUMN `lastOpDate`;

        ALTER TABLE `RaidPhysicalDriveVO` ADD CONSTRAINT fkRaidPhysicalDriveVOPhysicalDriveVO FOREIGN KEY (uuid) REFERENCES PhysicalDriveVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE;
        UPDATE ResourceVO SET resourceType = "PhysicalDriveVO" WHERE resourceType = "RaidPhysicalDriveVO";
    END $$
DELIMITER ;

call moveRaidPhysicalDriveToPhysicalDriveVO();
DROP PROCEDURE IF EXISTS moveRaidPhysicalDriveToPhysicalDriveVO;


CREATE TABLE IF NOT EXISTS `zstack`.`HBADeviceVO` (
    `uuid` varchar(32) not null unique,
    `hostUuid` varchar(32) default null,
    `nodeName`  varchar(255) default null,
    `portName`  varchar(255) default null,
    `maxSupportedSpeed`  varchar(255) default null,
    `speed`  varchar(255) default null,
    `manufacturer`  varchar(255) default null,
    `model`  varchar(255) default null,
    `firmwareVersion`  varchar(255) default null,
    `driverVersion`  varchar(255) default null,
    `createDate` timestamp not null default '0000-00-00 00:00:00',
    `lastOpDate` timestamp not null default '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fkHBADeviceVOHostVO FOREIGN KEY (hostUuid) REFERENCES HostEO (uuid) ON DELETE CASCADE,
    PRIMARY KEY (`uuid`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`LunPhysicalDriveRefVO` (
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `lunUuid` char(32) NOT NULL,
    `physicalDriveUuid` char(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`id`),
    CONSTRAINT fkLunPhysicalDriveRefVOLunVO FOREIGN KEY (lunUuid) REFERENCES LunVO (uuid) ON DELETE CASCADE,
    CONSTRAINT fkLunPhysicalDriveRefVOPhysicalDriveVO FOREIGN KEY (physicalDriveUuid) REFERENCES PhysicalDriveVO (uuid) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;
