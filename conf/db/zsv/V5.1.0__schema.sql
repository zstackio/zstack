-- Volume LUKS encryption flag (API opt-in + EncryptedResourceKeyRefVO binding)

ALTER TABLE `zstack`.`VolumeEO` ADD COLUMN `encrypted` tinyint(1) NOT NULL DEFAULT 0;
ALTER TABLE `zstack`.`VolumeSnapshotEO` ADD COLUMN `encrypted` tinyint(1) NOT NULL DEFAULT 0;
ALTER TABLE `zstack`.`VmInstanceEO` ADD COLUMN `encrypted` tinyint(1) NOT NULL DEFAULT 0;

DROP VIEW IF EXISTS `zstack`.`VolumeVO`;
CREATE VIEW `zstack`.`VolumeVO` AS
SELECT uuid, name, description, primaryStorageUuid, vmInstanceUuid, diskOfferingUuid,
       rootImageUuid, installPath, type, status, size, actualSize, deviceId, format, state, createDate, lastOpDate,
       isShareable, volumeQos, lastVmInstanceUuid, lastDetachDate, lastAttachDate, protocol, encrypted
FROM `zstack`.`VolumeEO`
WHERE deleted IS NULL;

DROP VIEW IF EXISTS `zstack`.`VolumeSnapshotVO`;
CREATE VIEW `zstack`.`VolumeSnapshotVO` AS
SELECT uuid, name, description, type, volumeUuid, format, treeUuid, parentUuid,
       primaryStorageUuid, primaryStorageInstallPath, distance, size, latest,
       fullSnapshot, encrypted, volumeType, state, status, createDate, lastOpDate
FROM `zstack`.`VolumeSnapshotEO`
WHERE deleted IS NULL;

DROP VIEW IF EXISTS `zstack`.`VmInstanceVO`;
CREATE VIEW `zstack`.`VmInstanceVO` AS
SELECT uuid, name, description, zoneUuid, clusterUuid, imageUuid, hostUuid, internalId,
       lastHostUuid, instanceOfferingUuid, rootVolumeUuid, defaultL3NetworkUuid, type,
       hypervisorType, cpuNum, cpuSpeed, memorySize, reservedMemorySize, platform,
       guestOsType, allocatorStrategy, createDate, lastOpDate, state, architecture, encrypted
FROM `zstack`.`VmInstanceEO`
WHERE deleted IS NULL;
