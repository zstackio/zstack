ALTER TABLE `VolumeSnapshotTreeEO` ADD `rootImageUuid` VARCHAR(32) DEFAULT NULL;
DROP VIEW IF EXISTS `zstack`.`VolumeSnapshotTreeVO`;
CREATE VIEW `zstack`.`VolumeSnapshotTreeVO` AS SELECT uuid, volumeUuid, rootImageUuid, current, status, createDate, lastOpDate FROM `zstack`.`VolumeSnapshotTreeEO` WHERE deleted IS NULL;

CREATE TABLE `VolumeSnapshotReferenceTreeVO` (
    `uuid`          varchar(32) NOT NULL,
    `rootImageUuid` varchar(32) DEFAULT NULL,
    `lastOpDate`    timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate`    timestamp,
    PRIMARY KEY (`uuid`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

ALTER TABLE `VolumeSnapshotReferenceVO` ADD `parentId` bigint(20) DEFAULT NULL;
ALTER TABLE `VolumeSnapshotReferenceVO` ADD `treeUuid` VARCHAR(32) DEFAULT NULL;

ALTER TABLE `VolumeSnapshotReferenceVO` ADD CONSTRAINT `fkVolumeSnapshotReferenceReferenceParentId` FOREIGN KEY (`parentId`) REFERENCES `VolumeSnapshotReferenceVO` (`id`) ON DELETE SET NULL;
ALTER TABLE `VolumeSnapshotReferenceVO` ADD CONSTRAINT `fkVolumeSnapshotReferenceReferenceTreeUuid` FOREIGN KEY (`treeUuid`) REFERENCES `VolumeSnapshotReferenceTreeVO` (`uuid`) ON DELETE SET NULL;

ALTER TABLE `VolumeSnapshotReferenceVO` DROP FOREIGN KEY `fkVolumeSnapshotReferenceSnapshotUuid`;
ALTER TABLE `VolumeSnapshotReferenceVO` DROP FOREIGN KEY `fkVolumeSnapshotReferenceVolumeUuid`;

ALTER TABLE `VolumeSnapshotReferenceVO` ADD INDEX `idxVolumeSnapshotReferenceVOVolumeUuid` (`volumeUuid`);
ALTER TABLE `VolumeSnapshotReferenceVO` ADD INDEX `idxVolumeSnapshotReferenceVOVolumeSnapshotUuid` (`volumeSnapshotUuid`);
ALTER TABLE `VolumeSnapshotReferenceVO` ADD INDEX `idxVolumeSnapshotReferenceVOReferenceUuid` (`referenceUuid`);

DROP PROCEDURE IF EXISTS upgradeVolumeSnapshotRefSystemTags;
DELIMITER $$
CREATE PROCEDURE upgradeVolumeSnapshotRefSystemTags()
BEGIN
    DECLARE refTag VARCHAR(51);
    DECLARE snapshotUuid VARCHAR(32);
    DECLARE snapshotInstallUrl VARCHAR(1024);
    DECLARE volUuid VARCHAR(32);
    DECLARE refVolumeUuid VARCHAR(32);
    DECLARE refVolumeInstallUrl VARCHAR(32);
    DECLARE done INT DEFAULT FALSE;
    DECLARE cur CURSOR FOR SELECT systemTag.tag, systemTag.resourceUuid FROM `zstack`.`SystemTagVO` systemTag where systemTag.tag like 'backingTo::%';
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO refTag, snapshotUuid;
        IF done THEN
            LEAVE read_loop;
        END IF;

        SET refVolumeUuid = SUBSTRING_INDEX(refTag, '::', 2);
        SELECT `volumeUuid`, `primaryStorageInstallPath` INTO volUuid, snapshotInstallUrl FROM `VolumeSnapshotVO` WHERE `uuid` = snapshotUuid;
        SELECT `installPath` INTO refVolumeInstallUrl FROM `VolumeVO` WHERE `uuid` = refVolumeUuid;
        INSERT INTO `VolumeSnapshotReferenceVO` (`volumeUuid`, `volumeSnapshotUuid`, `volumeSnapshotInstallUrl`, `referenceUuid`, `referenceType`, `referenceInstallUrl`, `referenceVolumeUuid`, `lastOpDate`, `createDate`)
        VALUES (volUuid, snapshotUuid, snapshotInstallUrl, refVolumeUuid, 'VolumeVO', refVolumeInstallUrl, refVolumeUuid, NOW(), NOW());

    END LOOP;
    CLOSE cur;
    SELECT CURTIME();
END $$
DELIMITER ;
CALL upgradeVolumeSnapshotRefSystemTags();
