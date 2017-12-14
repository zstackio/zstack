CREATE TABLE if not exists  `zstack`.`SurfsBackupStorageVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `fsid` varchar(64) DEFAULT NULL,
    `poolName` varchar(255) NOT NULL,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE if not exists  `zstack`.`SurfsBackupStorageNodeVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `sshUsername` varchar(64) NOT NULL,
    `sshPassword` varchar(255) NOT NULL,
    `hostname` varchar(255) NOT NULL,
    `status` varchar(255) NOT NULL,
    `nodeAddr` varchar(255) NOT NULL,
    `sshPort` int unsigned NOT NULL,
    `nodePort` int unsigned NOT NULL,
    `backupStorageUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE if not exists  `zstack`.`SurfsPrimaryStorageVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `fsid` varchar(64) DEFAULT NULL,
    `rootVolumePoolName` varchar(255) NOT NULL,
    `dataVolumePoolName` varchar(255) NOT NULL,
    `imageCachePoolName` varchar(255) NOT NULL,
    `userKey` varchar(255) DEFAULT NULL,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE if not exists  `zstack`.`SurfsPrimaryStorageNodeVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `sshUsername` varchar(64) NOT NULL,
    `sshPassword` varchar(255) NOT NULL,
    `hostname` varchar(255) NOT NULL,
    `status` varchar(255) NOT NULL,
    `nodeAddr` varchar(255) NOT NULL,
    `sshPort` int unsigned NOT NULL,
    `nodePort` int unsigned NOT NULL,
    `primaryStorageUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE if not exists `zstack`.`SurfsCapacityVO` (
    `fsid` varchar(64) NOT NULL UNIQUE,
    `totalCapacity` bigint unsigned DEFAULT 0,
    `availableCapacity` bigint unsigned DEFAULT 0,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`fsid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE if not exists  `zstack`.`SurfsPoolClassVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `fsid` varchar(64) NOT NULL,
    `clsname` varchar(32) NOT NULL,
    `clsdisplayname` varchar(64) NOT NULL,
    `isrootcls` tinyint(1) NOT NULL DEFAULT 0,
    `isactive` tinyint(1) NOT NULL DEFAULT 0,
    `totalCapacity` bigint unsigned DEFAULT 0,
    `availableCapacity` bigint unsigned DEFAULT 0,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

delimiter //
drop procedure if exists do_surfs_alter //
create procedure do_surfs_alter()
begin
   IF not exists(select CONSTRAINT_NAME from INFORMATION_SCHEMA.KEY_COLUMN_USAGE where CONSTRAINT_NAME='fkSurfsBackupStorageNodeVOBackupStorageEO') then
      ALTER TABLE SurfsBackupStorageNodeVO ADD CONSTRAINT fkSurfsBackupStorageNodeVOBackupStorageEO FOREIGN KEY (backupStorageUuid) REFERENCES BackupStorageEO (uuid) ON DELETE CASCADE;
   END IF;
   IF not exists(select CONSTRAINT_NAME from INFORMATION_SCHEMA.KEY_COLUMN_USAGE where CONSTRAINT_NAME='fkSurfsBackupStorageVOBackupStorageEO') then
      ALTER TABLE SurfsBackupStorageVO ADD CONSTRAINT fkSurfsBackupStorageVOBackupStorageEO FOREIGN KEY (uuid) REFERENCES BackupStorageEO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE;
   END IF;
   IF not exists(select CONSTRAINT_NAME from INFORMATION_SCHEMA.KEY_COLUMN_USAGE where CONSTRAINT_NAME='fkSurfsPrimaryStorageNodeVOPrimaryStorageEO') then
      ALTER TABLE SurfsPrimaryStorageNodeVO ADD CONSTRAINT fkSurfsPrimaryStorageNodeVOPrimaryStorageEO FOREIGN KEY (primaryStorageUuid) REFERENCES PrimaryStorageEO (uuid) ON DELETE CASCADE;
   END IF;
   IF not exists(select CONSTRAINT_NAME from INFORMATION_SCHEMA.KEY_COLUMN_USAGE where CONSTRAINT_NAME='fkSurfsPrimaryStorageVOPrimaryStorageEO') then
      ALTER TABLE SurfsPrimaryStorageVO ADD CONSTRAINT fkSurfsPrimaryStorageVOPrimaryStorageEO FOREIGN KEY (uuid) REFERENCES PrimaryStorageEO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE;
   END IF;
end;//
call  do_surfs_alter() //
drop procedure if exists do_surfs_alter //
delimiter ;

