ALTER TABLE `zstack`.`VmInstancePciDeviceSpecRefVO` ADD UNIQUE INDEX(`vmInstanceUuid`,`pciSpecUuid`);
ALTER TABLE `zstack`.`VmInstancePciSpecDeviceRefVO` ADD UNIQUE INDEX(`vmInstanceUuid`,`pciSpecUuid`, `pciDeviceUuid`);

DELIMITER $$
CREATE PROCEDURE handleLegacyPciSpecUuidTags()
    BEGIN
        DECLARE done INT DEFAULT FALSE;
        DECLARE tagUuid VARCHAR(32);
        DECLARE vmInstanceUuid VARCHAR(32);
        DECLARE pciSpecUuidTag VARCHAR(64);
        DECLARE pciSpecUuid VARCHAR(32);
        DECLARE pciDeviceUuid VARCHAR(32);
        DECLARE autoReleaseTagUuid VARCHAR(32);
        DEClARE cur CURSOR FOR SELECT `uuid`, `resourceUuid`, `tag` FROM `zstack`.`SystemTagVO`
            WHERE `resourceType` = 'VmInstanceVO' AND `tag` LIKE 'pciSpecUuid::%';
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
        OPEN cur;
        read_loop: LOOP
            FETCH cur INTO tagUuid, vmInstanceUuid, pciSpecUuidTag;
            IF done THEN
                LEAVE read_loop;
            END IF;

            -- create records in VmInstancePciDeviceSpecRefVO
            SET pciSpecUuid = substring(pciSpecUuidTag, LENGTH('pciSpecUuid::') + 1);
            IF pciSpecUuid IS NOT NULL THEN
                INSERT IGNORE INTO `zstack`.`VmInstancePciDeviceSpecRefVO` (`vmInstanceUuid`, `pciSpecUuid`, `pciDeviceNumber`, `lastOpDate`, `createDate`)
                VALUES (vmInstanceUuid, pciSpecUuid, 1, NOW(), NOW());
            END IF;

            -- in case done is set to TRUE because this query return null
            SELECT pci.uuid INTO pciDeviceUuid FROM `zstack`.`PciDeviceVO` pci WHERE pci.vmInstanceUuid = vmInstanceUuid AND pci.type = 'GPU_Video_Controller' LIMIT 1;
            IF done THEN
                SET pciDeviceUuid = NULL;
                SET done = FALSE;
            END IF;

            -- create records in VmInstancePciSpecDeviceRefVO
            IF pciDeviceUuid IS NOT NULL THEN
                INSERT IGNORE INTO `zstack`.`VmInstancePciSpecDeviceRefVO` (`vmInstanceUuid`, `pciSpecUuid`, `pciDeviceUuid`, `lastOpDate`, `createDate`)
                VALUES (vmInstanceUuid, pciSpecUuid, pciDeviceUuid, NOW(), NOW());
            END IF;

            -- create autoReleaseSpecReleatedPhysicalPciDevice tag for vm
            SET autoReleaseTagUuid = REPLACE(UUID(), '-', '');
            INSERT INTO `zstack`.`SystemTagVO` (`uuid`, `resourceUuid`, `resourceType`, `inherent`, `type`, `tag`, `createDate`, `lastOpDate`)
                VALUES (autoReleaseTagUuid, vmInstanceUuid, 'VmInstanceVO', 0, 'System', 'autoReleaseSpecReleatedPhysicalPciDevice', NOW(), NOW());

            -- delete legacy pciSpecUuid tag
            DELETE FROM `zstack`.`SystemTagVO` WHERE `uuid` = tagUuid;
        END LOOP;
        CLOSE cur;
        SELECT CURTIME();
    END $$
DELIMITER ;

SET FOREIGN_KEY_CHECKS = 0;
call handleLegacyPciSpecUuidTags();
SET FOREIGN_KEY_CHECKS = 1;
DROP PROCEDURE IF EXISTS handleLegacyPciSpecUuidTags;