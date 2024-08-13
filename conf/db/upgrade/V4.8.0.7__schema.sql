CREATE TABLE IF NOT EXISTS `zstack`.`ExternalPrimaryStorageVO`(
    `uuid`            varchar(32)  NOT NULL,
    `identity`        varchar(32)  NOT NULL,
    `config`          varchar(255)  DEFAULT NULL,
    `password`        varchar(255)  DEFAULT NULL,
    `addonInfo`       varchar(2048) DEFAULT NULL,
    `defaultProtocol` varchar(255) NOT NULL,
    PRIMARY KEY (`uuid`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`PrimaryStorageOutputProtocolRefVO`(
    `id`                 bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `primaryStorageUuid` varchar(32)     NOT NULL,
    `outputProtocol`     varchar(255)    NOT NULL,
    `createDate`         timestamp       NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate`         timestamp       NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT `fkPrimaryStorageOutputProtocolRefVOExternalPrimaryStorageVO` FOREIGN KEY (`primaryStorageUuid`) REFERENCES ExternalPrimaryStorageVO (`uuid`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;

ALTER TABLE VolumeEO ADD COLUMN protocol VARCHAR(32) DEFAULT NULL;

DROP VIEW IF EXISTS `zstack`.`VolumeVO`;
CREATE VIEW `zstack`.`VolumeVO` AS SELECT uuid, name, description, primaryStorageUuid, vmInstanceUuid, diskOfferingUuid,
                                          rootImageUuid, installPath, type, status, size, actualSize, deviceId, format, state, createDate, lastOpDate,
                                          isShareable, volumeQos, lastVmInstanceUuid, lastDetachDate, lastAttachDate, protocol FROM `zstack`.`VolumeEO` WHERE deleted IS NULL;

ALTER TABLE VmCdRomVO ADD COLUMN protocol VARCHAR(32) DEFAULT NULL;
