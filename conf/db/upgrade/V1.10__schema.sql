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

# VxlanNetwork
CREATE TABLE `zstack`.`VxlanNetworkPoolVO` (
  `uuid` varchar(32) NOT NULL UNIQUE,
  PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `zstack`.`VtepVO` (
  `uuid` varchar(32) NOT NULL UNIQUE,
  `hostUuid` varchar(32) NOT NULL,
  `vtepIp` int NOT NULL,
  `port` int NOT NULL,
  `clusterUuid` varchar(32) NOT NULL,
  `type` varchar(32) NOT NULL,
  `poolUuid` varchar(32) NOT NULL,
  PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `zstack`.`VxlanNetwork` (
  `uuid` varchar(32) NOT NULL UNIQUE,
  `vni` int NOT NULL,
  `poolUuid` varchar(32),
  PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `zstack`.`VniRangeVO` (
  `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'uuid',
  `name` varchar(255) DEFAULT NULL COMMENT 'name',
  `description` varchar(2048) DEFAULT NULL COMMENT 'description',
  `poolUuid` varchar(32) NOT NULL COMMENT 'l3 network uuid',
  `startVni` INT NOT NULL COMMENT 'start vni',
  `endVni` INT NOT NULL COMMENT 'end vni'
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE VxlanNetworkVO ADD CONSTRAINT fkVxlanNetworkVOL2NetworkEO FOREIGN KEY (uuid) REFERENCES L2NetworkEO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE;
ALTER TABLE VxlanNetworkPoolVO ADD CONSTRAINT fkVxlanNetworkPoolVOL2NetworkEO FOREIGN KEY (uuid) REFERENCES L2NetworkEO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE;

ALTER TABLE VtepVO ADD CONSTRAINT fkVtepVOHostEO FOREIGN KEY (hostUuid) REFERENCES HostEO (uuid) ON DELETE RESTRICT;
ALTER TABLE VtepVO ADD CONSTRAINT fkVtepVOClusterEO FOREIGN KEY (clusterUuid) REFERENCES ClusterEO (uuid) ON DELETE RESTRICT;

ALTER TABLE VtepL2NetworkRefVO ADD CONSTRAINT fkVtepNetworkRefVOL2NetworkEO FOREIGN KEY (poolUuid) REFERENCES L2NetworkEO (uuid) ON DELETE CASCADE;
ALTER TABLE VtepL2NetworkRefVO ADD CONSTRAINT fkVtepNetworkRefVOVtepVO FOREIGN KEY (vtepUuid) REFERENCES VtepVO (uuid) ON DELETE CASCADE;

ALTER TABLE VniRangeVO ADD CONSTRAINT fkVniRangeVOL2NetworkEO  FOREIGN KEY (poolUuid) REFERENCES L2NetworkEO (uuid) ON DELETE CASCADE;
