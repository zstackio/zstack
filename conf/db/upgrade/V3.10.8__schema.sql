CREATE TABLE IF NOT EXISTS `zstack`.`VirtualRouterMetadataVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `zvrVersion` varchar(32) DEFAULT NULL,
    `vyosVersion` varchar(32) DEFAULT NULL,
    `kernelVersion` varchar(32) DEFAULT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkVirtualRouterMetadataVOVirtualRouterVmVO` FOREIGN KEY (`uuid`) REFERENCES `VirtualRouterVmVO` (`uuid`) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;