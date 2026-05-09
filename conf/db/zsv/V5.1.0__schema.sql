CREATE TABLE IF NOT EXISTS `zstack`.`TpmKeyBackupVO` (
    `uuid` char(32) NOT NULL UNIQUE,
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '1999-12-31 23:59:59',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DELETE FROM `EncryptedResourceKeyRefVO`
       WHERE `resourceUuid` NOT IN (SELECT `uuid` FROM `ResourceVO`);
ALTER TABLE `EncryptedResourceKeyRefVO`
        ADD CONSTRAINT `fkEncryptedResourceKeyRefResourceVO` FOREIGN KEY (`resourceUuid`) REFERENCES `ResourceVO`(`uuid`)
        ON DELETE CASCADE;

-- Volume LUKS encryption flag (API opt-in + EncryptedResourceKeyRefVO binding)

ALTER TABLE `zstack`.`VolumeEO` ADD COLUMN `encrypted` tinyint(1) NOT NULL DEFAULT 0;
ALTER TABLE `zstack`.`VolumeSnapshotEO` ADD COLUMN `encrypted` tinyint(1) NOT NULL DEFAULT 0;

DROP VIEW IF EXISTS `zstack`.`VolumeVO`;
CREATE VIEW `zstack`.`VolumeVO` AS
SELECT uuid, name, description, primaryStorageUuid, vmInstanceUuid, diskOfferingUuid,
       rootImageUuid, installPath, type, status, size, actualSize, deviceId, format, state, createDate, lastOpDate,
       isShareable, volumeQos, lastVmInstanceUuid, lastDetachDate, lastAttachDate, protocol, encrypted
FROM `zstack`.`VolumeEO`
WHERE deleted IS NULL;

DROP VIEW IF EXISTS `zstack`.`VolumeSnapshotVO`;
CREATE VIEW `zstack`.`VolumeSnapshotVO` AS
SELECT uuid, name, description, type, volumeUuid, format, treeUuid, parentUuid,
       primaryStorageUuid, primaryStorageInstallPath, distance, size, latest,
       fullSnapshot, encrypted, volumeType, state, status, createDate, lastOpDate
FROM `zstack`.`VolumeSnapshotEO`
WHERE deleted IS NULL;
