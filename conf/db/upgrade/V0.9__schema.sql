CREATE TABLE  `zstack`.`CephBackupStorageVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `fsid` varchar(64) DEFAULT NULL,
    `poolName` varchar(255) NOT NULL,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`CephBackupStorageMonVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `sshUsername` varchar(64) NOT NULL,
    `sshPassword` varchar(255) NOT NULL,
    `hostname` varchar(255) NOT NULL,
    `status` varchar(255) NOT NULL,
    `monPort` int unsigned NOT NULL,
    `backupStorageUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`CephPrimaryStorageVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `fsid` varchar(64) DEFAULT NULL,
    `rootVolumePoolName` varchar(64) NOT NULL,
    `dataVolumePoolName` varchar(64) NOT NULL,
    `imageCachePoolName` varchar(64) NOT NULL,
    `snapshotPoolName` varchar(64) NOT NULL,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`CephPrimaryStorageMonVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `sshUsername` varchar(64) NOT NULL,
    `sshPassword` varchar(255) NOT NULL,
    `hostname` varchar(255) NOT NULL,
    `status` varchar(255) NOT NULL,
    `monPort` int unsigned NOT NULL,
    `primaryStorageUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`CephCapacityVO` (
    `fsid` varchar(64) NOT NULL UNIQUE,
    `totalCapacity` bigint unsigned DEFAULT 0,
    `availableCapacity` bigint unsigned DEFAULT 0,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`fsid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
