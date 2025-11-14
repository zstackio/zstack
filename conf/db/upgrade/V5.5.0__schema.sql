CALL ADD_COLUMN('VmVfNicVO', 'secondaryPciDeviceUuid', 'VARCHAR(32)', 1, NULL);
ALTER TABLE `zstack`.`VmVfNicVO` ADD CONSTRAINT `fkVmVfNicVOSecondaryPciDeviceVO` FOREIGN KEY (`secondaryPciDeviceUuid`) REFERENCES `zstack`.`PciDeviceVO` (`uuid`) ON DELETE SET NULL;

CALL ADD_COLUMN('PciDeviceVO', 'dependentDevices', 'varchar(255)', 1, NULL);

CREATE TABLE IF NOT EXISTS `zstack`.`ExternalPrimaryStorageSpaceVO`
(
    uuid                      VARCHAR(32) NOT NULL,
    primaryStorageUuid        VARCHAR(32),
    locationUrl               VARCHAR(255),
    type                      VARCHAR(255),
    name                      VARCHAR(255),
    availableCapacity         BIGINT,
    totalCapacity             BIGINT,
    availablePhysicalCapacity BIGINT,
    totalPhysicalCapacity     BIGINT,
    `lastOpDate`              TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate`              TIMESTAMP,
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkExternalPrimaryStorageSpaceVOPrimaryStorageEO` FOREIGN KEY (`primaryStorageUuid`) REFERENCES `zstack`.`PrimaryStorageEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


DROP PROCEDURE IF EXISTS UpdateVolumeInstallPathForZbs;

DELIMITER $$
CREATE PROCEDURE UpdateVolumeInstallPathForZbs()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE psUuid VARCHAR(32);
    DECLARE cur CURSOR FOR
        SELECT uuid FROM `zstack`.`ExternalPrimaryStorageVO` WHERE identity = 'zbs';
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO psUuid;
        IF done THEN
            LEAVE read_loop;
        END IF;

        UPDATE `zstack`.`VolumeEO`
        SET installPath = CONCAT('zbs://', SUBSTRING_INDEX(SUBSTRING(installPath, 5), '/', -2))
        WHERE primaryStorageUuid = psUuid
          AND installPath LIKE 'cbd:%/%/%';

        UPDATE `zstack`.`VolumeSnapshotEO`
        SET primaryStorageInstallPath = CONCAT('zbs://', SUBSTRING_INDEX(SUBSTRING(primaryStorageInstallPath, 5), '/', -2))
        WHERE primaryStorageUuid = psUuid
          AND primaryStorageInstallPath LIKE 'cbd:%/%/%';

        UPDATE `zstack`.`ImageCacheVO`
        SET installUrl = CONCAT('zbs://', SUBSTRING_INDEX(SUBSTRING(installUrl, 5), '/', -2))
        WHERE primaryStorageUuid = psUuid
          AND installUrl LIKE 'cbd:%/%/%';

        UPDATE `zstack`.`ImageCacheShadowVO`
        SET installUrl = CONCAT('zbs://', SUBSTRING_INDEX(SUBSTRING(installUrl, 5), '/', -2))
        WHERE primaryStorageUuid = psUuid
          AND installUrl LIKE 'cbd:%/%/%';
    END LOOP;
    CLOSE cur;
END $$

DELIMITER ;

CALL UpdateVolumeInstallPathForZbs();
