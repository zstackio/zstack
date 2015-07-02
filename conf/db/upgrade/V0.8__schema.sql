CREATE TABLE  `zstack`.`LocalStorageHostRefVO` (
    `primaryStorageUuid` varchar(32) NOT NULL UNIQUE,
    `hostUuid` varchar(32),
    `totalCapacity` bigint unsigned DEFAULT 0,
    `availableCapacity` bigint unsigned DEFAULT 0,
    `totalPhysicalCapacity` bigint unsigned DEFAULT 0,
    `availablePhysicalCapacity` bigint unsigned DEFAULT 0,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`primaryStorageUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`LocalStorageResourceRefVO` (
    `primaryStorageUuid` varchar(32) NOT NULL UNIQUE,
    `hostUuid` varchar(32),
    `resourceUuid` varchar(32),
    `resourceType` varchar(256),
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`primaryStorageUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
