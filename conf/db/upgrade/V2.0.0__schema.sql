ALTER TABLE VmInstanceEO ADD CONSTRAINT fkVmInstanceEOVolumeEO FOREIGN KEY (rootVolumeUuid) REFERENCES VolumeEO (uuid) ON DELETE SET NULL;

-- ----------------------------
--  Table structure for `BaremetalPxeServerVO`
-- ----------------------------
CREATE TABLE `BaremetalPxeServerVO` (
  `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'uuid',
  `dhcpInterface` varchar(128) NOT NULL UNIQUE COMMENT 'pxe dhcp interface',
  `dhcpRangeBegin` varchar(32) DEFAULT NULL COMMENT 'dhcp range begin',
  `dhcpRangeEnd` varchar(32) DEFAULT NULL COMMENT 'dhcp range end',
  `dhcpRangeNetmask` varchar(32) DEFAULT NULL COMMENT 'dhcp range netmask',
  `status` varchar(32) DEFAULT NULL COMMENT 'pxe server status',
  `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp,
  PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
--  Table structure for `BaremetalChessisVO`
-- ----------------------------
CREATE TABLE `BaremetalChessisVO` (
  `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'uuid',
  `ipmiAddress` varchar(32) NOT NULL UNIQUE COMMENT 'baremetal chessis ipmi address',
  `ipmiUsername` varchar(255) NOT NULL COMMENT 'baremetal chessis ipmi username',
  `ipmiPassword` varchar(255) NOT NULL COMMENT 'baremetal chessis ipmi password',
  `provisioned` tinyint(1) NOT NULL DEFAULT 0 COMMENT 'is baremetal host provisioned already',
  `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp,
  PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
--  Table structure for `BaremetalHostCfgVO`
-- ----------------------------
CREATE TABLE `BaremetalHostCfgVO` (
  `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'uuid',
  `chessisUuid` varchar(32) NOT NULL UNIQUE COMMENT 'baremetal chessis uuid',
  `password` varchar(255) DEFAULT NULL COMMENT 'host root password',
  `vnc` tinyint(1) unsigned NOT NULL DEFAULT 1 COMMENT 'start vnc or not',
  `unattended` tinyint(1) unsigned NOT NULL DEFAULT 1 COMMENT 'unattended installation process or not',
  `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp,
  PRIMARY KEY  (`uuid`),
  CONSTRAINT `fkBaremetalHostCfgVOBaremetalChessisVO` FOREIGN KEY (`chessisUuid`) REFERENCES `BaremetalChessisVO` (`uuid`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ----------------------------
--  Table structure for `BaremetalHostNicCfgVO`
-- ----------------------------
CREATE TABLE `BaremetalHostNicCfgVO` (
  `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'uuid',
  `hostCfgUuid` varchar(32) NOT NULL COMMENT 'baremetal hostcfg uuid',
  `mac` varchar(32) NOT NULL UNIQUE COMMENT 'host nic mac',
  `ip` varchar(32) NOT NULL UNIQUE COMMENT 'host nic ip',
  `netmask` varchar(32) NOT NULL COMMENT 'host nic netmask',
  `gateway` varchar(32) DEFAULT NULL COMMENT 'host nic gateway',
  `dns` varchar(32) DEFAULT NULL COMMENT 'host nic dns',
  `pxe` tinyint(1) unsigned NOT NULL DEFAULT 0 COMMENT 'as pxe client or not',
  `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp,
  PRIMARY KEY  (`uuid`),
  CONSTRAINT `fkBaremetalHostNicCfgVOBaremetalHostCfgVO` FOREIGN KEY (`hostCfgUuid`) REFERENCES `BaremetalHostCfgVO` (`uuid`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO ResourceVO (uuid, resourceType) SELECT t.uuid, "BaremetalPxeServerVO" FROM BaremetalPxeServerVO t;
INSERT INTO ResourceVO (uuid, resourceType) SELECT t.uuid, "BaremetalChessisVO" FROM BaremetalChessisVO t;
INSERT INTO ResourceVO (uuid, resourceType) SELECT t.uuid, "BaremetalHostCfgVO" FROM BaremetalHostCfgVO t;
INSERT INTO ResourceVO (uuid, resourceType) SELECT t.uuid, "BaremetalHostNicCfgVO" FROM BaremetalHostNicCfgVO t;

UPDATE ResourceVO SET resourceType = "BackupStorageVO" WHERE resourceType = "BackupStorageEO";
UPDATE ResourceVO SET resourceType = "ClusterVO" WHERE resourceType = "ClusterEO";
UPDATE ResourceVO SET resourceType = "HostVO" WHERE resourceType = "HostEO";
UPDATE ResourceVO SET resourceType = "ImageVO" WHERE resourceType = "ImageEO";
UPDATE ResourceVO SET resourceType = "InstanceOfferingVO" WHERE resourceType = "InstanceOfferingEO";
UPDATE ResourceVO SET resourceType = "DiskOfferingVO" WHERE resourceType = "DiskOfferingEO";
UPDATE ResourceVO SET resourceType = "IpRangeVO" WHERE resourceType = "IpRangeEO";
UPDATE ResourceVO SET resourceType = "L2NetworkVO" WHERE resourceType = "L2NetworkEO";
UPDATE ResourceVO SET resourceType = "L3NetworkVO" WHERE resourceType = "L3NetworkEO";
UPDATE ResourceVO SET resourceType = "PrimaryStorageVO" WHERE resourceType = "PrimaryStorageEO";
UPDATE ResourceVO SET resourceType = "VmInstanceVO" WHERE resourceType = "VmInstanceEO";
UPDATE ResourceVO SET resourceType = "VolumeVO" WHERE resourceType = "VolumeEO";
UPDATE ResourceVO SET resourceType = "VolumeSnapshotVO" WHERE resourceType = "VolumeSnapshotEO";
UPDATE ResourceVO SET resourceType = "VolumeSnapshotTreeVO" WHERE resourceType = "VolumeSnapshotTreeEO";
UPDATE ResourceVO SET resourceType = "ZoneVO" WHERE resourceType = "ZoneEO";
-- ----------------------------
--  Table structure for `SnapshotUsageVO`
-- ----------------------------
CREATE TABLE `SnapShotUsageVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `volumeUuid` varchar(32) NOT NULL,
  `SnapshotUuid` varchar(32) NOT NULL,
  `SnapshotStatus` varchar(64) NOT NULL,
  `SnapshotName` varchar(255) NOT NULL,
  `accountUuid` varchar(32) NOT NULL,
  `SnapshotSize` bigint unsigned NOT NULL,
  `dateInLong` bigint unsigned NOT NULL,
  `inventory` text,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
