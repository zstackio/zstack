CREATE TABLE IF NOT EXISTS `zstack`.`GpuDeviceVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `serialNumber` varchar(255),
    `memory` varchar(255),
    `power` varchar(255),
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkGpuDeviceInfoVOPciDeviceVO` FOREIGN KEY (`uuid`) REFERENCES `PciDeviceVO` (`uuid`) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CALL ADD_COLUMN('PciDeviceVO', 'vendor', 'VARCHAR(128)', 1, NULL);
CALL ADD_COLUMN('PciDeviceVO', 'device', 'VARCHAR(128)', 1, NULL);
CALL ADD_COLUMN('PciDeviceSpecVO', 'vendor', 'VARCHAR(128)', 1, NULL);
CALL ADD_COLUMN('PciDeviceSpecVO', 'device', 'VARCHAR(128)', 1, NULL);
CALL ADD_COLUMN('MdevDeviceVO', 'vendor', 'VARCHAR(128)', 1, NULL);
