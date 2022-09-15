CREATE TABLE `zstack`.`VolumeHostRefVO` (
    `volumeUuid` varchar(32) NOT NULL UNIQUE,
    `hostUuid` varchar(32) NOT NULL,
    `mountPath` varchar(512) NOT NULL,
    `device` varchar(512) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`volumeUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
ALTER TABLE `zstack`.`VolumeHostRefVO` ADD CONSTRAINT `fkVolumeHostRefVOHostEO`
FOREIGN KEY (`hostUuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE;