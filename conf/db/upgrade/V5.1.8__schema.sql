CREATE TABLE IF NOT EXISTS `zstack`.`GpuDeviceVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `serialNumber` varchar(255),
    `memory` bigint unsigned NULL DEFAULT 0,
    `power` bigint unsigned NULL DEFAULT 0,
    `isDriverLoaded` TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkGpuDeviceInfoVOPciDeviceVO` FOREIGN KEY (`uuid`) REFERENCES `PciDeviceVO` (`uuid`) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CALL ADD_COLUMN('PciDeviceVO', 'vendor', 'VARCHAR(128)', 1, NULL);
CALL ADD_COLUMN('PciDeviceVO', 'device', 'VARCHAR(128)', 1, NULL);
CALL ADD_COLUMN('PciDeviceSpecVO', 'vendor', 'VARCHAR(128)', 1, NULL);
CALL ADD_COLUMN('PciDeviceSpecVO', 'device', 'VARCHAR(128)', 1, NULL);
CALL ADD_COLUMN('MdevDeviceVO', 'vendor', 'VARCHAR(128)', 1, NULL);

DROP PROCEDURE IF EXISTS `MdevDeviceAddVendor`;
DELIMITER $$
CREATE PROCEDURE MdevDeviceAddVendor()
    BEGIN
        DECLARE vendor VARCHAR(128);
        DECLARE pciDeviceUuid VARCHAR(32);
        DECLARE done INT DEFAULT FALSE;
        DECLARE cur CURSOR FOR SELECT pci.uuid, pci.vendor FROM PciDeviceVO pci;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

        OPEN cur;
        read_loop: LOOP
            FETCH cur INTO vendor, pciDeviceUuid;
            IF done THEN
                LEAVE read_loop;
            END IF;
            UPDATE MdevDeviceVO SET vendor = vendor WHERE parentUuid = pciDeviceUuid;
        END LOOP;
        CLOSE cur;
        SELECT CURTIME();
    END $$
DELIMITER ;
call MdevDeviceAddVendor;
DROP PROCEDURE IF EXISTS `MdevDeviceAddVendor`;

CREATE TABLE IF NOT EXISTS `HostHwMonitorStatusVO`
(
    `uuid` varchar(32)  NOT NULL UNIQUE,
    `cpuStatus` TINYINT(1)  unsigned DEFAULT 1,
    `memoryStatus` TINYINT(1) unsigned DEFAULT 1,
    `diskStatus` TINYINT(1) unsigned DEFAULT 1,
    `nicStatus` TINYINT(1) unsigned DEFAULT 1,
    `gpuStatus` TINYINT(1) unsigned DEFAULT 1,
    `powerSupplyStatus` TINYINT(1) unsigned DEFAULT 1,
    `fanStatus` TINYINT(1) unsigned DEFAULT 1,
    `raidStatus` TINYINT(1) unsigned DEFAULT 1,
    `temperatureStatus` TINYINT(1) unsigned DEFAULT 1,
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkHostHwMonitorStatusVO` FOREIGN KEY (`uuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE
    ) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`BareMetal2ChassisPciDeviceVO` (
    `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'uuid',
    `chassisUuid` varchar(32) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `type` varchar(32) NOT NULL,
    `pciDeviceAddress` varchar(32) NOT NULL,
    `vendorId` varchar(64) NOT NULL,
    `deviceId` varchar(64) NOT NULL,
    `subvendorId` varchar(64) DEFAULT NULL,
    `subdeviceId` varchar(64) DEFAULT NULL,
    `iommuGroup` varchar(255) DEFAULT NULL,
    `name` varchar(255) NOT NULL,
    `vendor` varchar(128) DEFAULT NULL,
    `device` varchar(128) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`BareMetal2ChassisGpuDeviceVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `serialNumber` varchar(255),
    `memory` varchar(255),
    `power` varchar(255),
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkBm2ChassisGpuDeviceVOBm2ChassisPciDeviceVO` FOREIGN KEY (`uuid`) REFERENCES `BareMetal2ChassisPciDeviceVO` (`uuid`) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
