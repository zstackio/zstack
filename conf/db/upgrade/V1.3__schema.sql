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

