ALTER TABLE `zstack`.`HostEO` ADD COLUMN architecture varchar(32) DEFAULT NULL;

UPDATE HostEO h
INNER JOIN (
SELECT
substring_index(tag,"::",-1) arch,
resourceUuid ru
FROM
SystemTagVO
WHERE
tag like "cpuArchitecture%"
) tags on tags.ru = h.uuid
SET h.architecture = tags.arch;

UPDATE HostEO set architecture = "x86_64" WHERE architecture IS NULL;
DROP VIEW IF EXISTS `zstack`.`HostVO`;
CREATE VIEW `zstack`.`HostVO` AS SELECT uuid, zoneUuid, clusterUuid, name, description, managementIp, hypervisorType, state, status, createDate, lastOpDate, architecture FROM `zstack`.`HostEO` WHERE deleted IS NULL;

ALTER TABLE `zstack`.`VmInstanceEO` ADD COLUMN architecture varchar(32) DEFAULT NULL;

UPDATE VmInstanceEO v
INNER JOIN HostEO h
ON (v.hostUuid = h.uuid OR v.lastHostUuid = h.uuid)
set v.architecture = h.architecture;

DROP VIEW IF EXISTS `zstack`.`VmInstanceVO`;
CREATE VIEW `zstack`.`VmInstanceVO` AS SELECT uuid, name, description, zoneUuid, clusterUuid, imageUuid, hostUuid, internalId, lastHostUuid, instanceOfferingUuid, rootVolumeUuid, defaultL3NetworkUuid, type, hypervisorType, cpuNum, cpuSpeed, memorySize, platform, allocatorStrategy, createDate, lastOpDate, state, architecture FROM `zstack`.`VmInstanceEO` WHERE deleted IS NULL;
