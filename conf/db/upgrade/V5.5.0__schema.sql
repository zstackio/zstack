-- -----------------------------------
--  BEGIN OF HYGON CCP DEVICE VIRTUALIZATION
-- -----------------------------------
CREATE TABLE IF NOT EXISTS `zstack`.`HygonCcpDeviceVO` (
    `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'uuid',
    `name` varchar(255) NOT NULL,
    `description` text DEFAULT NULL,
    `hostUuid` varchar(32) NOT NULL,
    `pciBdf` varchar(32) NOT NULL,
    `deviceType` varchar(32) NOT NULL,
    `deviceId` varchar(32) NOT NULL,
    `driverStatus` varchar(32) NOT NULL,
    `isMasterPsp` tinyint(1) DEFAULT 0,
    `vendorIdx` INT DEFAULT NULL,
    `state` varchar(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    INDEX `idxHygonCCPDeviceVOhostUuid` (`hostUuid`),
    INDEX `idxHygonCCPDeviceVOdeviceType` (`deviceType`),
    INDEX `idxHygonCCPDeviceVOpciBdf` (`pciBdf`),
    CONSTRAINT `fkHygonCCPDeviceVOHostEO` FOREIGN KEY (`hostUuid`) REFERENCES `zstack`.`HostEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`HygonCcpMdevVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(128) NOT NULL,
    `description` text DEFAULT NULL,
    `hostUuid` varchar(32) NOT NULL,
    `ccpDeviceUuid` varchar(32) NOT NULL,
    `mdevUuid` varchar(64) NOT NULL UNIQUE,
    `vendorIdx` INT DEFAULT NULL,
    `useFlag` tinyint(1) NOT NULL DEFAULT 0,
    `vmInstanceUuid` varchar(32) DEFAULT NULL,
    `status` varchar(32) NOT NULL,
    `state` varchar(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    INDEX `idxHygonCcpMdevVOhostUuid` (`hostUuid`),
    INDEX `idxHygonCcpMdevVOccpDeviceUuid` (`ccpDeviceUuid`),
    INDEX `idxHygonCcpMdevVOmdevUuid` (`mdevUuid`),
    INDEX `idxHygonCcpMdevVOvmInstanceUuid` (`vmInstanceUuid`),
    INDEX `idxHygonCcpMdevVOuseFlag` (`useFlag`),
    CONSTRAINT `fkHygonCcpMdevVOHostEO` FOREIGN KEY (`hostUuid`) REFERENCES `zstack`.`HostEO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkHygonCcpMdevVOHygonCCPDeviceVO` FOREIGN KEY (`ccpDeviceUuid`) REFERENCES `zstack`.`HygonCcpDeviceVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkHygonCcpMdevVOVmInstanceEO` FOREIGN KEY (`vmInstanceUuid`) REFERENCES `zstack`.`VmInstanceEO` (`uuid`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
-- ---------------------------------
--  END OF HYGON CCP DEVICE VIRTUALIZATION
-- ---------------------------------
