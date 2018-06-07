CREATE TABLE `zstack`.`SharedBlockGroupVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `sharedBlockGroupType` varchar(128) NOT NULL,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `zstack`.`SharedBlockVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `sharedBlockGroupUuid` varchar(32) NOT NULL,
    `type` varchar(128) NOT NULL,
    `diskUuid` varchar(64) NOT NULL UNIQUE,
    `name` varchar(255) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `state` varchar(64) NOT NULL,
    `status` varchar(64) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    CONSTRAINT `fkSharedBlockVOSharedBlockGroupVO` FOREIGN KEY (`sharedBlockGroupUuid`) REFERENCES `zstack`.`SharedBlockGroupVO` (`uuid`) ON DELETE CASCADE,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `zstack`.`SharedBlockGroupPrimaryStorageHostRefVO` (
    `primaryStorageUuid` varchar(32) NOT NULL,
    `hostUuid` varchar(32) NOT NULL,
    `status` varchar(32) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    `hostId` INT NOT NULL,
    CONSTRAINT `fkSharedBlockGroupPrimaryStorageHostRefVOPrimaryStorageEO` FOREIGN KEY (`primaryStorageUuid`) REFERENCES `zstack`.`PrimaryStorageEO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkSharedBlockGroupPrimaryStorageHostRefVOHostEO` FOREIGN KEY (`hostUuid`) REFERENCES `zstack`.`HostEO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `ukSharedBlockGroupPrimaryStorageHostRefVO` UNIQUE (`primaryStorageUuid`,`hostId`),
    PRIMARY KEY (`primaryStorageUuid`, `hostUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
