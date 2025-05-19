CREATE TABLE IF NOT EXISTS `zstack`.`ModelServiceImageVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `modelServiceUuid` varchar(32) NOT NULL,
    `cpuArchitecture` varchar(32) NOT NULL,
    `vmImageUuid` varchar(32) NULL,
    `dockerImage` varchar(255) NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkModelServiceImageVOModelServiceVO` FOREIGN KEY (`modelServiceUuid`) REFERENCES `ModelServiceVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CALL ADD_COLUMN('ModelServiceVO', 'gpuVendors', 'varchar(255)', 1, 'NULL');
CALL ADD_COLUMN('ModelServiceVO', 'cpuArchitectures', 'varchar(255)', 1, 'NULL');