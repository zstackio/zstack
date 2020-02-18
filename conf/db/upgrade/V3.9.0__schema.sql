ALTER TABLE JsonLabelVO MODIFY COLUMN labelValue MEDIUMTEXT;

CREATE INDEX idxTaskProgressVOapiId ON TaskProgressVO(apiId);

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
