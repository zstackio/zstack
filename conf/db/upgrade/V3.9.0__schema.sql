ALTER TABLE JsonLabelVO MODIFY COLUMN labelValue MEDIUMTEXT;

CREATE INDEX idxTaskProgressVOapiId ON TaskProgressVO(apiId);

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE DahoVllVbrRefVO;
DROP TABLE DahoCloudConnectionVO;
DROP TABLE DahoVllsVO;
DROP TABLE DahoConnectionVO;
DROP TABLE DahoDCAccessVO;

SET FOREIGN_KEY_CHECKS = 1;

ALTER TABLE ImageBackupStorageRefVO ADD COLUMN exportMd5Sum VARCHAR(255) DEFAULT NULL;
ALTER TABLE ImageBackupStorageRefVO ADD COLUMN exportUrl VARCHAR(2048) DEFAULT NULL;
UPDATE ImageBackupStorageRefVO ibs, ImageVO i SET ibs.exportMd5Sum = i.exportMd5Sum, ibs.exportUrl = i.exportUrl WHERE ibs.imageUuid = i.uuid;
DROP VIEW IF EXISTS `zstack`.`ImageVO`;
CREATE VIEW `zstack`.`ImageVO` AS SELECT uuid, name, description, status, state, size, actualSize, md5Sum, platform, type, format, url, `system`, mediaType, createDate, lastOpDate, guestOsType FROM `zstack`.`ImageEO` WHERE deleted IS NULL;
ALTER TABLE ImageEO DROP exportMd5Sum, DROP exportUrl;

ALTER TABLE `zstack`.`PolicyRouteRuleSetVO` ADD COLUMN type VARCHAR(64) DEFAULT "User" NOT NULL;
ALTER TABLE `zstack`.`PolicyRouteTableVO` ADD COLUMN type VARCHAR(64) DEFAULT "User" NOT NULL;

ALTER TABLE `zstack`.`VmNicVO` ADD COLUMN `driverType` varchar(64) DEFAULT NULL;

ALTER TABLE `zstack`.`AutoScalingGroupInstanceVO` ADD COLUMN protectionStrategy VARCHAR(128) DEFAULT "Unprotected" NOT NULL;
ALTER TABLE `zstack`.`AutoScalingGroupInstanceVO` MODIFY COLUMN `protectionStrategy` VARCHAR(128) NOT NULL;
CREATE TABLE IF NOT EXISTS `zstack`.`ZBoxVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `name` VARCHAR(256) NOT NULL,
    `description` VARCHAR(2048) DEFAULT NULL,
    `state` VARCHAR(32) NOT NULL,
    `status` VARCHAR(32) NOT NULL,
    `mountPath` VARCHAR(2048) DEFAULT NULL,
    `totalCapacity` BIGINT UNSIGNED,
    `availableCapacity` BIGINT UNSIGNED,
    `busNum` varchar(32) DEFAULT NULL,
    `devNum` varchar(32) DEFAULT NULL,
    `idVendor` varchar(32) DEFAULT NULL,
    `idProduct` varchar(32) DEFAULT NULL,
    `iManufacturer` varchar(1024) DEFAULT NULL,
    `iProduct` varchar(1024) DEFAULT NULL,
    `iSerial` varchar(32) DEFAULT NULL,
    `usbVersion` varchar(32) DEFAULT NULL,
    `lastOpDate` TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` TIMESTAMP,

    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ZBoxLocationRefVO` (
    `id` bigint(20) NOT NULL AUTO_INCREMENT,
    `zboxUuid` VARCHAR(32) NOT NULL,
    `resourceUuid` VARCHAR(32) NOT NULL,
    `resourceType` VARCHAR(256) NOT NULL,

    PRIMARY KEY  (`id`),
    CONSTRAINT `fkZBoxLocationRefVOZBoxVO` FOREIGN KEY (`zboxUuid`) REFERENCES `zstack`.`ZBoxVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
