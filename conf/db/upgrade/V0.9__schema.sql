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
    `sshPort` int unsigned NOT NULL,
    `monPort` int unsigned NOT NULL,
    `backupStorageUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`CephPrimaryStorageVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `fsid` varchar(64) DEFAULT NULL,
    `rootVolumePoolName` varchar(255) NOT NULL,
    `dataVolumePoolName` varchar(255) NOT NULL,
    `imageCachePoolName` varchar(255) NOT NULL,
    `userKey` varchar(255) DEFAULT NULL,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`CephPrimaryStorageMonVO` (
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

CREATE TABLE  `zstack`.`CephCapacityVO` (
    `fsid` varchar(64) NOT NULL UNIQUE,
    `totalCapacity` bigint unsigned DEFAULT 0,
    `availableCapacity` bigint unsigned DEFAULT 0,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`fsid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`GarbageCollectorVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `runnerClass` varchar(512) NOT NULL,
    `context` text NOT NULL,
    `status` varchar(64) NOT NULL,
    `managementNodeUuid` varchar(32) DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`ImageCacheVolumeRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `imageCacheId` bigint unsigned NOT NULL,
    `volumeUuid` varchar(32) NOT NULL,
    `primaryStorageUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`LoadBalancerVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `providerType` varchar(255) DEFAULT NULL,
    `state` varchar(64) NOT NULL,
    `vipUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`LoadBalancerListenerVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `loadBalancerUuid` varchar(32) NOT NULL,
    `instancePort` int unsigned NOT NULL,
    `loadBalancerPort` int unsigned NOT NULL,
    `protocol` varchar(64) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`LoadBalancerListenerVmNicRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `listenerUuid` varchar(32) NOT NULL,
    `vmNicUuid` varchar(32) NOT NULL,
    `status` varchar(64) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`VirtualRouterLoadBalancerRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `virtualRouterVmUuid` varchar(32) NOT NULL,
    `loadBalancerUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# Foreign keys for table ImageCacheVolumeRefVO

ALTER TABLE ImageCacheVolumeRefVO ADD CONSTRAINT fkImageCacheVolumeRefVOImageCacheVO FOREIGN KEY (imageCacheId) REFERENCES ImageCacheVO (id) ON DELETE RESTRICT;
ALTER TABLE ImageCacheVolumeRefVO ADD CONSTRAINT fkImageCacheVolumeRefVOPrimaryStorageEO FOREIGN KEY (primaryStorageUuid) REFERENCES PrimaryStorageEO (uuid) ON DELETE CASCADE;
ALTER TABLE ImageCacheVolumeRefVO ADD CONSTRAINT fkImageCacheVolumeRefVOVolumeEO FOREIGN KEY (volumeUuid) REFERENCES VolumeEO (uuid) ON DELETE CASCADE;

# Foreign keys for table CephBackupStorageMonVO

ALTER TABLE CephBackupStorageMonVO ADD CONSTRAINT fkCephBackupStorageMonVOBackupStorageEO FOREIGN KEY (backupStorageUuid) REFERENCES BackupStorageEO (uuid) ON DELETE CASCADE;

# Foreign keys for table CephBackupStorageVO

ALTER TABLE CephBackupStorageVO ADD CONSTRAINT fkCephBackupStorageVOBackupStorageEO FOREIGN KEY (uuid) REFERENCES BackupStorageEO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE;

# Foreign keys for table CephPrimaryStorageMonVO

ALTER TABLE CephPrimaryStorageMonVO ADD CONSTRAINT fkCephPrimaryStorageMonVOPrimaryStorageEO FOREIGN KEY (primaryStorageUuid) REFERENCES PrimaryStorageEO (uuid) ON DELETE CASCADE;

# Foreign keys for table CephPrimaryStorageVO

ALTER TABLE CephPrimaryStorageVO ADD CONSTRAINT fkCephPrimaryStorageVOPrimaryStorageEO FOREIGN KEY (uuid) REFERENCES PrimaryStorageEO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE;

# Foreign keys for table LoadBalancerListenerVO

ALTER TABLE LoadBalancerListenerVO ADD CONSTRAINT fkLoadBalancerListenerVOLoadBalancerVO FOREIGN KEY (loadBalancerUuid) REFERENCES LoadBalancerVO (uuid) ON DELETE CASCADE;

# Foreign keys for table LoadBalancerListenerVmNicRefVO

ALTER TABLE LoadBalancerListenerVmNicRefVO ADD CONSTRAINT fkLoadBalancerListenerVmNicRefVOLoadBalancerListenerVO FOREIGN KEY (listenerUuid) REFERENCES LoadBalancerListenerVO (uuid) ON DELETE CASCADE;
ALTER TABLE LoadBalancerListenerVmNicRefVO ADD CONSTRAINT fkLoadBalancerListenerVmNicRefVOVmNicVO FOREIGN KEY (vmNicUuid) REFERENCES VmNicVO (uuid) ON DELETE CASCADE;

# Foreign keys for table LoadBalancerVO

ALTER TABLE LoadBalancerVO ADD CONSTRAINT fkLoadBalancerVOVipVO FOREIGN KEY (vipUuid) REFERENCES VipVO (uuid) ;

ALTER TABLE `zstack`.`ImageEO` MODIFY platform VARCHAR(255);

INSERT INTO NetworkServiceTypeVO (networkServiceProviderUuid, type) SELECT vr.uuid, 'LoadBalancer' FROM NetworkServiceProviderVO vr WHERE type = 'VirtualRouter';
