ALTER TABLE `zstack`.`InstanceOfferingEO` ADD COLUMN `reservedMemorySize` bigint unsigned DEFAULT 0;
ALTER TABLE `zstack`.`VmInstanceEO` ADD COLUMN `reservedMemorySize` bigint unsigned DEFAULT 0;

DROP VIEW IF EXISTS `zstack`.`VmInstanceVO`;
CREATE VIEW `zstack`.`VmInstanceVO` AS SELECT uuid, name, description, zoneUuid, clusterUuid, imageUuid, hostUuid, internalId, lastHostUuid, instanceOfferingUuid, rootVolumeUuid, defaultL3NetworkUuid, type, hypervisorType, cpuNum, cpuSpeed, memorySize, reservedMemorySize, platform, guestOsType, allocatorStrategy, createDate, lastOpDate, state, architecture FROM `zstack`.`VmInstanceEO` WHERE deleted IS NULL;
DROP VIEW IF EXISTS `zstack`.`InstanceOfferingVO`;
CREATE VIEW `zstack`.`InstanceOfferingVO` AS SELECT uuid, name, description, cpuNum, cpuSpeed, memorySize, reservedMemorySize, allocatorStrategy, sortKey, state, createDate, lastOpDate, type, duration FROM `zstack`.`InstanceOfferingEO` WHERE deleted IS NULL;
