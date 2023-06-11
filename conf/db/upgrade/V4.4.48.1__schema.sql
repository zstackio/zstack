CREATE TABLE IF NOT EXISTS `zstack`.`BlockPrimaryStorageVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `vendorName` varchar(256) NOt NULL,
    `metadata` text DEFAULT NULL,
    `encryptGatewayIp` varchar(64) DEFAULT NULL,
    `encryptGatewayPort` smallint unsigned DEFAULT 8443,
    `encryptGatewayUsername` varchar(256) DEFAULT NULL,
    `encryptGatewayPassword` varchar(256) DEFAULT NULL,
    PRIMARY KEY (`uuid`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`BlockScsiLunVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `target` varchar(256) DEFAULT NULL,
    `name` VARCHAR(256) DEFAULT NULL,
    `id` smallint unsigned NOT NULL,
    `wwn` VARCHAR(256) DEFAULT NULL,
    `type` VARCHAR(128) NOT NULL,
    `size` bigint unsigned NOT NULL,
    `lunMapId` smallint unsigned DEFAULT 0,
    `lunInitSnapshotID` bigint unsigned DEFAULT 0,
    `usedSize` bigint(20) unsigned DEFAULT 0,
    `encryptedId` smallint unsigned DEFAULT 0,
    `encryptedWwn` varchar(256) DEFAULT NULL,
    `lunType` varchar(256) NOT NULL,
    `volumeUuid` varchar(32) DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkScsiLunVOVolumeVO` FOREIGN KEY (`volumeUuid`) REFERENCES `zstack`.`VolumeEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`BlockScsiLunVO` MODIFY COLUMN id smallint unsigned DEFAULT 0;
ALTER TABLE `zstack`.`BlockScsiLunVO` MODIFY COLUMN type VARCHAR(128) DEFAULT NULL;
ALTER TABLE `zstack`.`BlockScsiLunVO` MODIFY COLUMN lunType varchar(256) DEFAULT NULL;

CREATE TABLE IF NOT EXISTS `zstack`.`HostInitiatorRefVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `hostUuid` varchar(32) NOT NULL UNIQUE,
    `initiatorName` varchar(256) NOT NULL,
    `metadata` text DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkHostInitiatorRefVOHostVo` FOREIGN KEY (`hostUuid`) REFERENCES `zstack`.`HostEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`BlockPrimaryStorageVO` DROP `encryptGatewayIp`;
ALTER TABLE `zstack`.`BlockPrimaryStorageVO` DROP `encryptGatewayPort`;
ALTER TABLE `zstack`.`BlockPrimaryStorageVO` DROP `encryptGatewayUsername`;
ALTER TABLE `zstack`.`BlockPrimaryStorageVO` DROP `encryptGatewayPassword`;
ALTER TABLE `zstack`.`BlockScsiLunVO` DROP `type`;
ALTER TABLE `zstack`.`BlockScsiLunVO` MODIFY COLUMN `id` int unsigned default 0;
ALTER TABLE `zstack`.`BlockScsiLunVO` MODIFY COLUMN `lunMapId` int unsigned default 0;

CREATE TABLE IF NOT EXISTS `zstack`.`BlockPrimaryStorageHostRefVO` (
    `id` BIGINT UNSIGNED NOT NULL UNIQUE AUTO_INCREMENT,
    `initiatorName` varchar(256) DEFAULT NULL,
    `metadata` text DEFAULT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fkBlockPrimaryStorageHostRefVOPrimaryStorageHostRefVO` FOREIGN KEY (`id`) REFERENCES `zstack`.`PrimaryStorageHostRefVO` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DELIMITER $$
CREATE PROCEDURE checkAllBlockHostInPrimaryHostRef()
    BEGIN
        DECLARE hostUuid VARCHAR(32);
        DECLARE primaryStorageUuid VARCHAR(32);
        DECLARE done INT DEFAULT FALSE;
        DECLARE cur CURSOR FOR SELECT hiref.hostUuid, hiref.primaryStorageUuid FROM HostInitiatorRefVO hiref;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

        OPEN cur;
        read_loop: LOOP
            FETCH cur INTO hostUuid, primaryStorageUuid;
            IF done THEN
                LEAVE read_loop;
            END IF;
            IF (select count(*) from PrimaryStorageHostRefVO pshref where pshref.hostUuid = hostUuid and pshref.primaryStorageUuid = primaryStorageUuid) = 0 THEN
                BEGIN
                    INSERT INTO zstack.PrimaryStorageHostRefVO (`primaryStorageUuid`, `hostUuid`, `status`, `lastOpDate`, `createDate`) values (primaryStorageUuid, hostUuid, 'Disconnected', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());
                END;
            END IF;
        END LOOP;
        CLOSE cur;
        SELECT CURTIME();
    END $$
DELIMITER ;

DELIMITER $$
CREATE PROCEDURE migrateBlockPrimaryHostRef()
    BEGIN
        DECLARE initiatorName VARCHAR(256);
        DECLARE psId BIGINT(20);
        DECLARE metadata text;
        DECLARE psUuid VARCHAR(32);
        DECLARE done INT DEFAULT FALSE;
        DECLARE cur CURSOR FOR SELECT  hostInitiatorRef.initiatorName, hostInitiatorRef.metadata, primaryStorageHostRef.id
             FROM zstack.HostInitiatorRefVO hostInitiatorRef, zstack.PrimaryStorageHostRefVO primaryStorageHostRef
             where hostInitiatorRef.hostUuid = primaryStorageHostRef.hostUuid and hostInitiatorRef.primaryStorageUuid = primaryStorageHostRef.primaryStorageUuid;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done =TRUE;
        OPEN cur;
        read_loop: LOOP
            FETCH cur INTO initiatorName, metadata, psId;
            IF done THEN
                LEAVE read_loop;
            END IF;
            IF (select count(*) from BlockPrimaryStorageHostRefVO bpshref where id = psId) = 0 THEN
                BEGIN
                    INSERT INTO zstack.BlockPrimaryStorageHostRefVO(id, initiatorName, metadata, lastOpDate, createDate) values(psId, initiatorName, metadata, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());
                END;
            END IF;
        END LOOP;
        CLOSE cur;
        SELECT CURTIME();
    END $$
DELIMITER ;

DELIMITER $$
CREATE PROCEDURE checkHostInitiatorRefVO()
    BEGIN
        IF (SELECT count(*) FROM information_schema.columns WHERE table_name = 'HostInitiatorRefVO' AND column_name = 'primaryStorageUuid') != 0 THEN
            call checkAllBlockHostInPrimaryHostRef();
            call migrateBlockPrimaryHostRef();
        END IF;
    END $$
DELIMITER ;
call checkHostInitiatorRefVO();
DROP PROCEDURE IF EXISTS migrateBlockPrimaryHostRef;
DROP PROCEDURE IF EXISTS checkAllBlockHostInPrimaryHostRef;
DROP PROCEDURE IF EXISTS checkHostInitiatorRefVO;
DROP TABLE HostInitiatorRefVO;
