INSERT INTO AccountResourceRefVO (`accountUuid`, `ownerAccountUuid`, `resourceUuid`, `resourceType`, `permission`, `isShared`, `lastOpDate`, `createDate`, `concreteResourceType`) SELECT "36c27e8ff05c4780bf6d2fa65700f22e", "36c27e8ff05c4780bf6d2fa65700f22e", t.uuid, "VCenterVO", 2, 0, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP(), "org.zstack.vmware.VCenterVO" FROM VCenterVO t where t.uuid NOT IN (SELECT resourceUuid FROM AccountResourceRefVO);

CREATE TABLE `zstack`.`VolumeSnapshotGroupVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `name` VARCHAR(255) NOT NULL,
    `description` VARCHAR(2048) DEFAULT NULL,
    `vmInstanceUuid` VARCHAR(32) NOT NULL,
    `snapshotCount` int unsigned NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `zstack`.`VolumeSnapshotGroupRefVO` (
    `volumeSnapshotUuid` VARCHAR(32) NOT NULL UNIQUE,
    `volumeSnapshotGroupUuid` VARCHAR(32) NOT NULL,
    `snapshotDeleted` BOOLEAN NOT NULL,
    `deviceId` int unsigned NOT NULL,
    `volumeUuid` VARCHAR(32) NOT NULL,
    `volumeName` VARCHAR(256) NOT NULL,
    `volumeType` VARCHAR(32) NOT NULL,
    `volumeSnapshotName` varchar(256) DEFAULT NULL,
    `volumeSnapshotInstallPath` varchar(1024) DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY (`volumeSnapshotUuid`),
    CONSTRAINT `fkVolumeSnapshotGroupRefVOVolumeSnapshotGroupVO` FOREIGN KEY (`volumeSnapshotGroupUuid`) REFERENCES `VolumeSnapshotGroupVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

