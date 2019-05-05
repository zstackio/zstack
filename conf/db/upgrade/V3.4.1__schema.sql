CREATE INDEX idxVmUsageVOvmUuid ON VmUsageVO(accountUuid, dateInLong, vmUuid);
CREATE INDEX idxVmUsageVOaccountUuidVmUuid ON VmUsageVO(accountUuid, vmUuid);

CREATE INDEX idxRootVolumeUsageVOvolumeUuid ON RootVolumeUsageVO(accountUuid, dateInLong, volumeUuid);
CREATE INDEX idxRootVolumeUsageVOaccountUuidVolumeUuid ON RootVolumeUsageVO(accountUuid, volumeUuid);

CREATE INDEX idxDataVolumeUsageVOvolumeUuid ON DataVolumeUsageVO(accountUuid, dateInLong, volumeUuid);
CREATE INDEX idxDataVolumeUsageVOaccountUuidVolumeUuid ON DataVolumeUsageVO(accountUuid, volumeUuid);

CREATE INDEX idxPciDeviceUsageVOpciDeviceUuid ON PciDeviceUsageVO(accountUuid, dateInLong, pciDeviceUuid);
CREATE INDEX idxPciDeviceUsageVOaccountUuidPciDeviceUuid ON PciDeviceUsageVO(accountUuid, pciDeviceUuid);

CREATE INDEX idxPubIpVipBandwidthUsageVOvipUuid ON PubIpVipBandwidthUsageVO(accountUuid, dateInLong, vipUuid);
CREATE INDEX idxPubIpVipBandwidthUsageVOaccountUuidVipUuid ON PubIpVipBandwidthUsageVO(accountUuid, vipUuid);

CREATE INDEX idxPubIpVmNicBandwidthUsageVOvmNicUuid ON PubIpVmNicBandwidthUsageVO(accountUuid, dateInLong, vmNicUuid);
CREATE INDEX idxPubIpVmNicBandwidthUsageVOaccountUuidVmNicUuid ON PubIpVmNicBandwidthUsageVO(accountUuid, vmNicUuid);

ALTER TABLE VmUsageVO add column resourcePriceUserConfig varchar(1024) DEFAULT NULL;
ALTER TABLE RootVolumeUsageVO add column resourcePriceUserConfig varchar(1024) DEFAULT NULL;
ALTER TABLE DataVolumeUsageVO add column resourcePriceUserConfig varchar(1024) DEFAULT NULL;
ALTER TABLE PciDeviceUsageVO add column resourcePriceUserConfig varchar(1024) DEFAULT NULL;
ALTER TABLE PubIpVipBandwidthUsageVO add column resourcePriceUserConfig varchar(1024) DEFAULT NULL;
ALTER TABLE PubIpVmNicBandwidthUsageVO add column resourcePriceUserConfig varchar(1024) DEFAULT NULL;
ALTER TABLE SnapShotUsageVO add column resourcePriceUserConfig varchar(1024) DEFAULT NULL;

CREATE TABLE `BillingVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `billingType` varchar(255) NOT NULL,
  `accountUuid` varchar(32) NOT NULL,
  `resourceUuid` varchar(32) NOT NULL,
  `resourceName` varchar(255),
  `spending` double unsigned NOT NULL,
  `startTime` bigint(20) unsigned NOT NULL,
  `endTime` bigint(20) unsigned NOT NULL,
  `hypervisorType` varchar(64) DEFAULT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `resourceUuid` (`resourceUuid`),
  KEY `acountUuid` (`accountUuid`),
  KEY `idxBillingVOaccountUuid` (`accountUuid`,`startTime`, `endTime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `DataVolumeBillingVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `volumeSize` bigint(20) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `RootVolumeBillingVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `volumeSize` bigint(20) unsigned NOT NULL,
  `vmInstanceUuid` varchar(32),
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VmCPUBillingVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `VmMemoryBillingVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PciDeviceBillingVO`(
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `vmName` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
)ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PubIpVipBandwidthInBillingVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `vipIp` varchar(255),
  `bandwidthSize` bigint(20) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PubIpVipBandwidthOutBillingVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `vipIp` varchar(255),
  `bandwidthSize` bigint(20) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PubIpVmNicBandwidthInBillingVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `vmNicIp` varchar(255),
  `bandwidthSize` bigint(20) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PubIpVmNicBandwidthOutBillingVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `vmNicIp` varchar(255),
  `bandwidthSize` bigint(20) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `DataVolumeUsageHistoryVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `volumeUuid` varchar(32) NOT NULL,
  `volumeStatus` varchar(64) NOT NULL,
  `volumeName` varchar(255) NOT NULL,
  `accountUuid` varchar(32) NOT NULL,
  `volumeSize` bigint(20) unsigned NOT NULL,
  `dateInLong` bigint(20) unsigned NOT NULL,
  `inventory` text,
  `resourcePriceUserConfig` varchar(1024) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `idxDataVolumeUsageVOaccountUuid` (`accountUuid`,`dateInLong`),
  KEY `idxDataVolumeUsageVOvolumeUuid` (`accountUuid`,`dateInLong`,`volumeUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `RootVolumeUsageHistoryVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `vmUuid` varchar(32) NOT NULL,
  `volumeUuid` varchar(32) NOT NULL,
  `volumeStatus` varchar(64) NOT NULL,
  `volumeName` varchar(255) NOT NULL,
  `accountUuid` varchar(32) NOT NULL,
  `volumeSize` bigint(20) unsigned NOT NULL,
  `dateInLong` bigint(20) unsigned NOT NULL,
  `inventory` text,
  `resourcePriceUserConfig` varchar(1024) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `idxRootVolumeUsageVOaccountUuid` (`accountUuid`,`dateInLong`),
  KEY `idxRootVolumeUsageVOvolumeUuid` (`accountUuid`,`dateInLong`,`volumeUuid`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8;

CREATE TABLE `VmUsageHistoryVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `vmUuid` varchar(32) NOT NULL,
  `state` varchar(64) NOT NULL,
  `name` varchar(255) NOT NULL,
  `accountUuid` varchar(32) NOT NULL,
  `cpuNum` int(10) unsigned NOT NULL,
  `memorySize` bigint(20) unsigned NOT NULL,
  `rootVolumeSize` bigint(20) unsigned NOT NULL,
  `dateInLong` bigint(20) unsigned NOT NULL,
  `inventory` text,
  `resourcePriceUserConfig` varchar(1024) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `idxVmUsageVOaccountUuid` (`accountUuid`,`dateInLong`),
  KEY `idxVmUuid` (`vmUuid`) USING BTREE,
  KEY `idxVmUsageVOvmUuid` (`accountUuid`, `dateInLong`, `vmUuid`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8;

CREATE TABLE `PciDeviceUsageHistoryVO`(
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `pciDeviceUuid` varchar(32) NOT NULL,
  `vendorId` varchar(64) NOT NULL,
  `deviceId` varchar(64) NOT NULL,
  `subvendorId` varchar(64) DEFAULT NULL,
  `subdeviceId` varchar(64) DEFAULT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `vmUuid` varchar(32) NOT NULL,
  `vmName` varchar(255) DEFAULT NULL,
  `status` varchar(64) NOT NULL,
  `accountUuid` varchar(32) NOT NULL,
  `dateInLong` bigint(20) unsigned NOT NULL,
  `inventory` text,
  `resourcePriceUserConfig` varchar(1024) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `idxPciDeviceUsageVOaccountUuid` (`accountUuid`,`dateInLong`),
  KEY `idxPciDeviceUsageVOpciDeviceUuid` (`accountUuid`, `dateInLong`, `pciDeviceUuid`)
)ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PubIpVipBandwidthUsageHistoryVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `vipUuid` varchar(32) NOT NULL,
  `vipName` varchar(255) DEFAULT NULL,
  `vipIp` varchar(128) NOT NULL,
  `bandwidthOut` bigint(20) unsigned NOT NULL,
  `bandwidthIn` bigint(20) unsigned NOT NULL,
  `l3NetworkUuid` varchar(64) NOT NULL,
  `vipStatus` varchar(64) NOT NULL,
  `accountUuid` varchar(32) NOT NULL,
  `dateInLong` bigint(20) unsigned NOT NULL,
  `inventory` text,
  `resourcePriceUserConfig` varchar(1024) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `idxPubIpVipBandwidthUsageVOaccountUuid` (`accountUuid`,`dateInLong`),
  KEY `idxPubIpVipBandwidthUsageVOvipUuid` (`accountUuid`, `dateInLong`, `vipUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `PubIpVmNicBandwidthUsageHistoryVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `vmNicUuid` varchar(32) NOT NULL,
  `vmInstanceUuid` varchar(32) DEFAULT NULL,
  `bandwidthOut` bigint(20) unsigned NOT NULL,
  `bandwidthIn` bigint(20) unsigned NOT NULL,
  `vmNicIp` varchar(128) DEFAULT NULL,
  `vmNicStatus` varchar(64) NOT NULL,
  `l3NetworkUuid` varchar(64) NOT NULL,
  `accountUuid` varchar(32) NOT NULL,
  `dateInLong` bigint(20) unsigned NOT NULL,
  `inventory` text,
  `resourcePriceUserConfig` varchar(1024) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `idxPubIpVmNicBandwidthUsageVOaccountUuid` (`accountUuid`,`dateInLong`),
  KEY  `idxPubIpVmNicBandwidthUsageVOvmNicUuid` (`accountUuid`, `dateInLong`, `vmNicUuid`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8;

INSERT INTO ResourceVO (`uuid`, `resourceName`, `resourceType`, `concreteResourceType`) SELECT t.uuid, t.resourceName, "PriceVO", "org.zstack.billing.PriceVO" FROM PriceVO t;