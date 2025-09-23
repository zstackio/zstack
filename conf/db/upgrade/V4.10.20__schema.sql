CREATE TABLE IF NOT EXISTS `zstack`.`SoftwarePackageVO` (
    `uuid` char(32) NOT NULL UNIQUE,
    `name` varchar(255) NOT NULL,
    `hostUuid` char(32),
    `managementNodeUuid` char(32),
    `installPath` varchar(1024),
    `unzipInstallPath` varchar(1024),
    `type` varchar(1024),
    `md5sum` char(32),
    `status` char(32),
    `size` bigint unsigned,
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;