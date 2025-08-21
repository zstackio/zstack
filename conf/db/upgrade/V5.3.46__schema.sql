DELETE FROM UserTagVO WHERE uuid = 'a4de80903e57422699fb05bd367a3cb4';

CALL ADD_COLUMN('PciDeviceSpecVO', 'allowResourceConfigWithMultipleDevices', 'tinyint(1)', 0, '1');

CALL ADD_COLUMN('GpuDeviceVO', 'opaque', 'MEDIUMTEXT', 1, NULL);

CALL ADD_COLUMN('ModelServiceInstanceVO', 'nodeRank', 'int', 1, 0);

CREATE TABLE IF NOT EXISTS `zstack`.`GpuDeviceSpecVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `memory` bigint unsigned NULL DEFAULT 0,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkGpuDeviceSpecVOPciDeviceSpecVO` FOREIGN KEY (`uuid`) REFERENCES `PciDeviceSpecVO` (`uuid`) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
