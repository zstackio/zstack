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
    `size` bigint unsigned DEFAULT 0,
    `resourceUuid` varchar(32),
    `resourceType` varchar(256),
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`primaryStorageUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

# Foreign keys for table LocalStorageHostRefVO

ALTER TABLE LocalStorageHostRefVO ADD CONSTRAINT fkLocalStorageHostRefVOHostEO FOREIGN KEY (hostUuid) REFERENCES HostEO (uuid) ON DELETE CASCADE;
ALTER TABLE LocalStorageHostRefVO ADD CONSTRAINT fkLocalStorageHostRefVOPrimaryStorageEO FOREIGN KEY (primaryStorageUuid) REFERENCES PrimaryStorageEO (uuid) ON DELETE CASCADE;

# Foreign keys for table LocalStorageResourceRefVO

ALTER TABLE LocalStorageResourceRefVO ADD CONSTRAINT fkLocalStorageResourceRefVOHostEO FOREIGN KEY (hostUuid) REFERENCES HostEO (uuid) ON DELETE CASCADE;
ALTER TABLE LocalStorageResourceRefVO ADD CONSTRAINT fkLocalStorageResourceRefVOPrimaryStorageEO FOREIGN KEY (primaryStorageUuid) REFERENCES PrimaryStorageEO (uuid) ON DELETE CASCADE;

# Index for table LocalStorageHostRefVO

CREATE INDEX idxLocalStorageHostRefVOtotalCapacity ON LocalStorageHostRefVO (totalCapacity);
CREATE INDEX idxLocalStorageHostRefVOavailableCapacity ON LocalStorageHostRefVO (availableCapacity);
CREATE INDEX idxLocalStorageHostRefVOtotalPhysicalCapacity ON LocalStorageHostRefVO (totalPhysicalCapacity);
CREATE INDEX idxLocalStorageHostRefVOavailablePhysicalCapacity ON LocalStorageHostRefVO (availablePhysicalCapacity);
