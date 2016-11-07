ALTER TABLE `LocalStorageHostRefVO` DROP FOREIGN KEY `fkLocalStorageHostRefVOHostEO`;
ALTER TABLE `LocalStorageHostRefVO` DROP INDEX `hostUuid`;
ALTER TABLE `LocalStorageHostRefVO` DROP PRIMARY KEY;
ALTER TABLE `LocalStorageHostRefVO` ADD CONSTRAINT `fkLocalStorageHostRefVOHostEO` FOREIGN KEY (`hostUuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE;
ALTER TABLE `LocalStorageHostRefVO` ADD CONSTRAINT `pkHostUuidPrimaryStorageUuid` PRIMARY KEY (`hostUuid`,`primaryStorageUuid`);

CREATE TABLE `zstack`.`VCenterVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `zoneUuid` varchar(32) NOT NULL,
    `name` varchar(255) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `domainName` varchar(255) NOT NULL,
    `userName` varchar(255) NOT NULL,
    `password` varchar(1024) NOT NULL,
    `https` int unsigned DEFAULT NULL,
    `state` varchar(32) NOT NULL,
    `status` varchar(32) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `zstack`.`VCenterClusterVO` (
    `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'vcenter cluster uuid',
    `vCenterUuid` varchar(32) NOT NULL COMMENT 'vcenter uuid',
    `morval` varchar(64) NOT NULL COMMENT 'MOR value',
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `zstack`.`ESXHostVO` (
    `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'host uuid',
    `vCenterUuid` varchar(32) NOT NULL COMMENT 'vcenter uuid',
    `morval` varchar(128) NOT NULL COMMENT 'MOR value',
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`VCenterBackupStorageVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `vCenterUuid` varchar(32) NOT NULL COMMENT 'vcenter uuid',
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`VCenterPrimaryStorageVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `vCenterUuid` varchar(32) NOT NULL COMMENT 'vcenter uuid',
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# Foreign keys for table ESXHostVO
ALTER TABLE ESXHostVO ADD CONSTRAINT fkESXHostVOHostEO FOREIGN KEY (uuid) REFERENCES HostEO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE;
ALTER TABLE ESXHostVO ADD CONSTRAINT fkESXHostVOVCenterVO FOREIGN KEY (vCenterUuid) REFERENCES VCenterVO (uuid) ON DELETE CASCADE;

# Foreign keys for table VCenterBackupStorageVO
ALTER TABLE VCenterBackupStorageVO ADD CONSTRAINT fkVCenterBackupStorageVOBackupStorageEO FOREIGN KEY (uuid) REFERENCES BackupStorageEO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE;
ALTER TABLE VCenterBackupStorageVO ADD CONSTRAINT fkVCenterBackupStorageVOVCenterVO FOREIGN KEY (vCenterUuid) REFERENCES VCenterVO (uuid) ON DELETE CASCADE;

# Foreign keys for table VCenterClusterVO
ALTER TABLE VCenterClusterVO ADD CONSTRAINT fkVCenterClusterVOClusterEO FOREIGN KEY (uuid) REFERENCES ClusterEO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE;
ALTER TABLE VCenterClusterVO ADD CONSTRAINT fkVCenterClusterVOVCenterVO FOREIGN KEY (vCenterUuid) REFERENCES VCenterVO (uuid) ON DELETE CASCADE;

# Foreign keys for table VCenterPrimaryStorageVO
ALTER TABLE VCenterPrimaryStorageVO ADD CONSTRAINT fkVCenterPrimaryStorageVOPrimaryStorageEO FOREIGN KEY (uuid) REFERENCES PrimaryStorageEO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE;
ALTER TABLE VCenterPrimaryStorageVO ADD CONSTRAINT fkVCenterPrimaryStorageVOVCenterVO FOREIGN KEY (vCenterUuid) REFERENCES VCenterVO (uuid) ON DELETE CASCADE;

# Foreign keys for table VCenterVO
ALTER TABLE ApplianceVmVO ADD agentPort int unsigned DEFAULT 7759;
ALTER TABLE VCenterVO ADD CONSTRAINT fkVCenterVOZoneEO FOREIGN KEY (zoneUuid) REFERENCES ZoneEO (uuid) ON DELETE CASCADE  ;
ALTER TABLE SchedulerVO CHANGE startDate  startTime  timestamp;
ALTER TABLE SchedulerVO CHANGE stopDate  stopTime  timestamp NULL DEFAULT NULL;
UPDATE SchedulerVO SET stopTime = NULL;

CREATE TABLE `zstack`.`IPsecConnectionVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `l3NetworkUuid` varchar(32) NOT NULL,
    `peerAddress` varchar(255) NOT NULL,
    `authMode` varchar(255) NOT NULL,
    `authKey` text NOT NULL,
    `vipUuid` varchar(32) NOT NULL,
    `ikeAuthAlgorithm` varchar(32) NOT NULL,
    `ikeEncryptionAlgorithm` varchar(32) NOT NULL,
    `ikeDhGroup` int unsigned NOT NULL,
    `policyAuthAlgorithm` varchar(32) NOT NULL,
    `policyEncryptionAlgorithm` varchar(32) NOT NULL,
    `pfs` varchar(32) DEFAULT NULL,
    `policyMode` varchar(32) NOT NULL,
    `transformProtocol` varchar(32) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `zstack`.`IPsecPeerCidrVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `cidr` varchar(255) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `connectionUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


