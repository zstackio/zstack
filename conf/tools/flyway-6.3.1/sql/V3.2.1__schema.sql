
CREATE INDEX idxRootVolumeUsageVOaccountUuid ON RootVolumeUsageVO(accountUuid, dateInLong);
CREATE INDEX idxDataVolumeUsageVOaccountUuid ON DataVolumeUsageVO(accountUuid, dateInLong);
CREATE INDEX idxSnapShotUsageVOaccountUuid ON SnapShotUsageVO(accountUuid, dateInLong);
CREATE INDEX idxPciDeviceUsageVOaccountUuid ON PciDeviceUsageVO(accountUuid, dateInLong);
CREATE INDEX idxPubIpVmNicBandwidthUsageVOaccountUuid ON PubIpVmNicBandwidthUsageVO(accountUuid, dateInLong);
CREATE INDEX idxPubIpVipBandwidthUsageVOaccountUuid ON PubIpVipBandwidthUsageVO(accountUuid, dateInLong);
