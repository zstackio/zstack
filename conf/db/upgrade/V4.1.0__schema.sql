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


ALTER TABLE `zstack`.`ImageEO` ADD COLUMN `virtio` boolean DEFAULT TRUE;
UPDATE `zstack`.`ImageEO` SET virtio = FALSE, guestOsType = "Windows" WHERE platform="Windows";
UPDATE `zstack`.`ImageEO` SET platform = "Windows", virtio = TRUE, guestOsType = "Windows" WHERE platform = "WindowsVirtio";
UPDATE `zstack`.`ImageEO` SET virtio = TRUE, guestOsType = "Linux" WHERE platform = "Linux";
UPDATE `zstack`.`ImageEO` SET virtio = FALSE, guestOsType = "Other" WHERE platform = "Other";
UPDATE `zstack`.`ImageEO` SET platform = "Other", virtio = TRUE, guestOsType = "Other" WHERE platform = "Paravirtualization";

DROP VIEW IF EXISTS `zstack`.`ImageVO`;
CREATE VIEW `zstack`.`ImageVO` AS SELECT uuid, name, description, status, state, size, actualSize, md5Sum, platform, type, format, url, `system`, mediaType, guestOsType, architecture, virtio, createDate, lastOpDate FROM `zstack`.`ImageEO` WHERE deleted IS NULL;

ALTER TABLE `zstack`.`VmInstanceEO` ADD COLUMN `guestOsType` varchar(255) DEFAULT NULL;
DROP VIEW IF EXISTS `zstack`.`VmInstanceVO`;
CREATE VIEW `zstack`.`VmInstanceVO` AS SELECT uuid, name, description, zoneUuid, clusterUuid, imageUuid, hostUuid, internalId, lastHostUuid, instanceOfferingUuid, rootVolumeUuid, defaultL3NetworkUuid, type, hypervisorType, cpuNum, cpuSpeed, memorySize, platform, guestOsType, allocatorStrategy, createDate, lastOpDate, state, architecture FROM `zstack`.`VmInstanceEO` WHERE deleted IS NULL;

CREATE TABLE IF NOT EXISTS `GuestOsCategoryVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `platform` VARCHAR(32) NOT NULL,
    `name` VARCHAR(32) NOT NULL,
    `version` VARCHAR(32),
    `osRelease` VARCHAR(32) NOT NULL,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT IGNORE INTO SystemTagVO (`uuid`, `resourceUuid`, `resourceType`, `inherent`, `type`, `tag`, `createDate`, `lastOpDate`)
SELECT REPLACE(UUID(),'-',''), t.uuid, 'VmInstanceVO', 0, 'System', 'driver::virtio', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP() FROM VmInstanceVO t WHERE t.platform IN ('Paravirtualization', 'WindowsVirtio', 'Linux');
UPDATE `zstack`.`VmInstanceVO` SET guestOsType = "Windows" WHERE platform="Windows";
UPDATE `zstack`.`VmInstanceVO` SET platform = "Windows", guestOsType = "Windows" WHERE platform = "WindowsVirtio";
UPDATE `zstack`.`VmInstanceVO` SET guestOsType = "Linux" WHERE platform = "Linux";
UPDATE `zstack`.`VmInstanceVO` SET guestOsType = "Other" WHERE platform = "Other";
UPDATE `zstack`.`VmInstanceVO` SET platform = "Other", guestOsType = "Other" WHERE platform = "Paravirtualization";