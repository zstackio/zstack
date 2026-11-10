CREATE TABLE IF NOT EXISTS `zstack`.`CryptoDeviceVO` (
    `uuid` varchar(32) NOT NULL,
    `qualification` varchar(32) NOT NULL DEFAULT 'DriverUnavailable',
    `driverVersion` varchar(255) DEFAULT NULL,
    `healthStatus` varchar(32) DEFAULT NULL,
    `serialNumber` varchar(255) DEFAULT NULL,
    `firmwareVersion` varchar(255) DEFAULT NULL,
    `healthReason` varchar(255) DEFAULT NULL,
    `recoveryReadyAt` datetime(3) DEFAULT NULL,
    `supportedVfCounts` varchar(255) NOT NULL DEFAULT '[]',
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkCryptoDeviceVOPciDeviceVO` FOREIGN KEY (`uuid`) REFERENCES `PciDeviceVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE INDEX idxPciDeviceVOhostIommuGroup ON PciDeviceVO (hostUuid, iommuGroup);

ALTER TABLE HaStrategyConditionVO
    ADD COLUMN `type` varchar(32) NOT NULL DEFAULT 'Fencer',
    MODIFY COLUMN `fencerName` varchar(256) DEFAULT NULL;
