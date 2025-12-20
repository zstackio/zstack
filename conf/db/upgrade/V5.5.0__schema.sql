CALL ADD_COLUMN('VmVfNicVO', 'secondaryPciDeviceUuid', 'VARCHAR(32)', 1, NULL);
ALTER TABLE `zstack`.`VmVfNicVO` ADD CONSTRAINT `fkVmVfNicVOSecondaryPciDeviceVO` FOREIGN KEY (`secondaryPciDeviceUuid`) REFERENCES `zstack`.`PciDeviceVO` (`uuid`) ON DELETE SET NULL;

CALL ADD_COLUMN('LicenseAuthorizedCapacityVO', 'resourceInfo', 'text', 1, NULL);

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

ALTER TABLE `zstack`.`ExternalPrimaryStorageVO` MODIFY `addonInfo` TEXT DEFAULT NULL;

-- -----------------------------------
--  BEGIN OF HYGON CCP DEVICE VIRTUALIZATION
-- -----------------------------------
CREATE TABLE IF NOT EXISTS `zstack`.`HygonCcpDeviceVO` (
                                                           `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'uuid',
    `name` varchar(255) NOT NULL,
    `description` text DEFAULT NULL,
    `hostUuid` varchar(32) NOT NULL,
    `pciBdf` varchar(32) NOT NULL,
    `deviceType` varchar(32) NOT NULL,
    `deviceId` varchar(32) NOT NULL,
    `driverStatus` varchar(32) NOT NULL,
    `isMasterPsp` tinyint(1) DEFAULT 0,
    `vendorIdx` INT DEFAULT NULL,
    `state` varchar(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    INDEX `idxHygonCCPDeviceVOhostUuid` (`hostUuid`),
    INDEX `idxHygonCCPDeviceVOdeviceType` (`deviceType`),
    INDEX `idxHygonCCPDeviceVOpciBdf` (`pciBdf`),
    CONSTRAINT `fkHygonCCPDeviceVOHostEO` FOREIGN KEY (`hostUuid`) REFERENCES `zstack`.`HostEO` (`uuid`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`HygonCcpMdevVO` (
                                                         `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(128) NOT NULL,
    `description` text DEFAULT NULL,
    `hostUuid` varchar(32) NOT NULL,
    `ccpDeviceUuid` varchar(32) NOT NULL,
    `mdevUuid` varchar(64) NOT NULL UNIQUE,
    `vendorIdx` INT DEFAULT NULL,
    `useFlag` tinyint(1) NOT NULL DEFAULT 0,
    `vmInstanceUuid` varchar(32) DEFAULT NULL,
    `status` varchar(32) NOT NULL,
    `state` varchar(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    INDEX `idxHygonCcpMdevVOhostUuid` (`hostUuid`),
    INDEX `idxHygonCcpMdevVOccpDeviceUuid` (`ccpDeviceUuid`),
    INDEX `idxHygonCcpMdevVOmdevUuid` (`mdevUuid`),
    INDEX `idxHygonCcpMdevVOvmInstanceUuid` (`vmInstanceUuid`),
    INDEX `idxHygonCcpMdevVOuseFlag` (`useFlag`),
    CONSTRAINT `fkHygonCcpMdevVOHostEO` FOREIGN KEY (`hostUuid`) REFERENCES `zstack`.`HostEO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkHygonCcpMdevVOHygonCCPDeviceVO` FOREIGN KEY (`ccpDeviceUuid`) REFERENCES `zstack`.`HygonCcpDeviceVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkHygonCcpMdevVOVmInstanceEO` FOREIGN KEY (`vmInstanceUuid`) REFERENCES `zstack`.`VmInstanceEO` (`uuid`) ON DELETE SET NULL
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;
-- ---------------------------------
--  END OF HYGON CCP DEVICE VIRTUALIZATION
-- ---------------------------------
