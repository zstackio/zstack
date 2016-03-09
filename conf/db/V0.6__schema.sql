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
    `type` varchar(32) NOT NULL COMMENT 'primary storage type',
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP COMMENT 'last operation date',
    `createDate` timestamp,
    `deleted` varchar(255) DEFAULT NULL,
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

# Foreign keys for table ApplianceVmFirewallRuleVO

ALTER TABLE ApplianceVmFirewallRuleVO ADD CONSTRAINT fkApplianceVmFirewallRuleVOL3NetworkEO FOREIGN KEY (l3NetworkUuid) REFERENCES L3NetworkEO (uuid) ON DELETE CASCADE;
ALTER TABLE ApplianceVmFirewallRuleVO ADD CONSTRAINT fkApplianceVmFirewallRuleVOVmInstanceEO FOREIGN KEY (applianceVmUuid) REFERENCES VmInstanceEO (uuid) ON DELETE CASCADE;

# Foreign keys for table ApplianceVmVO

ALTER TABLE ApplianceVmVO ADD CONSTRAINT fkApplianceVmVOL3NetworkEO FOREIGN KEY (managementNetworkUuid) REFERENCES L3NetworkEO (uuid) ON DELETE RESTRICT;
ALTER TABLE ApplianceVmVO ADD CONSTRAINT fkApplianceVmVOL3NetworkEO1 FOREIGN KEY (defaultRouteL3NetworkUuid) REFERENCES L3NetworkEO (uuid) ;
ALTER TABLE ApplianceVmVO ADD CONSTRAINT fkApplianceVmVOVmInstanceEO FOREIGN KEY (uuid) REFERENCES VmInstanceEO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE;

# Foreign keys for table BackupStorageZoneRefVO

ALTER TABLE BackupStorageZoneRefVO ADD CONSTRAINT fkBackupStorageZoneRefVOBackupStorageEO FOREIGN KEY (backupStorageUuid) REFERENCES BackupStorageEO (uuid) ON DELETE CASCADE;
ALTER TABLE BackupStorageZoneRefVO ADD CONSTRAINT fkBackupStorageZoneRefVOZoneEO FOREIGN KEY (zoneUuid) REFERENCES ZoneEO (uuid) ON DELETE CASCADE;

# Foreign keys for table ClusterEO

ALTER TABLE ClusterEO ADD CONSTRAINT fkClusterEOZoneEO FOREIGN KEY (zoneUuid) REFERENCES ZoneEO (uuid) ON DELETE RESTRICT;

# Foreign keys for table ConsoleProxyVO

ALTER TABLE ConsoleProxyVO ADD CONSTRAINT fkConsoleProxyVOVmInstanceEO FOREIGN KEY (vmInstanceUuid) REFERENCES VmInstanceEO (uuid) ON DELETE CASCADE;

# Foreign keys for table EipVO

ALTER TABLE EipVO ADD CONSTRAINT fkEipVOVipVO FOREIGN KEY (vipUuid) REFERENCES VipVO (uuid) ON DELETE CASCADE;
ALTER TABLE EipVO ADD CONSTRAINT fkEipVOVmNicVO FOREIGN KEY (vmNicUuid) REFERENCES VmNicVO (uuid) ON DELETE SET NULL;

# Foreign keys for table HostCapacityVO

ALTER TABLE HostCapacityVO ADD CONSTRAINT fkHostCapacityVOHostEO FOREIGN KEY (uuid) REFERENCES HostEO (uuid) ON DELETE CASCADE;

# Foreign keys for table HostEO

ALTER TABLE HostEO ADD CONSTRAINT fkHostEOClusterEO FOREIGN KEY (clusterUuid) REFERENCES ClusterEO (uuid) ON DELETE RESTRICT;
ALTER TABLE HostEO ADD CONSTRAINT fkHostEOZoneEO FOREIGN KEY (zoneUuid) REFERENCES ZoneEO (uuid) ON DELETE RESTRICT;

# Foreign keys for table ImageBackupStorageRefVO

ALTER TABLE ImageBackupStorageRefVO ADD CONSTRAINT fkImageBackupStorageRefVOBackupStorageEO FOREIGN KEY (backupStorageUuid) REFERENCES BackupStorageEO (uuid) ON DELETE CASCADE;
ALTER TABLE ImageBackupStorageRefVO ADD CONSTRAINT fkImageBackupStorageRefVOImageEO FOREIGN KEY (imageUuid) REFERENCES ImageEO (uuid) ON DELETE CASCADE;

# Foreign keys for table ImageCacheVO

ALTER TABLE ImageCacheVO ADD CONSTRAINT fkImageCacheVOImageEO FOREIGN KEY (imageUuid) REFERENCES ImageEO (uuid) ON DELETE SET NULL;
ALTER TABLE ImageCacheVO ADD CONSTRAINT fkImageCacheVOPrimaryStorageEO FOREIGN KEY (primaryStorageUuid) REFERENCES PrimaryStorageEO (uuid) ON DELETE CASCADE;

# Foreign keys for table IpRangeEO

ALTER TABLE IpRangeEO ADD CONSTRAINT fkIpRangeEOL3NetworkEO FOREIGN KEY (l3NetworkUuid) REFERENCES L3NetworkEO (uuid) ON DELETE CASCADE;

# Foreign keys for table JobQueueEntryVO

ALTER TABLE JobQueueEntryVO ADD CONSTRAINT fkJobQueueEntryVOJobQueueVO FOREIGN KEY (jobQueueId) REFERENCES JobQueueVO (id) ON DELETE CASCADE;
ALTER TABLE JobQueueEntryVO ADD CONSTRAINT fkJobQueueEntryVOManagementNodeVO FOREIGN KEY (issuerManagementNodeId) REFERENCES ManagementNodeVO (uuid) ON DELETE SET NULL;

# Foreign keys for table JobQueueVO

ALTER TABLE JobQueueVO ADD CONSTRAINT fkJobQueueVOManagementNodeVO FOREIGN KEY (workerManagementNodeId) REFERENCES ManagementNodeVO (uuid) ON DELETE SET NULL;

# Foreign keys for table KVMHostVO

ALTER TABLE KVMHostVO ADD CONSTRAINT fkKVMHostVOHostEO FOREIGN KEY (uuid) REFERENCES HostEO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE;

# Foreign keys for table KeyValueVO

ALTER TABLE KeyValueVO ADD CONSTRAINT fkKeyValueVOKeyValueBinaryVO FOREIGN KEY (uuid) REFERENCES KeyValueBinaryVO (uuid) ON DELETE CASCADE;

# Foreign keys for table L2NetworkClusterRefVO

ALTER TABLE L2NetworkClusterRefVO ADD CONSTRAINT fkL2NetworkClusterRefVOClusterEO FOREIGN KEY (clusterUuid) REFERENCES ClusterEO (uuid) ON DELETE CASCADE;
ALTER TABLE L2NetworkClusterRefVO ADD CONSTRAINT fkL2NetworkClusterRefVOL2NetworkEO FOREIGN KEY (l2NetworkUuid) REFERENCES L2NetworkEO (uuid) ON DELETE CASCADE;

# Foreign keys for table L2NetworkEO

ALTER TABLE L2NetworkEO ADD CONSTRAINT fkL2NetworkEOZoneEO FOREIGN KEY (zoneUuid) REFERENCES ZoneEO (uuid) ON DELETE RESTRICT;

# Foreign keys for table L2VlanNetworkVO

ALTER TABLE L2VlanNetworkVO ADD CONSTRAINT fkL2VlanNetworkVOL2NetworkEO FOREIGN KEY (uuid) REFERENCES L2NetworkEO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE;

# Foreign keys for table L3NetworkDnsVO

ALTER TABLE L3NetworkDnsVO ADD CONSTRAINT fkL3NetworkDnsVOL3NetworkEO FOREIGN KEY (l3NetworkUuid) REFERENCES L3NetworkEO (uuid) ON DELETE CASCADE;

# Foreign keys for table L3NetworkEO

ALTER TABLE L3NetworkEO ADD CONSTRAINT fkL3NetworkEOL2NetworkEO FOREIGN KEY (l2NetworkUuid) REFERENCES L2NetworkEO (uuid) ON DELETE RESTRICT;
ALTER TABLE L3NetworkEO ADD CONSTRAINT fkL3NetworkEOZoneEO FOREIGN KEY (zoneUuid) REFERENCES ZoneEO (uuid) ON DELETE RESTRICT;

# Foreign keys for table NetworkServiceL3NetworkRefVO

ALTER TABLE NetworkServiceL3NetworkRefVO ADD CONSTRAINT fkNetworkServiceL3NetworkRefVOL3NetworkEO FOREIGN KEY (l3NetworkUuid) REFERENCES L3NetworkEO (uuid) ON DELETE CASCADE;
ALTER TABLE NetworkServiceL3NetworkRefVO ADD CONSTRAINT fkNetworkServiceL3NetworkRefVONetworkServiceProviderVO FOREIGN KEY (networkServiceProviderUuid) REFERENCES NetworkServiceProviderVO (uuid) ON DELETE CASCADE;

# Foreign keys for table NetworkServiceProviderL2NetworkRefVO

ALTER TABLE NetworkServiceProviderL2NetworkRefVO ADD CONSTRAINT fkNetworkServiceProviderL2NetworkRefVOL2NetworkEO FOREIGN KEY (l2NetworkUuid) REFERENCES L2NetworkEO (uuid) ON DELETE CASCADE;
ALTER TABLE NetworkServiceProviderL2NetworkRefVO ADD CONSTRAINT fkNetworkServiceProviderL2NetworkRefVONetworkServiceProviderVO FOREIGN KEY (networkServiceProviderUuid) REFERENCES NetworkServiceProviderVO (uuid) ON DELETE CASCADE;

# Foreign keys for table NetworkServiceTypeVO

ALTER TABLE NetworkServiceTypeVO ADD CONSTRAINT fkNetworkServiceTypeVONetworkServiceProviderVO FOREIGN KEY (networkServiceProviderUuid) REFERENCES NetworkServiceProviderVO (uuid) ON DELETE CASCADE;

# Foreign keys for table PortForwardingRuleVO

ALTER TABLE PortForwardingRuleVO ADD CONSTRAINT fkPortForwardingRuleVOVipVO FOREIGN KEY (vipUuid) REFERENCES VipVO (uuid) ON DELETE RESTRICT;
ALTER TABLE PortForwardingRuleVO ADD CONSTRAINT fkPortForwardingRuleVOVmNicVO FOREIGN KEY (vmNicUuid) REFERENCES VmNicVO (uuid) ON DELETE SET NULL;

# Foreign keys for table PrimaryStorageCapacityVO

ALTER TABLE PrimaryStorageCapacityVO ADD CONSTRAINT fkPrimaryStorageCapacityVOPrimaryStorageEO FOREIGN KEY (uuid) REFERENCES PrimaryStorageEO (uuid) ON DELETE CASCADE;

# Foreign keys for table PrimaryStorageClusterRefVO

ALTER TABLE PrimaryStorageClusterRefVO ADD CONSTRAINT fkPrimaryStorageClusterRefVOClusterEO FOREIGN KEY (clusterUuid) REFERENCES ClusterEO (uuid) ON DELETE CASCADE;
ALTER TABLE PrimaryStorageClusterRefVO ADD CONSTRAINT fkPrimaryStorageClusterRefVOPrimaryStorageEO FOREIGN KEY (primaryStorageUuid) REFERENCES PrimaryStorageEO (uuid) ON DELETE CASCADE;

# Foreign keys for table PrimaryStorageEO

ALTER TABLE PrimaryStorageEO ADD CONSTRAINT fkPrimaryStorageEOZoneEO FOREIGN KEY (zoneUuid) REFERENCES ZoneEO (uuid) ON DELETE RESTRICT;

# Foreign keys for table SecurityGroupFailureHostVO

ALTER TABLE SecurityGroupFailureHostVO ADD CONSTRAINT fkSecurityGroupFailureHostVOHostEO FOREIGN KEY (hostUuid) REFERENCES HostEO (uuid) ON DELETE CASCADE;
ALTER TABLE SecurityGroupFailureHostVO ADD CONSTRAINT fkSecurityGroupFailureHostVOManagementNodeVO FOREIGN KEY (managementNodeId) REFERENCES ManagementNodeVO (uuid) ON DELETE SET NULL;

# Foreign keys for table SecurityGroupL3NetworkRefVO

ALTER TABLE SecurityGroupL3NetworkRefVO ADD CONSTRAINT fkSecurityGroupL3NetworkRefVOL3NetworkEO FOREIGN KEY (l3NetworkUuid) REFERENCES L3NetworkEO (uuid) ON DELETE CASCADE;
ALTER TABLE SecurityGroupL3NetworkRefVO ADD CONSTRAINT fkSecurityGroupL3NetworkRefVOSecurityGroupVO FOREIGN KEY (securityGroupUuid) REFERENCES SecurityGroupVO (uuid) ON DELETE CASCADE;

# Foreign keys for table SecurityGroupRuleVO

ALTER TABLE SecurityGroupRuleVO ADD CONSTRAINT fkSecurityGroupRuleVOSecurityGroupVO FOREIGN KEY (securityGroupUuid) REFERENCES SecurityGroupVO (uuid) ON DELETE CASCADE;

# Foreign keys for table SftpBackupStorageVO

ALTER TABLE SftpBackupStorageVO ADD CONSTRAINT fkSftpBackupStorageVOBackupStorageEO FOREIGN KEY (uuid) REFERENCES BackupStorageEO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE;

# Foreign keys for table SimulatorHostVO

ALTER TABLE SimulatorHostVO ADD CONSTRAINT fkSimulatorHostVOHostEO FOREIGN KEY (uuid) REFERENCES HostEO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE;

# Foreign keys for table UsedIpVO

ALTER TABLE UsedIpVO ADD CONSTRAINT fkUsedIpVOIpRangeEO FOREIGN KEY (ipRangeUuid) REFERENCES IpRangeEO (uuid) ON DELETE CASCADE;
ALTER TABLE UsedIpVO ADD CONSTRAINT fkUsedIpVOL3NetworkEO FOREIGN KEY (l3NetworkUuid) REFERENCES L3NetworkEO (uuid) ON DELETE CASCADE;

# Foreign keys for table VipVO

ALTER TABLE VipVO ADD CONSTRAINT fkVipVOIpRangeEO FOREIGN KEY (ipRangeUuid) REFERENCES IpRangeEO (uuid) ON DELETE CASCADE;
ALTER TABLE VipVO ADD CONSTRAINT fkVipVOL3NetworkEO FOREIGN KEY (l3NetworkUuid) REFERENCES L3NetworkEO (uuid) ON DELETE CASCADE;
ALTER TABLE VipVO ADD CONSTRAINT fkVipVOL3NetworkEO1 FOREIGN KEY (peerL3NetworkUuid) REFERENCES L3NetworkEO (uuid) ON DELETE CASCADE;

# Foreign keys for table VirtualRouterBootstrapIsoVO

ALTER TABLE VirtualRouterBootstrapIsoVO ADD CONSTRAINT fkVirtualRouterBootstrapIsoVOVmInstanceEO FOREIGN KEY (virtualRouterUuid) REFERENCES VmInstanceEO (uuid) ON DELETE CASCADE;

# Foreign keys for table VirtualRouterEipRefVO

ALTER TABLE VirtualRouterEipRefVO ADD CONSTRAINT fkVirtualRouterEipRefVOEipVO FOREIGN KEY (eipUuid) REFERENCES EipVO (uuid) ON DELETE RESTRICT;
ALTER TABLE VirtualRouterEipRefVO ADD CONSTRAINT fkVirtualRouterEipRefVOVmInstanceEO FOREIGN KEY (virtualRouterVmUuid) REFERENCES VmInstanceEO (uuid) ON DELETE CASCADE;

# Foreign keys for table VirtualRouterOfferingVO

ALTER TABLE VirtualRouterOfferingVO ADD CONSTRAINT fkVirtualRouterOfferingVOImageEO FOREIGN KEY (imageUuid) REFERENCES ImageEO (uuid) ON DELETE CASCADE;
ALTER TABLE VirtualRouterOfferingVO ADD CONSTRAINT fkVirtualRouterOfferingVOInstanceOfferingEO FOREIGN KEY (uuid) REFERENCES InstanceOfferingEO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE;
ALTER TABLE VirtualRouterOfferingVO ADD CONSTRAINT fkVirtualRouterOfferingVOL3NetworkEO FOREIGN KEY (managementNetworkUuid) REFERENCES L3NetworkEO (uuid) ON DELETE CASCADE;
ALTER TABLE VirtualRouterOfferingVO ADD CONSTRAINT fkVirtualRouterOfferingVOL3NetworkEO1 FOREIGN KEY (publicNetworkUuid) REFERENCES L3NetworkEO (uuid) ON DELETE CASCADE;
ALTER TABLE VirtualRouterOfferingVO ADD CONSTRAINT fkVirtualRouterOfferingVOZoneEO FOREIGN KEY (zoneUuid) REFERENCES ZoneEO (uuid) ON DELETE CASCADE;

# Foreign keys for table VirtualRouterPortForwardingRuleRefVO

ALTER TABLE VirtualRouterPortForwardingRuleRefVO ADD CONSTRAINT fkVirtualRouterPortForwardingRuleRefVOVipVO FOREIGN KEY (vipUuid) REFERENCES VipVO (uuid) ON DELETE CASCADE;
ALTER TABLE VirtualRouterPortForwardingRuleRefVO ADD CONSTRAINT fkVirtualRouterPortForwardingRuleRefVOVmInstanceEO FOREIGN KEY (virtualRouterVmUuid) REFERENCES VmInstanceEO (uuid) ON DELETE CASCADE;

# Foreign keys for table VirtualRouterVipVO

ALTER TABLE VirtualRouterVipVO ADD CONSTRAINT fkVirtualRouterVipVOVipVO FOREIGN KEY (uuid) REFERENCES VipVO (uuid) ON DELETE RESTRICT;
ALTER TABLE VirtualRouterVipVO ADD CONSTRAINT fkVirtualRouterVipVOVmInstanceEO FOREIGN KEY (virtualRouterVmUuid) REFERENCES VmInstanceEO (uuid) ON DELETE CASCADE;

# Foreign keys for table VirtualRouterVmVO

ALTER TABLE VirtualRouterVmVO ADD CONSTRAINT fkVirtualRouterVmVOVmInstanceEO FOREIGN KEY (uuid) REFERENCES VmInstanceEO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE;

# Foreign keys for table VmInstanceEO

ALTER TABLE VmInstanceEO ADD CONSTRAINT fkVmInstanceEOClusterEO FOREIGN KEY (clusterUuid) REFERENCES ClusterEO (uuid) ON DELETE SET NULL;
ALTER TABLE VmInstanceEO ADD CONSTRAINT fkVmInstanceEOHostEO FOREIGN KEY (hostUuid) REFERENCES HostEO (uuid) ON DELETE SET NULL;
ALTER TABLE VmInstanceEO ADD CONSTRAINT fkVmInstanceEOHostEO1 FOREIGN KEY (lastHostUuid) REFERENCES HostEO (uuid) ON DELETE SET NULL;
ALTER TABLE VmInstanceEO ADD CONSTRAINT fkVmInstanceEOImageEO FOREIGN KEY (imageUuid) REFERENCES ImageEO (uuid) ON DELETE RESTRICT;
ALTER TABLE VmInstanceEO ADD CONSTRAINT fkVmInstanceEOInstanceOfferingEO FOREIGN KEY (instanceOfferingUuid) REFERENCES InstanceOfferingEO (uuid) ON DELETE RESTRICT;
ALTER TABLE VmInstanceEO ADD CONSTRAINT fkVmInstanceEOZoneEO FOREIGN KEY (zoneUuid) REFERENCES ZoneEO (uuid) ON DELETE SET NULL;

# Foreign keys for table VmNicSecurityGroupRefVO

ALTER TABLE VmNicSecurityGroupRefVO ADD CONSTRAINT fkVmNicSecurityGroupRefVOSecurityGroupVO FOREIGN KEY (securityGroupUuid) REFERENCES SecurityGroupVO (uuid) ON DELETE CASCADE;
ALTER TABLE VmNicSecurityGroupRefVO ADD CONSTRAINT fkVmNicSecurityGroupRefVOVmInstanceEO FOREIGN KEY (vmInstanceUuid) REFERENCES VmInstanceEO (uuid) ON DELETE CASCADE;
ALTER TABLE VmNicSecurityGroupRefVO ADD CONSTRAINT fkVmNicSecurityGroupRefVOVmNicVO FOREIGN KEY (vmNicUuid) REFERENCES VmNicVO (uuid) ON DELETE CASCADE;

# Foreign keys for table VmNicVO

ALTER TABLE VmNicVO ADD CONSTRAINT fkVmNicVOL3NetworkEO FOREIGN KEY (l3NetworkUuid) REFERENCES L3NetworkEO (uuid) ON DELETE SET NULL;
ALTER TABLE VmNicVO ADD CONSTRAINT fkVmNicVOUsedIpVO FOREIGN KEY (usedIpUuid) REFERENCES UsedIpVO (uuid) ON DELETE SET NULL;
ALTER TABLE VmNicVO ADD CONSTRAINT fkVmNicVOVmInstanceEO FOREIGN KEY (vmInstanceUuid) REFERENCES VmInstanceEO (uuid) ON DELETE CASCADE;

# Foreign keys for table VolumeEO

ALTER TABLE VolumeEO ADD CONSTRAINT fkVolumeEODiskOfferingEO FOREIGN KEY (diskOfferingUuid) REFERENCES DiskOfferingEO (uuid) ON DELETE RESTRICT;
ALTER TABLE VolumeEO ADD CONSTRAINT fkVolumeEOImageEO FOREIGN KEY (rootImageUuid) REFERENCES ImageEO (uuid) ON DELETE SET NULL;
ALTER TABLE VolumeEO ADD CONSTRAINT fkVolumeEOPrimaryStorageEO FOREIGN KEY (primaryStorageUuid) REFERENCES PrimaryStorageEO (uuid) ON DELETE CASCADE;
ALTER TABLE VolumeEO ADD CONSTRAINT fkVolumeEOVmInstanceEO FOREIGN KEY (vmInstanceUuid) REFERENCES VmInstanceEO (uuid) ON DELETE CASCADE;

# Foreign keys for table VolumeSnapshotBackupStorageRefVO

ALTER TABLE VolumeSnapshotBackupStorageRefVO ADD CONSTRAINT fkVolumeSnapshotBackupStorageRefVOBackupStorageEO FOREIGN KEY (backupStorageUuid) REFERENCES BackupStorageEO (uuid) ON DELETE CASCADE;
ALTER TABLE VolumeSnapshotBackupStorageRefVO ADD CONSTRAINT fkVolumeSnapshotBackupStorageRefVOVolumeSnapshotEO FOREIGN KEY (volumeSnapshotUuid) REFERENCES VolumeSnapshotEO (uuid) ON DELETE CASCADE;

# Foreign keys for table VolumeSnapshotEO

ALTER TABLE VolumeSnapshotEO ADD CONSTRAINT fkVolumeSnapshotEOPrimaryStorageEO FOREIGN KEY (primaryStorageUuid) REFERENCES PrimaryStorageEO (uuid) ON DELETE SET NULL;
ALTER TABLE VolumeSnapshotEO ADD CONSTRAINT fkVolumeSnapshotEOVolumeEO FOREIGN KEY (volumeUuid) REFERENCES VolumeEO (uuid) ON DELETE SET NULL;
ALTER TABLE VolumeSnapshotEO ADD CONSTRAINT fkVolumeSnapshotEOVolumeSnapshotEO FOREIGN KEY (parentUuid) REFERENCES VolumeSnapshotEO (uuid) ON DELETE SET NULL;
ALTER TABLE VolumeSnapshotEO ADD CONSTRAINT fkVolumeSnapshotEOVolumeSnapshotTreeEO FOREIGN KEY (treeUuid) REFERENCES VolumeSnapshotTreeEO (uuid) ON DELETE CASCADE;

# Foreign keys for table VolumeSnapshotTreeEO

ALTER TABLE VolumeSnapshotTreeEO ADD CONSTRAINT fkVolumeSnapshotTreeEOVolumeEO FOREIGN KEY (volumeUuid) REFERENCES VolumeEO (uuid) ON DELETE SET NULL;

# Index for table AccountResourceRefVO

CREATE INDEX idxAccountResourceRefVOresourceUuid ON AccountResourceRefVO (resourceUuid);
CREATE INDEX idxAccountResourceRefVOresourceType ON AccountResourceRefVO (resourceType);

# Index for table AccountVO

CREATE INDEX idxAccountVOname ON AccountVO (name);

# Index for table ApplianceVmFirewallRuleVO

CREATE INDEX idxApplianceVmFirewallRuleVOprotocol ON ApplianceVmFirewallRuleVO (protocol);
CREATE INDEX idxApplianceVmFirewallRuleVOstartPort ON ApplianceVmFirewallRuleVO (startPort);
CREATE INDEX idxApplianceVmFirewallRuleVOendPort ON ApplianceVmFirewallRuleVO (endPort);
CREATE INDEX idxApplianceVmFirewallRuleVOallowCidr ON ApplianceVmFirewallRuleVO (allowCidr);
CREATE INDEX idxApplianceVmFirewallRuleVOsourceIp ON ApplianceVmFirewallRuleVO (sourceIp);
CREATE INDEX idxApplianceVmFirewallRuleVOdestIp ON ApplianceVmFirewallRuleVO (destIp);
CREATE INDEX idxApplianceVmFirewallRuleVOidentity ON ApplianceVmFirewallRuleVO (identity);

# Index for table BackupStorageEO

CREATE INDEX idxBackupStorageEOname ON BackupStorageEO (name);

# Index for table ClusterEO

CREATE INDEX idxClusterEOname ON ClusterEO (name);

# Index for table DiskOfferingEO

CREATE INDEX idxDiskOfferingEOname ON DiskOfferingEO (name);

# Index for table EipVO

CREATE INDEX idxEipVOname ON EipVO (name);

# Index for table HostCapacityVO

CREATE INDEX idxHostCapacityVOtotalMemory ON HostCapacityVO (totalMemory);
CREATE INDEX idxHostCapacityVOtotalCpu ON HostCapacityVO (totalCpu);
CREATE INDEX idxHostCapacityVOavailableMemory ON HostCapacityVO (availableMemory);
CREATE INDEX idxHostCapacityVOavailableCpu ON HostCapacityVO (availableCpu);

# Index for table HostEO

CREATE INDEX idxHostEOuuid ON HostEO (uuid);

# Index for table ImageEO

CREATE INDEX idxImageEOname ON ImageEO (name);

# Index for table InstanceOfferingEO

CREATE INDEX idxInstanceOfferingEOname ON InstanceOfferingEO (name);

# Index for table IpRangeEO

CREATE INDEX idxIpRangeEOname ON IpRangeEO (name);
CREATE INDEX idxIpRangeEOstartIp ON IpRangeEO (startIp);
CREATE INDEX idxIpRangeEOendIp ON IpRangeEO (endIp);
CREATE INDEX idxIpRangeEOnetmask ON IpRangeEO (netmask);
CREATE INDEX idxIpRangeEOgateway ON IpRangeEO (gateway);

# Index for table L2NetworkEO

CREATE INDEX idxL2NetworkEOname ON L2NetworkEO (name);

# Index for table L3NetworkEO

CREATE INDEX idxL3NetworkEOname ON L3NetworkEO (name);

# Index for table NetworkServiceProviderVO

CREATE INDEX idxNetworkServiceProviderVOname ON NetworkServiceProviderVO (name);

# Index for table PortForwardingRuleVO

CREATE INDEX idxPortForwardingRuleVOname ON PortForwardingRuleVO (name);
CREATE INDEX idxPortForwardingRuleVOvipPortStart ON PortForwardingRuleVO (vipPortStart);
CREATE INDEX idxPortForwardingRuleVOvipPortEnd ON PortForwardingRuleVO (vipPortEnd);
CREATE INDEX idxPortForwardingRuleVOprivatePortStart ON PortForwardingRuleVO (privatePortStart);
CREATE INDEX idxPortForwardingRuleVOprivatePortEnd ON PortForwardingRuleVO (privatePortEnd);

# Index for table PrimaryStorageCapacityVO

CREATE INDEX idxPrimaryStorageCapacityVOtotalCapacity ON PrimaryStorageCapacityVO (totalCapacity);
CREATE INDEX idxPrimaryStorageCapacityVOavailableCapacity ON PrimaryStorageCapacityVO (availableCapacity);

# Index for table SecurityGroupVO

CREATE INDEX idxSecurityGroupVOname ON SecurityGroupVO (name);

# Index for table SystemTagVO

CREATE INDEX idxSystemTagVOresourceUuid ON SystemTagVO (resourceUuid);
CREATE INDEX idxSystemTagVOresourceType ON SystemTagVO (resourceType);
CREATE INDEX idxSystemTagVOtag ON SystemTagVO (tag(128));
CREATE INDEX idxSystemTagVOtype ON SystemTagVO (type);

# Index for table UsedIpVO

CREATE INDEX idxUsedIpVOip ON UsedIpVO (ip);
CREATE INDEX idxUsedIpVOipInLong ON UsedIpVO (ipInLong);

# Index for table UserTagVO

CREATE INDEX idxUserTagVOresourceUuid ON UserTagVO (resourceUuid);
CREATE INDEX idxUserTagVOresourceType ON UserTagVO (resourceType);
CREATE INDEX idxUserTagVOtag ON UserTagVO (tag(128));
CREATE INDEX idxUserTagVOtype ON UserTagVO (type);

# Index for table VipVO

CREATE INDEX idxVipVOname ON VipVO (name);
CREATE INDEX idxVipVOip ON VipVO (ip);

# Index for table VmInstanceEO

CREATE INDEX idxVmInstanceEOname ON VmInstanceEO (name(128));

# Index for table VmNicVO

CREATE INDEX idxVmNicVOip ON VmNicVO (ip);
CREATE INDEX idxVmNicVOmac ON VmNicVO (mac);

# Index for table VolumeEO

CREATE INDEX idxVolumeEOname ON VolumeEO (name);

# Index for table VolumeSnapshotEO

CREATE INDEX idxVolumeSnapshotEOname ON VolumeSnapshotEO (name);

# Index for table ZoneEO

CREATE INDEX idxZoneEOname ON ZoneEO (name);

CREATE VIEW `zstack`.`VmInstanceVO` AS SELECT uuid, name, description, zoneUuid, clusterUuid, imageUuid, hostUuid, internalId, lastHostUuid, instanceOfferingUuid, rootVolumeUuid, defaultL3NetworkUuid, type, hypervisorType, cpuNum, cpuSpeed, memorySize, allocatorStrategy, createDate, lastOpDate, state FROM `zstack`.`VmInstanceEO` WHERE deleted IS NULL;

CREATE VIEW `zstack`.`ImageVO` AS SELECT uuid, name, description, status, state, size, md5Sum, platform, type, format, url, system, mediaType, createDate, lastOpDate, guestOsType FROM `zstack`.`ImageEO` WHERE deleted IS NULL;

CREATE VIEW `zstack`.`VolumeVO` AS SELECT uuid, name, description, primaryStorageUuid, vmInstanceUuid, diskOfferingUuid, rootImageUuid, installPath, type, status, size, deviceId, format, state, createDate, lastOpDate FROM `zstack`.`VolumeEO` WHERE deleted IS NULL;

CREATE VIEW `zstack`.`InstanceOfferingVO` AS SELECT uuid, name, description, cpuNum, cpuSpeed, memorySize, allocatorStrategy, sortKey, state, createDate, lastOpDate, type, duration FROM `zstack`.`InstanceOfferingEO` WHERE deleted IS NULL;

CREATE VIEW `zstack`.`DiskOfferingVO` AS SELECT uuid, name, description, diskSize, sortKey, type, state, createDate, lastOpDate, allocatorStrategy FROM `zstack`.`DiskOfferingEO` WHERE deleted IS NULL;

CREATE VIEW `zstack`.`PrimaryStorageVO` AS SELECT uuid, zoneUuid, name, url, description, type, mountPath, state, status, createDate, lastOpDate FROM `zstack`.`PrimaryStorageEO` WHERE deleted IS NULL;

CREATE VIEW `zstack`.`VolumeSnapshotVO` AS SELECT uuid, name, description, type, volumeUuid, format, treeUuid, parentUuid, primaryStorageUuid, primaryStorageInstallPath, distance, size, latest, fullSnapshot, volumeType, state, status, createDate, lastOpDate FROM `zstack`.`VolumeSnapshotEO` WHERE deleted IS NULL;

CREATE VIEW `zstack`.`VolumeSnapshotTreeVO` AS SELECT uuid, volumeUuid, current, createDate, lastOpDate FROM `zstack`.`VolumeSnapshotTreeEO` WHERE deleted IS NULL;

CREATE VIEW `zstack`.`BackupStorageVO` AS SELECT uuid, name, url, description, totalCapacity, availableCapacity, type, state, status, createDate, lastOpDate FROM `zstack`.`BackupStorageEO` WHERE deleted IS NULL;

CREATE VIEW `zstack`.`L3NetworkVO` AS SELECT uuid, name, description, state, type, zoneUuid, l2NetworkUuid, system, dnsDomain, createDate, lastOpDate FROM `zstack`.`L3NetworkEO` WHERE deleted IS NULL;

CREATE VIEW `zstack`.`IpRangeVO` AS SELECT uuid, l3NetworkUuid, name, description, startIp, endIp, netmask, gateway, networkCidr, createDate, lastOpDate FROM `zstack`.`IpRangeEO` WHERE deleted IS NULL;

CREATE VIEW `zstack`.`L2NetworkVO` AS SELECT uuid, name, description, type, zoneUuid, physicalInterface, createDate, lastOpDate FROM `zstack`.`L2NetworkEO` WHERE deleted IS NULL;

CREATE VIEW `zstack`.`ClusterVO` AS SELECT uuid, zoneUuid, name, type, description, state, hypervisorType, createDate, lastOpDate, managementNodeId FROM `zstack`.`ClusterEO` WHERE deleted IS NULL;

CREATE VIEW `zstack`.`ZoneVO` AS SELECT uuid, name, type, description, state, createDate, lastOpDate FROM `zstack`.`ZoneEO` WHERE deleted IS NULL;

CREATE VIEW `zstack`.`HostVO` AS SELECT uuid, zoneUuid, clusterUuid, name, description, managementIp, hypervisorType, state, status, createDate, lastOpDate FROM `zstack`.`HostEO` WHERE deleted IS NULL;
