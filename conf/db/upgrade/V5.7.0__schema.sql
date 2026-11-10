-- IB devices retain the existing PCI UUID, VM binding and account references.
-- PCI classification arrives from a host synchronization, never from upgrade-time guesses.
CREATE TABLE IF NOT EXISTS `zstack`.`IbDeviceVO` (
    `uuid` varchar(32) NOT NULL,
    `displayName` varchar(255) DEFAULT NULL,
    `allocateStatus` varchar(32) NOT NULL DEFAULT 'Unknown',
    `bindingSource` varchar(32) NOT NULL DEFAULT 'Unknown',
    `collectionStatus` varchar(32) NOT NULL DEFAULT 'Unknown',
    `lastCollectedAt` timestamp NULL DEFAULT NULL,
    `collectionError` text DEFAULT NULL,
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkIbDeviceVOPciDeviceVO` FOREIGN KEY (`uuid`)
        REFERENCES `zstack`.`PciDeviceVO` (`uuid`) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
