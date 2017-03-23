# VolumeEO
DROP TRIGGER IF EXISTS trigger_clean_AccountResourceRefVO_for_VolumeEO;
DELIMITER $$
CREATE TRIGGER trigger_cleanup_for_VolumeEO_soft_delete AFTER UPDATE ON `VolumeEO`
FOR EACH ROW
    BEGIN
        IF OLD.`deleted` IS NULL AND NEW.`deleted` IS NOT NULL THEN
            DELETE FROM `AccountResourceRefVO` WHERE `resourceUuid` = OLD.`uuid` AND `resourceType` = 'VolumeVO';
            DELETE FROM `SystemTagVO` WHERE `resourceUuid` = OLD.`uuid` AND `resourceType` = 'VolumeVO';
            DELETE FROM `UserTagVO` WHERE `resourceUuid` = OLD.`uuid` AND `resourceType` = 'VolumeVO';
        END IF;
    END$$
DELIMITER ;

DROP TRIGGER IF EXISTS trigger_cleanup_for_VolumeEO_hard_delete;
DELIMITER $$
CREATE TRIGGER trigger_cleanup_for_VolumeEO_hard_delete AFTER DELETE ON `VolumeEO`
FOR EACH ROW
    BEGIN
            DELETE FROM `AccountResourceRefVO` WHERE `resourceUuid` = OLD.`uuid` AND `resourceType` = 'VolumeVO';
            DELETE FROM `SystemTagVO` WHERE `resourceUuid` = OLD.`uuid` AND `resourceType` = 'VolumeVO';
            DELETE FROM `UserTagVO` WHERE `resourceUuid` = OLD.`uuid` AND `resourceType` = 'VolumeVO';
    END $$
DELIMITER ;

# VmInstanceEO
DROP TRIGGER IF EXISTS trigger_cleanup_for_VmInstanceEO_hard_delete;
DELIMITER $$
CREATE TRIGGER trigger_cleanup_for_VmInstanceEO_hard_delete AFTER DELETE ON `VmInstanceEO`
FOR EACH ROW
    BEGIN
            DELETE FROM `AccountResourceRefVO` WHERE `resourceUuid` = OLD.`uuid` AND `resourceType` = 'VmInstanceVO';
            DELETE FROM `SystemTagVO` WHERE `resourceUuid` = OLD.`uuid` AND `resourceType` = 'VmInstanceVO';
            DELETE FROM `UserTagVO` WHERE `resourceUuid` = OLD.`uuid` AND `resourceType` = 'VmInstanceVO';
            DELETE FROM ImageEO WHERE `deleted` IS NOT NULL AND `uuid` NOT IN (SELECT imageUuid FROM VmInstanceEO WHERE imageUuid IS NOT NULL);
    END $$
DELIMITER ;

ALTER TABLE `zstack`.`SharedResourceVO` DROP foreign key fkSharedResourceVOAccountVO;
ALTER TABLE `zstack`.`SharedResourceVO` DROP INDEX `ownerAccountUuid`;
ALTER TABLE `zstack`.`SharedResourceVO` ADD CONSTRAINT fkSharedResourceVOAccountVO FOREIGN KEY (ownerAccountUuid) REFERENCES AccountVO (uuid) ON DELETE CASCADE;
ALTER TABLE `zstack`.`SharedResourceVO` ADD UNIQUE INDEX(`ownerAccountUuid`,`receiverAccountUuid`,`resourceUuid`,`toPublic`);


CREATE TABLE  `zstack`.`CephPrimaryStoragePoolVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `primaryStorageUuid` varchar(32) NOT NULL,
    `poolName` varchar(255) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DELETE FROM GarbageCollectorVO;
ALTER TABLE GarbageCollectorVO ADD name varchar(1024) NOT NULL;
ALTER TABLE GarbageCollectorVO MODIFY COLUMN id INT;
ALTER TABLE GarbageCollectorVO DROP PRIMARY KEY;
ALTER TABLE GarbageCollectorVO DROP id;
ALTER TABLE GarbageCollectorVO ADD uuid varchar(32);
UPDATE GarbageCollectorVO SET uuid = REPLACE(UUID(),'-','') WHERE uuid IS NULL;
ALTER TABLE GarbageCollectorVO MODIFY uuid varchar(32) UNIQUE NOT NULL PRIMARY KEY;

ALTER TABLE HostCapacityVO ADD cpuSockets int unsigned NOT NULL;
