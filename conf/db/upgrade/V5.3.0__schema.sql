CREATE TABLE IF NOT EXISTS `zstack`.`HbaDeviceVO` (
    `uuid` varchar(32) not null unique,
    `hostUuid` varchar(32) default null,
    `name` varchar(255) default null,
    `hbaType`  varchar(64) default null,
    `createDate` timestamp not null default '0000-00-00 00:00:00',
    `lastOpDate` timestamp not null default CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fkHBADeviceVOHostVO FOREIGN KEY (hostUuid) REFERENCES HostEO (uuid) ON DELETE CASCADE,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`FcHbaDeviceVO` (
    `uuid` varchar(32) not null unique,
    `portName` varchar(255) default null,
    `portState`  varchar(64) default null,
    `supportedSpeeds`  varchar(255) default null,
    `speed`  varchar(255) default null,
    `symbolicName`  varchar(255) default null,
    `supportedClasses`  varchar(255) default null,
    CONSTRAINT fkFcHbaDeviceVO FOREIGN KEY (uuid) REFERENCES HbaDeviceVO (uuid) ON DELETE CASCADE,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
