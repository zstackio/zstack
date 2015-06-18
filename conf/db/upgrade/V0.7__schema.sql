CREATE TABLE  `zstack`.`IscsiFileSystemBackendPrimaryStorageVO` (
    `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'uuid',
    `hostname` varchar(255) NOT NULL UNIQUE,
    `sshUsername` varchar(255) NOT NULL,
    `sshPassword` varchar(255) NOT NULL,
    `filesystemType` varchar(255) NOT NULL,
    `chapUsername` varchar(255) DEFAULT NULL,
    `chapPassword` varchar(255) DEFAULT NULL,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`IscsiIsoVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `imageUuid` varchar(32) NOT NULL,
    `primaryStorageUuid` varchar(32) NOT NULL,
    `target` varchar(128) DEFAULT NULL,
    `hostname` varchar(128) DEFAULT NULL,
    `path` varchar(512) DEFAULT NULL,
    `vmInstanceUuid` varchar(32) DEFAULT NULL,
    `lun` int(10) unsigned,
    `port` int(10) unsigned,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


ALTER TABLE `zstack`.`PrimaryStorageEO` MODIFY `type` varchar(255);
ALTER TABLE `zstack`.`VmInstanceEO` ADD COLUMN `platform` varchar(255) NOT NULL;

ALTER TABLE `zstack`.`PrimaryStorageCapacityVO` ADD COLUMN `totalPhysicalCapacity` bigint unsigned DEFAULT 0;
ALTER TABLE `zstack`.`PrimaryStorageCapacityVO` ADD COLUMN `availablePhysicalCapacity` bigint unsigned DEFAULT 0;

# Foreign keys for table IscsiFileSystemBackendPrimaryStorageVO

ALTER TABLE IscsiFileSystemBackendPrimaryStorageVO ADD CONSTRAINT fkIscsiFileSystemBackendPrimaryStorageVOPrimaryStorageEO FOREIGN KEY (uuid) REFERENCES PrimaryStorageEO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE;

# Foreign keys for table IscsiIsoVO

ALTER TABLE IscsiIsoVO ADD CONSTRAINT fkIscsiIsoVOPrimaryStorageEO FOREIGN KEY (primaryStorageUuid) REFERENCES PrimaryStorageEO (uuid) ON DELETE CASCADE;
ALTER TABLE IscsiIsoVO ADD CONSTRAINT fkIscsiIsoVOVmInstanceEO FOREIGN KEY (vmInstanceUuid) REFERENCES VmInstanceEO (uuid) ON DELETE SET NULL;

DROP VIEW IF EXISTS `zstack`.`VmInstanceVO`;
CREATE VIEW `zstack`.`VmInstanceVO` AS SELECT uuid, name, description, zoneUuid, clusterUuid, imageUuid, hostUuid, internalId, lastHostUuid, instanceOfferingUuid, rootVolumeUuid, defaultL3NetworkUuid, type, hypervisorType, cpuNum, cpuSpeed, memorySize, platform, allocatorStrategy, createDate, lastOpDate, state FROM `zstack`.`VmInstanceEO` WHERE deleted IS NULL;

