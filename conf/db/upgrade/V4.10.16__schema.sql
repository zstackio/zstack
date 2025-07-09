CREATE TABLE IF NOT EXISTS `zstack`.`PluginDriverVO` (
    `uuid` char(32) NOT NULL UNIQUE,
    `name` varchar(64) NOT NULL,
    `type` varchar(64) NOT NULL,
    `vendor` varchar(64) NOT NULL,
    `features` varchar(1024) NOT NULL,
    `optionTypes` text DEFAULT NULL,
    `license` varchar(1024) DEFAULT NULL,
    `version` varchar(1024) DEFAULT NULL,
    `description` text DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59',
    `deleted` varchar(255) DEFAULT NULL,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`VolumeCbtBackupRecordVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `taskUuid` varchar(32) NOT NULL,
    `volumeUuid` varchar(32) NOT NULL,
    `mode` varchar(255) NOT NULL,
    `target` varchar(2048) NOT NULL,
    `scratchNodeName` varchar(255) NOT NULL,
    `bitmapName` varchar(255) NOT NULL,
    `lastBitmapName` varchar(255),
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ExponBlockVolumeVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `exponStatus` varchar(32) NULL,
    PRIMARY KEY (`uuid`),
    CONSTRAINT fkExponBlockVolumeVOBlockVolumeVO FOREIGN KEY (uuid) REFERENCES BlockVolumeVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`ExternalPrimaryStorageVO` MODIFY COLUMN `config` TEXT DEFAULT NULL;

CREATE TABLE IF NOT EXISTS `zstack`.`ExternalPrimaryStorageHostRefVO` (
    `id`       BIGINT UNSIGNED UNIQUE,
    `hostId`   INT          DEFAULT NULL,
    `protocol` varchar(128) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;

SET @row_number = 0;
INSERT INTO ExternalPrimaryStorageHostRefVO (id, hostId, protocol)
SELECT
    p.id,
    (@row_number := @row_number + 1) as hostId,
    e.defaultProtocol as protocol
FROM PrimaryStorageHostRefVO p LEFT JOIN ExternalPrimaryStorageVO e ON p.primaryStorageUuid = e.uuid
ORDER BY p.id;

-- Feature: Customer Resource Attributes | ZSV-???

CREATE TABLE IF NOT EXISTS `zstack`.`ResourceAttributeKeyVO` (
    `uuid` char(32) NOT NULL UNIQUE,
    `name` varchar(255) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ResourceAttributeValueVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `keyUuid` char(32) NOT NULL,
    `value` varchar(2048) NOT NULL,
    `resourceUuid` char(32) NOT NULL,
    `resourceType` varchar(255) NOT NULL,
    `createDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59',
    PRIMARY KEY (`id`),
    CONSTRAINT fkResourceAttributeValueVOResourceAttributeKeyVO FOREIGN KEY (keyUuid) REFERENCES ResourceAttributeKeyVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT fkResourceAttributeValueVOResourceVO FOREIGN KEY (resourceUuid) REFERENCES ResourceVO (uuid) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ResourceAttributeKeyResourceTypeVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `keyUuid` char(32) NOT NULL,
    `resourceType` varchar(255) NOT NULL,
    `createDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59',
    PRIMARY KEY (`id`),
    CONSTRAINT fkResourceAttributeKeyResourceTypeVOResourceAttributeKeyVO FOREIGN KEY (keyUuid) REFERENCES ResourceAttributeKeyVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT `uqResourceAttributeKeyResourceTypeVO` UNIQUE(`keyUuid`, `resourceType`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
