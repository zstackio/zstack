-- ----------------------------
--  For mini storage
-- ----------------------------

CREATE TABLE `zstack`.`MiniStorageResourceReplicationVO` (
    `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'mini storage replications',
    `type` varchar(32) NOT NULL COMMENT 'resource type',
    `state` varchar(32) NOT NULL COMMENT 'replication state',
    `resourceUuid` varchar(32) NOT NULL COMMENT 'resource uuid',
    `size` BIGINT UNSIGNED DEFAULT 0 COMMENT 'resource size',
    `port` BIGINT UNSIGNED DEFAULT 0 COMMENT 'resource port on host',
    `hostUuid` varchar(32) NOT NULL COMMENT 'host',
    `primaryStorageUuid` varchar(32) NOT NULL COMMENT 'primary storage uuid',
    `networkStatus` varchar(32) DEFAULT NULL COMMENT 'replication network status',
    `diskStatus` varchar(32) DEFAULT NULL COMMENT 'replication disk status',
    `role` varchar(32) DEFAULT NULL COMMENT 'replication role',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkMiniStorageResourceReplicationVOPrimaryStorageEO` FOREIGN KEY (`primaryStorageUuid`) REFERENCES `PrimaryStorageEO` (uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `zstack`.`MiniStorageVO` (
    `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'mini storage',
    `miniStorageType` varchar(32) NOT NULL COMMENT 'type',
    `diskIdentifier` varchar(255) DEFAULT NULL COMMENT 'disk wwid/wwn/etc',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`MiniStorageHostRefVO` (
    `id` BIGINT UNSIGNED NOT NULL,
    `totalCapacity` BIGINT UNSIGNED DEFAULT 0,
    `availableCapacity` BIGINT UNSIGNED DEFAULT 0,
    `totalPhysicalCapacity` BIGINT UNSIGNED DEFAULT 0,
    `availablePhysicalCapacity` BIGINT UNSIGNED DEFAULT 0,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `zstack`.`ImageReplicationGroupVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` VARCHAR(255) NOT NULL,
    `description` VARCHAR(2048) DEFAULT NULL,
    `state` varchar(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `zstack`.`ImageReplicationGroupBackupStorageRefVO` (
    `backupStorageUuid` varchar(32) NOT NULL,
    `replicationGroupUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`backupStorageUuid`),
    CONSTRAINT `fkImageReplicationGroupBackupStorageRefVOBackupStorageEO` FOREIGN KEY (`backupStorageUuid`) REFERENCES `BackupStorageEO` (uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `zstack`.`ImageOpsJournalVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `backupStorageUuid` varchar(32) NOT NULL,
    `imageUuid` varchar(32) NOT NULL,
    `action` varchar(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`id`),
    CONSTRAINT `fkImageOpsJournalVOBackupStorageEO` FOREIGN KEY (`backupStorageUuid`) REFERENCES `BackupStorageEO` (uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `zstack`.`ImageReplicationHistoryVO` (
    `backupStorageUuid` varchar(32) NOT NULL,
    `lastIndex` bigint unsigned NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`backupStorageUuid`),
    CONSTRAINT `fkImageReplicationHistoryVOBackupStorageEO` FOREIGN KEY (`backupStorageUuid`) REFERENCES `BackupStorageEO` (uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
