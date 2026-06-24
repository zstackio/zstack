-- Do not filter by architecture here. The upgrade preserves previous Windows VM behavior across all architectures;
-- current kvmagent consumption is still gated by host CPU architecture at start time.
INSERT INTO `zstack`.`ResourceConfigVO` (`uuid`, `name`, `description`, `category`, `value`, `resourceUuid`, `resourceType`, `lastOpDate`, `createDate`)
SELECT REPLACE(UUID(), '-', ''), 'vm.cpu.hardwareVirtualization', 'enable or disable hardware virtualization feature in Windows guest cpuid',
       'kvm', 'true', vm.`uuid`, 'VmInstanceVO', NOW(), NOW()
FROM `zstack`.`VmInstanceVO` vm
WHERE (vm.`platform` IN ('Windows', 'WindowsVirtio')
    OR LOWER(IFNULL(vm.`guestOsType`, '')) LIKE '%windows%')
  AND NOT EXISTS (
      SELECT 1
      FROM `zstack`.`ResourceConfigVO` rc
      WHERE rc.`resourceUuid` = vm.`uuid`
        AND rc.`category` = 'kvm'
        AND rc.`name` = 'vm.cpu.hardwareVirtualization'
  );

UPDATE `zstack`.`PciDeviceMdevSpecRefVO` keepRef
JOIN (
    SELECT `pciDeviceUuid`, `mdevSpecUuid`, MAX(`id`) AS `keepId`, MAX(`effective`) AS `effective`
    FROM `zstack`.`PciDeviceMdevSpecRefVO`
    GROUP BY `pciDeviceUuid`, `mdevSpecUuid`
) groupedRef ON keepRef.`id` = groupedRef.`keepId`
SET keepRef.`effective` = groupedRef.`effective`;

DELETE duplicateRef FROM `zstack`.`PciDeviceMdevSpecRefVO` duplicateRef
JOIN `zstack`.`PciDeviceMdevSpecRefVO` keepRef
  ON duplicateRef.`pciDeviceUuid` = keepRef.`pciDeviceUuid`
 AND duplicateRef.`mdevSpecUuid` = keepRef.`mdevSpecUuid`
 AND duplicateRef.`id` < keepRef.`id`;

UPDATE `zstack`.`PciDeviceMdevSpecRefVO` ref
JOIN (
    SELECT activeRef.`id`
    FROM `zstack`.`PciDeviceMdevSpecRefVO` activeRef
    JOIN (
        SELECT `pciDeviceUuid`
        FROM `zstack`.`PciDeviceMdevSpecRefVO`
        WHERE `effective` = 1
        GROUP BY `pciDeviceUuid`
        HAVING COUNT(*) > 1
    ) duplicatedPci ON activeRef.`pciDeviceUuid` = duplicatedPci.`pciDeviceUuid`
    WHERE activeRef.`effective` = 1
      AND NOT EXISTS (
          SELECT 1
          FROM `zstack`.`MdevDeviceVO` mdev
          WHERE mdev.`parentUuid` = activeRef.`pciDeviceUuid`
            AND mdev.`mdevSpecUuid` = activeRef.`mdevSpecUuid`
      )
) staleRef ON ref.`id` = staleRef.`id`
SET ref.`effective` = 0;

UPDATE `zstack`.`PciDeviceMdevSpecRefVO` oldRef
JOIN `zstack`.`PciDeviceMdevSpecRefVO` newRef
  ON oldRef.`pciDeviceUuid` = newRef.`pciDeviceUuid`
 AND oldRef.`effective` = 1
 AND newRef.`effective` = 1
 AND oldRef.`id` < newRef.`id`
SET oldRef.`effective` = 0;

DROP PROCEDURE IF EXISTS addPciDeviceMdevSpecRefUniqueKey;
DELIMITER $$
CREATE PROCEDURE addPciDeviceMdevSpecRefUniqueKey()
BEGIN
    DECLARE index_count INT DEFAULT 0;

    SELECT COUNT(*) INTO index_count
    FROM information_schema.statistics
    WHERE table_schema = 'zstack'
      AND table_name = 'PciDeviceMdevSpecRefVO'
      AND index_name = 'ukPciDeviceMdevSpecRefVOPciUuidMdevSpecUuid';

    IF index_count < 1 THEN
        ALTER TABLE `zstack`.`PciDeviceMdevSpecRefVO`
            ADD UNIQUE KEY `ukPciDeviceMdevSpecRefVOPciUuidMdevSpecUuid` (`pciDeviceUuid`, `mdevSpecUuid`);
    END IF;

    SELECT CURTIME();
END $$
DELIMITER ;
CALL addPciDeviceMdevSpecRefUniqueKey();
DROP PROCEDURE IF EXISTS addPciDeviceMdevSpecRefUniqueKey;

-- ZCF-4158: Store SCIM event application state.
CREATE TABLE IF NOT EXISTS `zstack`.`ScimEventVO` (
    `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'uuid',
    `clientId` varchar(128) NOT NULL DEFAULT 'default',
    `eventId` varchar(255) NOT NULL,
    `resourceType` varchar(64) NOT NULL,
    `resourceId` varchar(255) NOT NULL,
    `resourceVersion` bigint NOT NULL,
    `operation` varchar(32) NOT NULL,
    `status` varchar(32) NOT NULL,
    `payloadHash` varchar(128) DEFAULT NULL,
    `errorMessage` text DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00',
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `ukScimEventVOClientEvent` (`clientId`, `eventId`),
    KEY `idxScimEventVOResourceVersion` (`clientId`, `resourceType`, `resourceId`, `resourceVersion`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- SUG-2795: listener-level TCP IPVS data plane and forward mode.
CALL ADD_COLUMN('LoadBalancerListenerVO', 'data_plane', 'VARCHAR(32)', 1, NULL);
CALL ADD_COLUMN('LoadBalancerListenerVO', 'forward_mode', 'VARCHAR(32)', 1, NULL);

UPDATE `zstack`.`LoadBalancerListenerVO`
SET data_plane = 'ipvs'
WHERE protocol = 'udp' AND data_plane IS NULL;

UPDATE `zstack`.`LoadBalancerListenerVO`
SET data_plane = 'haproxy'
WHERE data_plane IS NULL;
