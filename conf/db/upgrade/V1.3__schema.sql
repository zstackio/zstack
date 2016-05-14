CREATE TABLE  `zstack`.`FusionstorBackupStorageVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `fsid` varchar(64) DEFAULT NULL,
    `poolName` varchar(255) NOT NULL,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`FusionstorBackupStorageMonVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `sshUsername` varchar(64) NOT NULL,
    `sshPassword` varchar(255) NOT NULL,
    `hostname` varchar(255) NOT NULL,
    `status` varchar(255) NOT NULL,
    `sshPort` int unsigned NOT NULL,
    `monPort` int unsigned NOT NULL,
    `backupStorageUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`FusionstorPrimaryStorageVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `fsid` varchar(64) DEFAULT NULL,
    `rootVolumePoolName` varchar(255) NOT NULL,
    `dataVolumePoolName` varchar(255) NOT NULL,
    `imageCachePoolName` varchar(255) NOT NULL,
    `userKey` varchar(255) DEFAULT NULL,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`FusionstorPrimaryStorageMonVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `sshUsername` varchar(64) NOT NULL,
    `sshPassword` varchar(255) NOT NULL,
    `hostname` varchar(255) NOT NULL,
    `status` varchar(255) NOT NULL,
    `sshPort` int unsigned NOT NULL,
    `monPort` int unsigned NOT NULL,
    `primaryStorageUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`FusionstorCapacityVO` (
    `fsid` varchar(64) NOT NULL UNIQUE,
    `totalCapacity` bigint unsigned DEFAULT 0,
    `availableCapacity` bigint unsigned DEFAULT 0,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`fsid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# Foreign keys for table FusionstorBackupStorageMonVO

ALTER TABLE FusionstorBackupStorageMonVO ADD CONSTRAINT fkFusionstorBackupStorageMonVOBackupStorageEO FOREIGN KEY (backupStorageUuid) REFERENCES BackupStorageEO (uuid) ON DELETE CASCADE;

# Foreign keys for table FusionstorBackupStorageVO

ALTER TABLE FusionstorBackupStorageVO ADD CONSTRAINT fkFusionstorBackupStorageVOBackupStorageEO FOREIGN KEY (uuid) REFERENCES BackupStorageEO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE;

# Foreign keys for table FusionstorPrimaryStorageMonVO

ALTER TABLE FusionstorPrimaryStorageMonVO ADD CONSTRAINT fkFusionstorPrimaryStorageMonVOPrimaryStorageEO FOREIGN KEY (primaryStorageUuid) REFERENCES PrimaryStorageEO (uuid) ON DELETE CASCADE;

# Foreign keys for table FusionstorPrimaryStorageVO

ALTER TABLE FusionstorPrimaryStorageVO ADD CONSTRAINT fkFusionstorPrimaryStorageVOPrimaryStorageEO FOREIGN KEY (uuid) REFERENCES PrimaryStorageEO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE;

ALTER TABLE ImageEO ADD actualSize bigint unsigned DEFAULT NULL;
DROP VIEW IF EXISTS `zstack`.`ImageVO`;
CREATE VIEW `zstack`.`ImageVO` AS SELECT uuid, name, description, status, state, size, actualSize, md5Sum, platform, type, format, url, system, mediaType, createDate, lastOpDate, guestOsType FROM `zstack`.`ImageEO` WHERE deleted IS NULL;
UPDATE ImageEO set actualSize = size;

ALTER TABLE VolumeEO ADD actualSize bigint unsigned DEFAULT NULL;
DROP VIEW IF EXISTS `zstack`.`VolumeVO`;
CREATE VIEW `zstack`.`VolumeVO` AS SELECT uuid, name, description, primaryStorageUuid, vmInstanceUuid, diskOfferingUuid, rootImageUuid, installPath, type, status, size, actualSize, deviceId, format, state, createDate, lastOpDate FROM `zstack`.`VolumeEO` WHERE deleted IS NULL;

ALTER TABLE KVMHostVO ADD port int unsigned DEFAULT 22;
ALTER TABLE SftpBackupStorageVO ADD port int unsigned DEFAULT 22;

ALTER TABLE HostCapacityVO ADD cpuNum int unsigned NOT NULL DEFAULT 0;

# Index for table HostCapacityVO
CREATE INDEX idxHostCapacityVOcpuNum ON HostCapacityVO (cpuNum);
