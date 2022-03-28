ALTER TABLE VolumeEO ADD COLUMN lastVmInstanceUuid VARCHAR(32) DEFAULT NULL;
ALTER TABLE VolumeEO ADD COLUMN lastDetachDate VARCHAR(32) DEFAULT NULL;

DROP VIEW IF EXISTS `zstack`.`VolumeVO`;
CREATE VIEW `zstack`.`VolumeVO` AS SELECT uuid, name, description, primaryStorageUuid, vmInstanceUuid, diskOfferingUuid, rootImageUuid, installPath, type, status, size, actualSize, deviceId, format, state, createDate, lastOpDate, isShareable, volumeQos, lastVmInstanceUuid, lastDetachDate FROM `zstack`.`VolumeEO` WHERE deleted IS NULL;

