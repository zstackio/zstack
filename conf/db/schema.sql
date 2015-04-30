use zstack;

CREATE TABLE  `zstack`.`person` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `name` varchar(255),
    `uuid` varchar(36) NOT NULL,
    `description` varchar(255),
    `age` int(10) unsigned,
    `sex` varchar(40) NOT NULL DEFAULT 'male',
    `marriage` tinyint(1) unsigned NOT NULL,
    `title` varchar(12) NOT NULL,
    `date` datetime NOT NULL,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`LogVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `content` text,
    `type` varchar(32) NOT NULL,
    `level` varchar(32),
    `resourceUuid` varchar(32),
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`ManagementNodeVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `hostName` varchar(255) DEFAULT NULL,
    `port` int unsigned DEFAULT NULL,
    `state` varchar(128) NOT NULL,
    `joinDate` timestamp DEFAULT CURRENT_TIMESTAMP,
    `heartBeat` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`AccountVO` (
    `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'account uuid',
    `name` varchar(128) NOT NULL UNIQUE COMMENT 'account name',
    `password` varchar(255) NOT NULL COMMENT 'password',
    `type` varchar(128) NOT NULL COMMENT 'account type',
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP COMMENT 'last operation date',
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`AccountResourceRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `accountUuid` varchar(32) NOT NULL,
    `ownerAccountUuid` varchar(32) NOT NULL,
    `resourceUuid` varchar(32) NOT NULL,
    `resourceType` varchar(255) NOT NULL,
    `permission` int unsigned NOT NULL,
    `isShared` tinyint(1) unsigned NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`id`),
    CONSTRAINT `fkAccountResourceAcntUuid` FOREIGN KEY (`accountUuid`) REFERENCES `AccountVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkAccountResourceOwnerUuid` FOREIGN KEY (`accountUuid`) REFERENCES `AccountVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`UserVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `accountUuid` varchar(32) NOT NULL,
    `name` varchar(128) NOT NULL,
    `password` varchar(255) DEFAULT NULL,
    `securityKey` varchar(128) DEFAULT NULL,
    `token` varchar(128) DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP COMMENT 'last operation date',
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `uqUserVO` UNIQUE(`accountUuid`, `name`),
    CONSTRAINT `fkUserAccountUuid` FOREIGN KEY (`accountUuid`) REFERENCES `AccountVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`SessionVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `accountUuid` varchar(32) NOT NULL,
    `userUuid` varchar(32) DEFAULT NULL,
    `expiredDate` timestamp NOT NULL,
    `createDate` timestamp,
    CONSTRAINT `fkSessionAccountUuid` FOREIGN KEY (`accountUuid`) REFERENCES `AccountVO` (`uuid`) ON DELETE CASCADE,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`PolicyVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) NOT NULL,
    `description` varchar(255) DEFAULT NULL,
    `accountUuid` varchar(32) NOT NULL,
    `type` varchar(32) NOT NULL,
    `data` text NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP COMMENT 'last operation date',
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkPolicyAccountUuid` FOREIGN KEY (`accountUuid`) REFERENCES `AccountVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`UserPolicyRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `policyUuid` varchar(32) NOT NULL,
    `userUuid` varchar(32) NOT NULL ,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP COMMENT 'last operation date',
    `createDate` timestamp,
    PRIMARY KEY  (`id`),
    CONSTRAINT `fkUpPolicyUuid` FOREIGN KEY (`policyUuid`) REFERENCES `PolicyVO` (`uuid`),
    CONSTRAINT `fkUpUserUuid` FOREIGN KEY (`userUuid`) REFERENCES `UserVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`UserGroupVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) NOT NULL,
    `description` varchar(255) DEFAULT NULL,
    `accountUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP COMMENT 'last operation date',
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkGroupAccountUuid` FOREIGN KEY (`accountUuid`) REFERENCES `AccountVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`UserGroupPolicyRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `policyUuid` varchar(32) NOT NULL,
    `groupUuid` varchar(32) NOT NULL ,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP COMMENT 'last operation date',
    `createDate` timestamp,
    PRIMARY KEY  (`id`),
    CONSTRAINT `fkGpPolicyUuid` FOREIGN KEY (`policyUuid`) REFERENCES `PolicyVO` (`uuid`),
    CONSTRAINT `fkGpGroupUuid` FOREIGN KEY (`groupUuid`) REFERENCES `UserGroupVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`UserGroupUserRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `userUuid` varchar(32) NOT NULL,
    `groupUuid` varchar(32) NOT NULL ,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP COMMENT 'last operation date',
    `createDate` timestamp,
    PRIMARY KEY  (`id`),
    CONSTRAINT `fkUgPolicyUuid` FOREIGN KEY (`userUuid`) REFERENCES `UserVO` (`uuid`),
    CONSTRAINT `fkUgGroupUuid` FOREIGN KEY (`groupUuid`) REFERENCES `UserGroupVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`ZoneEO` (
    `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'Zone uuid',
    `name` varchar(255) NOT NULL COMMENT 'Zone name',
    `type` varchar(255) NOT NULL COMMENT 'Zone type',
    `state` varchar(32) NOT NULL COMMENT 'Zone state',
    `description` varchar(2048) DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP COMMENT 'last operation date',
    `createDate` timestamp,
    `deleted` varchar(255) DEFAULT NULL,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`ClusterEO` (
    `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'cluster uuid',
    `zoneUuid` varchar(32) NOT NULL COMMENT 'zone uuid',
    `name` varchar(255) NOT NULL COMMENT 'cluster name',
    `type` varchar(255) NOT NULL COMMENT 'cluster name',
    `managementNodeId` varchar(128) DEFAULT NULL COMMENT 'management node id',
    `state` varchar(32) NOT NULL COMMENT 'cluster state',
    `hypervisorType` varchar(64) NOT NULL COMMENT 'hypervisor type',
    `description` varchar(2048) DEFAULT NULL COMMENT 'cluster description',
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP COMMENT 'last operation date',
    `createDate` timestamp,
    `deleted` varchar(255) DEFAULT NULL,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`HostEO` (
    `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'host uuid',
    `zoneUuid` varchar(32) NOT NULL COMMENT 'zone uuid',
    `clusterUuid` varchar(32) NOT NULL COMMENT 'cluster uuid',
    `name` varchar(255) NOT NULL COMMENT 'host name',
    `state` varchar(32) NOT NULL COMMENT 'host state',
    `status` varchar(32) NOT NULL COMMENT 'host connection status',
    `hypervisorType` varchar(64) NOT NULL COMMENT 'hypervisor type',
    `managementIp` varchar(255) NOT NULL COMMENT 'ip of managment nic',
    `description` varchar(2048) DEFAULT NULL COMMENT 'host description',
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP COMMENT 'last operation date',
    `createDate` timestamp,
    `deleted` varchar(255) DEFAULT NULL,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`UserTagVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `resourceUuid` varchar(32) NOT NULL,
    `resourceType` varchar(64) NOT NULL,
    `type` varchar(32) NOT NULL,
    `tag` varchar(2048) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`SystemTagVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `resourceUuid` varchar(32) NOT NULL,
    `resourceType` varchar(64) NOT NULL,
    `inherent` tinyint unsigned NOT NULL DEFAULT 0,
    `type` varchar(32) NOT NULL,
    `tag` varchar(2048) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`HostTagVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `hostUuid` varchar(32) NOT NULL COMMENT 'host uuid',
    `tag` varchar(128) NOT NULL COMMENT 'host tag',
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP COMMENT 'last operation date',
    `createDate` timestamp,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`SimulatorHostVO` (
    `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'host uuid',
    `memoryCapacity` bigint unsigned NOT NULL COMMENT 'total memory of host in bytes',
    `cpuCapacity` bigint unsigned NOT NULL COMMENT 'total cpu of host in HZ',
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`KVMHostVO` (
    `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'host uuid',
    `username` varchar(255) NOT NULL,
    `password` varchar(255) NOT NULL,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`PrimaryStorageEO` (
    `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'uuid',
    `zoneUuid` varchar(32) NOT NULL,
    `name` varchar(255) DEFAULT NULL COMMENT 'primary storage name',
    `url` varchar(2048) NOT NULL,
    `mountPath` varchar(2048) NOT NULL,
    `description` varchar(2048) DEFAULT NULL COMMENT 'primary storage description',
    `state` varchar(32) NOT NULL,
    `status` varchar(32) NOT NULL,
    `type` varchar(255) NOT NULL COMMENT 'primary storage type',
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP COMMENT 'last operation date',
    `createDate` timestamp,
    `deleted` varchar(255) DEFAULT NULL,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

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

CREATE TABLE  `zstack`.`PrimaryStorageCapacityVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `totalCapacity` bigint unsigned DEFAULT 0,
    `availableCapacity` bigint unsigned DEFAULT 0,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`PrimaryStorageClusterRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `primaryStorageUuid` varchar(32) NOT NULL COMMENT 'primary storage uuid',
    `clusterUuid` varchar(32) NOT NULL COMMENT 'primary storage uuid',
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP COMMENT 'last operation date',
    `createDate` timestamp,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`BackupStorageEO` (
    `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'uuid',
    `name` varchar(255) DEFAULT NULL COMMENT 'backup storage name',
    `url` varchar(2048) NOT NULL COMMENT 'url, can be ip or fqdn name or type specific string',
    `description` varchar(2048) DEFAULT NULL COMMENT 'backup storage description',
    `totalCapacity` bigint unsigned NOT NULL COMMENT 'total capacity of backup storage in bytes',
    `availableCapacity` bigint unsigned NOT NULL,
    `state` varchar(32) NOT NULL COMMENT 'backup storage state',
    `status` varchar(32) NOT NULL COMMENT 'backup storage status',
    `type` varchar(32) NOT NULL COMMENT 'backup storage type',
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP COMMENT 'last operation date',
    `createDate` timestamp,
    `deleted` varchar(255) DEFAULT NULL,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`SftpBackupStorageVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `hostname` varchar(255) NOT NULL UNIQUE,
    `username` varchar(255) NOT NULL,
    `password` varchar(255) NOT NULL,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`BackupStorageZoneRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `backupStorageUuid` varchar(32) NOT NULL COMMENT 'uuid of backup storage',
    `zoneUuid` varchar(32) NOT NULL COMMENT 'uuid of zone',
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP COMMENT 'last operation date',
    `createDate` timestamp,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`ImageEO` (
    `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'uuid',
    `size` bigint unsigned DEFAULT NULL COMMENT 'image size',
    `md5sum` varchar(255) DEFAULT NULL COMMENT 'md5sum of image',
    `name` varchar(255) NOT NULL COMMENT 'image name',
    `description` varchar(1024) DEFAULT NULL COMMENT 'image description',
    `url` varchar(1024) NOT NULL COMMENT 'image url',
    `installUrl` varchar(1024) DEFAULT NULL COMMENT 'url where image installed on secondary storage',
    `mediaType` varchar(32) NOT NULL,
    `format` varchar(32) NOT NULL,
    `system` tinyint unsigned DEFAULT 0,
    `platform` varchar(16) DEFAULT NULL,
    `type` varchar(255) NOT NULL COMMENT 'image type',
    `guestOsType` varchar(255) DEFAULT 'other' COMMENT 'guest os type string',
    `state` varchar(32) NOT NULL COMMENT 'image state',
    `status` varchar(32) NOT NULL COMMENT 'image status',
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP COMMENT 'last operation date',
    `createDate` timestamp,
    `deleted` varchar(255) DEFAULT NULL,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`ImageBackupStorageRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `backupStorageUuid` varchar(32) NOT NULL,
    `imageUuid` varchar(32) NOT NULL,
    `installPath` varchar(2048) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`ImageCacheVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `primaryStorageUuid` varchar(32) NOT NULL,
    `imageUuid` varchar(32) DEFAULT NULL,
    `installUrl` varchar(1024) NOT NULL,
    `mediaType` varchar(64) NOT NULL,
    `size` bigint unsigned NOT NULL,
    `md5sum` varchar(255) NOT NULL,
    `state` varchar(255) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`InstanceOfferingEO` (
    `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'uuid',
    `name` varchar(255) NOT NULL COMMENT 'instance offering name',
    `description` varchar(1024) DEFAULT NULL COMMENT 'instance offering description',
    `cpuNum` int(10) unsigned NOT NULL COMMENT 'number of cpus',
    `cpuSpeed` bigint unsigned NOT NULL COMMENT 'cpu speed in hz',
    `memorySize` bigint unsigned NOT NULL COMMENT 'memory size in bytes',
    `state` varchar(32) NOT NULL,
    `sortKey` int(10) unsigned DEFAULT 0 COMMENT 'sort key',
    `type` varchar(255) NOT NULL COMMENT 'offering type',
    `duration` varchar(255) NOT NULL,
    `allocatorStrategy` varchar(64) DEFAULT NULL COMMENT 'allocator strategy deciding which allocator chain to use',
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP COMMENT 'last operation date',
    `createDate` timestamp,
    `deleted` varchar(255) DEFAULT NULL,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`DiskOfferingEO` (
    `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'uuid',
    `name` varchar(255) NOT NULL COMMENT 'disk offering name',
    `description` varchar(1024) DEFAULT NULL COMMENT 'disk offering description',
    `diskSize` bigint unsigned NOT NULL COMMENT 'disk size in bytes',
    `sortKey` int(10) unsigned DEFAULT 0 COMMENT 'sort key',
    `state` varchar(32) NOT NULL,
    `type` varchar(255) NOT NULL,
    `allocatorStrategy` varchar(64) DEFAULT NULL COMMENT 'allocator strategy deciding which allocator chain to use',
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP COMMENT 'last operation date',
    `createDate` timestamp,
    `deleted` varchar(255) DEFAULT NULL,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`HostCapacityVO` (
    `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'host uuid',
    `totalMemory` bigint unsigned NOT NULL COMMENT 'total memory of host in bytes',
    `totalCpu` bigint unsigned NOT NULL COMMENT 'total cpu of host in HZ',
    `availableMemory` bigint unsigned NOT NULL COMMENT 'used memory of host in bytes',
    `availableCpu` bigint unsigned NOT NULL COMMENT 'used cpu of host in HZ',
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`VolumeEO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `primaryStorageUuid` varchar(32) DEFAULT NULL,
    `rootImageUuid` varchar(32) DEFAULT NULL,
    `vmInstanceUuid` varchar(32) DEFAULT NULL,
    `diskOfferingUuid` varchar(32) DEFAULT NULL,
    `name` varchar(255) NOT NULL,
    `description` varchar(1024) DEFAULT NULL,
    `installPath` varchar(1024) DEFAULT NULL,
    `type` varchar(64) NOT NULL,
    `format` varchar(64) DEFAULT NULL,
    `size` bigint unsigned NOT NULL,
    `deviceId` int unsigned DEFAULT NULL,
    `status` varchar(32) NOT NULL,
    `state` varchar(32) NOT NULL,
    `isAttached` tinyint(1) unsigned NOT NULL DEFAULT 0,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    `deleted` varchar(255) DEFAULT NULL,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE  `zstack`.`VolumeSnapshotTreeEO` (
    `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'host uuid',
    `volumeUuid` varchar(32) DEFAULT NULL,
    `current` tinyint(1) unsigned DEFAULT 0,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP COMMENT 'last operation date',
    `createDate` timestamp,
    `deleted` varchar(255) DEFAULT NULL,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`VolumeSnapshotEO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) NOT NULL,
    `volumeUuid` varchar(32) DEFAULT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `type` varchar(255) DEFAULT NULL,
    `format` varchar(64) NOT NULL,
    `treeUuid` varchar(32) NOT NULL,
    `parentUuid` varchar(32) DEFAULT NULL,
    `backupStorageUuid` varchar(32) DEFAULT NULL,
    `primaryStorageUuid` varchar(32) DEFAULT NULL,
    `primaryStorageInstallPath` varchar(1024) DEFAULT NULL,
    `backupStorageInstallPath` varchar(1024) DEFAULT NULL,
    `volumeType` varchar(32) NOT NULL,
    `state` varchar(64) NOT NULL,
    `status` varchar(64) NOT NULL,
    `distance` int unsigned DEFAULT 0,
    `size` bigint unsigned DEFAULT 0,
    `latest` tinyint(1) unsigned DEFAULT 0,
    `fullSnapshot` tinyint(1) unsigned DEFAULT 0,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    `deleted` varchar(255) DEFAULT NULL,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`VolumeSnapshotBackupStorageRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `volumeSnapshotUuid` varchar(32) NOT NULL,
    `backupStorageUuid` varchar(32) NOT NULL,
    `installPath` varchar(1024) NOT NULL,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`L2NetworkEO` (
    `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'l2 network uuid',
    `name` varchar(255) NOT NULL COMMENT 'name',
    `type` varchar(128) NOT NULL COMMENT 'type',
    `description` varchar(2048) DEFAULT NULL COMMENT 'description',
    `zoneUuid` varchar(32) NOT NULL COMMENT 'zone uuid',
    `physicalInterface` varchar(1024) NOT NULL COMMENT 'physical nic that this L2 network attaches to',
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP COMMENT 'last operation date',
    `createDate` timestamp,
    `deleted` varchar(255) DEFAULT NULL,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`L2VlanNetworkVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `vlan` int unsigned NOT NULL,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`L2NetworkClusterRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `l2NetworkUuid` varchar(32) NOT NULL,
    `clusterUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`L3NetworkEO` (
    `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'l3 network uuid',
    `l2NetworkUuid` varchar(32) NOT NULL COMMENT 'l2 network uuid that this l3 network belongs to', 
    `name` varchar(255) NOT NULL COMMENT 'name',
    `description` varchar(2048) DEFAULT NULL COMMENT 'description',
    `type` varchar(128) NOT NULL COMMENT 'type',
    `dnsDomain` varchar(255) DEFAULT NULL,
    `system` tinyint unsigned DEFAULT 0,
    `state` varchar(32) NOT NULL,
    `zoneUuid` varchar(32) NOT NULL COMMENT 'zone uuid',
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP COMMENT 'last operation date',
    `createDate` timestamp,
    `deleted` varchar(255) DEFAULT NULL,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE  `zstack`.`IpRangeEO` (
    `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'uuid',
    `l3NetworkUuid` varchar(32) NOT NULL COMMENT 'l3 network uuid',
    `name` varchar(255) DEFAULT NULL COMMENT 'name',
    `description` varchar(2048) DEFAULT NULL COMMENT 'description',
    `startIp` varchar(64) NOT NULL COMMENT 'start ip',
    `endIp` varchar(64) NOT NULL COMMENT 'end ip',
    `netmask` varchar(64) NOT NULL COMMENT 'netmask',
    `gateway` varchar(64) NOT NULL COMMENT 'gateway',
    `networkCidr` varchar(64) DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP COMMENT 'last operation date',
    `createDate` timestamp,
    `deleted` varchar(255) DEFAULT NULL,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`L3NetworkDnsVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `l3NetworkUuid` varchar(32) NOT NULL COMMENT 'l3 network uuid',
    `dns` varchar(255) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP COMMENT 'last operation date',
    `createDate` timestamp,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`UsedIpVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `ipRangeUuid` varchar(32) NOT NULL,
    `l3NetworkUuid` varchar(32) NOT NULL,
    `ip` varchar(128) NOT NULL,
    `ipInLong` bigint unsigned NOT NULL,
    `gateway` varchar(128) DEFAULT NULL,
    `netmask` varchar(128) DEFAULT NULL,
    `usedFor` varchar(128) DEFAULT NULL,
    `metaData` text DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`VipVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) NOT NULL,
    `description` varchar(1024) DEFAULT NULL,
    `ipRangeUuid` varchar(32) NOT NULL,
    `usedIpUuid` varchar(32) NOT NULL,
    `l3NetworkUuid` varchar(32) NOT NULL,
    `peerL3NetworkUuid` varchar(32) DEFAULT NULL,
    `state` varchar(32) NOT NULL,
    `ip` varchar(128) NOT NULL,
    `gateway` varchar(128) DEFAULT NULL,
    `netmask` varchar(128) DEFAULT NULL,
    `useFor` varchar(1024) DEFAULT NULL,
    `serviceProvider` varchar(255) DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`VmInstanceSequenceNumberVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`VmInstanceEO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `zoneUuid` varchar(32) DEFAULT NULL,
    `clusterUuid` varchar(32) DEFAULT NULL,
    `imageUuid` varchar(32) DEFAULT NULL,
    `hostUuid` varchar(32) DEFAULT NULL,
    `lastHostUuid` varchar(32) DEFAULT NULL,
    `rootVolumeUuid` varchar(32) DEFAULT NULL,
    `instanceOfferingUuid` varchar(32) DEFAULT NULL,
    `defaultL3NetworkUuid` varchar(32),
    `cpuNum` int(10) unsigned NOT NULL,
    `cpuSpeed` bigint unsigned NOT NULL,
    `memorySize` bigint unsigned NOT NULL,
    `allocatorStrategy` varchar(64) DEFAULT NULL,
    `name` varchar(255) NOT NULL,
    `platform` varchar(255) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `type` varchar(64) NOT NULL,
    `internalId` bigint unsigned NOT NULL,
    `hypervisorType` varchar(64) DEFAULT NULL,
    `state` varchar(128) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    `deleted` varchar(255) DEFAULT NULL,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`VmNicVO` (
    `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'uuid',
    `vmInstanceUuid` varchar(32) NOT NULL COMMENT 'vm instance uuid',
    `usedIpUuid` varchar(32) DEFAULT NULL UNIQUE COMMENT 'used ip uuid',
    `l3NetworkUuid` varchar(32) DEFAULT NULL COMMENT 'l3 network uuid',
    `metaData` varchar(255) DEFAULT NULL,
    `ip` varchar(128) NOT NULL COMMENT 'ip in string',
    `mac` varchar(17) NOT NULL UNIQUE COMMENT 'mac address',
    `gateway` varchar(128) DEFAULT NULL,
    `netmask` varchar(128) DEFAULT NULL,
    `internalName` varchar(128) NOT NULL,
    `deviceId` int unsigned NOT NULL COMMENT 'device id',
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP COMMENT 'last operation date',
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`EipVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `vipUuid` varchar(32) NOT NULL,
    `vipIp` varchar(128) NOT NULL,
    `state` varchar(32) NOT NULL,
    `vmNicUuid` varchar(32) DEFAULT NULL,
    `guestIp` varchar(128) DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`ApplianceVmVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `applianceVmType` varchar(64) NOT NULL,
    `managementNetworkUuid` varchar(32) DEFAULT NULL,
    `defaultRouteL3NetworkUuid` varchar(32) DEFAULT NULL,
    `status` varchar(64) NOT NULL,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`VirtualRouterVmVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `publicNetworkUuid` varchar(32) DEFAULT NULL,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`ApplianceVmFirewallRuleVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `applianceVmUuid` varchar(32) NOT NULL,
    `protocol` varchar(16),
    `sourceIp` varchar(128) DEFAULT NULL,
    `destIp` varchar(128) DEFAULT NULL,
    `startPort` int unsigned DEFAULT 0,
    `endPort` int unsigned DEFAULT 0,
    `allowCidr` varchar(32) DEFAULT NULL,
    `l3NetworkUuid` varchar(32) NOT NULL,
    `identity` varchar(128) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`GlobalConfigVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `name` varchar(255) NOT NULL,
    `description` varchar(1024) DEFAULT NULL,
    `category` varchar(64) NOT NULL,
    `defaultValue` text DEFAULT NULL,
    `value` text DEFAULT NULL,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`NetworkServiceProviderVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) NOT NULL,
    `description` varchar(1024) DEFAULT NULL,
    `type` varchar(255) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`NetworkServiceTypeVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `networkServiceProviderUuid` varchar(32) NOT NULL,
    `type` varchar(255) NOT NULL,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`NetworkServiceL3NetworkRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `l3NetworkUuid` varchar(32) NOT NULL,
    `networkServiceProviderUuid` varchar(32) NOT NULL,
    `networkServiceType` varchar(255) NOT NULL,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`NetworkServiceProviderL2NetworkRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `networkServiceProviderUuid` varchar(32) NOT NULL,
    `l2NetworkUuid` varchar(32) NOT NULL,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`VirtualRouterBootstrapIsoVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `virtualRouterUuid` varchar(32) NOT NULL,
    `isoPath` varchar(255) NOT NULL,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`VirtualRouterOfferingVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `managementNetworkUuid` varchar(32) DEFAULT NULL,
    `publicNetworkUuid` varchar(32) DEFAULT NULL,
    `imageUuid` varchar(32) NOT NULL,
    `zoneUuid` varchar(32) NOT NULL,
    `isDefault` tinyint(1) unsigned DEFAULT 0,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`SecurityGroupVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) NOT NULL,
    `description` varchar(1024) DEFAULT NULL,
    `state` varchar(32) NOT NULL,
    `internalId` bigint unsigned NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`SecurityGroupL3NetworkRefVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `l3NetworkUuid` varchar(32) NOT NULL,
    `securityGroupUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`SecurityGroupRuleVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `securityGroupUuid` varchar(32) NOT NULL,
    `type` varchar(255) NOT NULL,
    `protocol` varchar(255) NOT NULL,
    `allowedCidr` varchar(255) NOT NULL,
    `startPort` int NOT NULL,
    `endPort` int NOT NULL,
    `state` varchar(32) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`VmNicSecurityGroupRefVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `vmInstanceUuid` varchar(32) NOT NULL,
    `vmNicUuid` varchar(32) NOT NULL,
    `securityGroupUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`SecurityGroupFailureHostVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `hostUuid` varchar(32) NOT NULL,
    `securityGroupUuid` varchar(32) DEFAULT NULL,
    `managementNodeId` varchar(128) DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`SecurityGroupSequenceNumberVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`PortForwardingRuleVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `state` varchar(32) NOT NULL,
    `guestIp` varchar(128) DEFAULT NULL,
    `vipIp` varchar(128) NOT NULL,
    `vipUuid` varchar(32) NOT NULL,
    `vipPortStart` int NOT NULL,
    `vipPortEnd` int NOT NULL,
    `privatePortStart` int NOT NULL,
    `privatePortEnd` int NOT NULL,
    `vmNicUuid` varchar(32) DEFAULT NULL,
    `allowedCidr` varchar(128) DEFAULT NULL,
    `protocolType` varchar(128) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`VirtualRouterPortForwardingRuleRefVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `vipUuid` varchar(32) NOT NULL,
    `virtualRouterVmUuid` varchar(32) DEFAULT NULL,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`VirtualRouterVipVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `virtualRouterVmUuid` varchar(32) DEFAULT NULL,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`VirtualRouterEipRefVO` (
    `eipUuid` varchar(32) NOT NULL UNIQUE,
    `virtualRouterVmUuid` varchar(32) DEFAULT NULL,
    PRIMARY KEY  (`eipUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`ConsoleProxyVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `vmInstanceUuid` varchar(32) DEFAULT NULL,
    `agentIp` varchar(32) NOT NULL,
    `proxyHostname` varchar(128) NOT NULL,
    `proxyPort` int NOT NULL,
    `targetHostname` varchar(128) NOT NULL,
    `targetPort` int NOT NULL,
    `status` varchar(32) NOT NULL,
    `scheme` varchar(32) DEFAULT 'http',
    `proxyIdentity` varchar(255) DEFAULT NULL,
    `agentType` varchar(128) NOT NULL,
    `token` varchar(255) DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`WorkFlowChainVO` (
    `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'uuid made of name of workflow_flow',
    `name` varchar(255) NOT NULL,
    `owner` varchar(255) NOT NULL,
    `state` varchar(128) NOT NULL,
    `totalWorkFlows` int NOT NULL,
    `currentPosition` int NOT NULL,
    `OperationDate` timestamp,
    `reason` text DEFAULT NULL,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`WorkFlowVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `chainUuid` varchar(32) NOT NULL,
    `name` varchar(255) NOT NULL,
    `state` varchar(128) NOT NULL,
    `reason` text DEFAULT NULL,
    `position` int NOT NULL,
    `OperationDate` timestamp,
    `context` blob DEFAULT NULL,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`DeleteVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `voName` varchar(255) NOT NULL,
    `uuid` varchar(32) NOT NULL,
    `foreignVOToDeleteName` varchar(255) DEFAULT NULL,
    `foreignVOToDeleteUuid` varchar(32) DEFAULT NULL,
    `foreignVOName` varchar(255) DEFAULT NULL,
    `foreignVOUuid` varchar(32) DEFAULT NULL,
    `deletedDate` timestamp,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`InsertVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `voName` varchar(255) NOT NULL,
    `uuid` varchar(32) NOT NULL,
    `foreignVOName` varchar(255) DEFAULT NULL,
    `foreignVOUuid` varchar(32) DEFAULT NULL,
    `insertDate` timestamp,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`UpdateVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `voName` varchar(255) NOT NULL,
    `uuid` varchar(32) NOT NULL,
    `foreignVOName` varchar(255) DEFAULT NULL,
    `foreignVOUuid` varchar(32) DEFAULT NULL,
    `updateDate` timestamp,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`check_point` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `name` varchar(255) NOT NULL,
    `uuid` varchar(40) NOT NULL UNIQUE,
    `state` varchar(128) NOT NULL,
    `context` blob DEFAULT NULL,
    `op_date` timestamp,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`check_point_entry` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `check_point_id` bigint unsigned NOT NULL,
    `name` varchar(255) NOT NULL,
    `context` blob DEFAULT NULL,
    `state` varchar(128) NOT NULL,
    `reason` varchar(1024) DEFAULT NULL,
    `op_date` timestamp,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`KeyValueBinaryVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `contents` longblob NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`KeyValueVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `uuid` varchar(32) NOT NULL,
    `className` varchar(128) NOT NULL,
    `entityKey` text NOT NULL,
    `entityValue` text NOT NULL,
    `valueType` varchar(128) NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`QuartzJdbcJobVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `groupName` varchar(255) NOT NULL,
    `managementNodeId` varchar(128) DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`JobQueueVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `name` varchar(255) NOT NULL UNIQUE,
    `owner` varchar(255) DEFAULT NULL,
    `workerManagementNodeId` varchar(32) DEFAULT NULL,
    `takenDate` timestamp DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`JobQueueEntryVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `name` varchar(255) NOT NULL,
    `jobQueueId` bigint unsigned NOT NULL,
    `state` varchar(128) NOT NULL,
    `context` blob DEFAULT NULL,
    `owner` varchar(255) DEFAULT NULL,
    `issuerManagementNodeId` varchar(32) DEFAULT NULL,
    `restartable` tinyint(1) unsigned NOT NULL DEFAULT 0,
    `inDate` timestamp DEFAULT CURRENT_TIMESTAMP,
    `doneDate` timestamp NULL,
    `errText` text DEFAULT NULL,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


CREATE TABLE  `zstack`.`ManagementNodeContextVO` (
    `id` bigint unsigned NOT NULL UNIQUE,
    `inventory` blob DEFAULT NULL,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
