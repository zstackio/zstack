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

CREATE TABLE IF NOT EXISTS `zstack`.`NfvInstOfferingVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `managementNetworkUuid` varchar(32) NOT NULL,
    `imageUuid` varchar(32) NOT NULL,
    `zoneUuid` varchar(32) NOT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT fkNfvInstOfferingVOL3NetworkEO FOREIGN KEY (managementNetworkUuid) REFERENCES `zstack`.`L3NetworkEO` (uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`NfvInstGroupVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `name` VARCHAR(255) NOT NULL,
    `description` VARCHAR(2048) DEFAULT NULL,
    `nfvInstOfferingUuid` VARCHAR(32) DEFAULT NULL,
    `instType` VARCHAR(64) NOT NULL,
    `funcType` VARCHAR(64) NOT NULL,
    `configVersion` int unsigned DEFAULT 0,
    `netOsDistro` VARCHAR(128) DEFAULT NULL,
    `baseOsDistro` VARCHAR(128) DEFAULT NULL,
    `status` VARCHAR(32) DEFAULT 'Initializing',
    `statusDetail` VARCHAR(255) DEFAULT NULL,
    `operationMode` VARCHAR(32) DEFAULT 'Normal',
    `vipUuid` VARCHAR(32) DEFAULT NULL,
    `ipv6VipUuid` VARCHAR(32) DEFAULT NULL,
    `primaryStorageUuid` VARCHAR(32) DEFAULT NULL,
    `primaryStoragePoolUuid` VARCHAR(32) DEFAULT NULL,
    `clusterUuid` VARCHAR(32) DEFAULT NULL,
    `zoneUuid` VARCHAR(32) DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkNfvInstGroupVONfvInstOfferingVO` FOREIGN KEY (`nfvInstOfferingUuid`) REFERENCES `zstack`.`NfvInstOfferingVO` (uuid) ON DELETE SET NULL,
    CONSTRAINT `fkNfvInstGroupVOPrimaryStorageEO` FOREIGN KEY (`primaryStorageUuid`) REFERENCES `zstack`.`PrimaryStorageEO` (uuid) ON DELETE SET NULL,
    CONSTRAINT `fkNfvInstGroupVOClusterEO` FOREIGN KEY (`clusterUuid`) REFERENCES `zstack`.`ClusterEO` (uuid) ON DELETE SET NULL,
    CONSTRAINT `fkNfvInstGroupVOZoneEO` FOREIGN KEY (`zoneUuid`) REFERENCES `zstack`.`ZoneEO` (uuid) ON DELETE SET NULL,
    KEY `idx_nfv_inst_group_status_mode` (`status`, `operationMode`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`NfvInstGroupMonitorIpVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `nfvInstGroupUuid` varchar(32) NOT NULL,
    `monitorIp` varchar(255) NOT NULL,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
     PRIMARY KEY (`id`),
     CONSTRAINT fkNfvInstGroupMonitorIpVONfvInstGroupVO FOREIGN KEY (nfvInstGroupUuid) REFERENCES NfvInstGroupVO (uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`NfvInstGroupNetworkServiceRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `nfvInstGroupUuid` varchar(32) NOT NULL,
    `networkServiceName` VARCHAR(255) NOT NULL,
    `networkServiceUuid` varchar(32) NOT NULL,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT fkNfvInstGroupNetworkServiceRefVONfvInstGroupVO FOREIGN KEY (nfvInstGroupUuid) REFERENCES NfvInstGroupVO (uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`NfvInstGroupL3NetworkRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `nfvInstGroupUuid` varchar(32) NOT NULL,
    `networkServiceUuid` VARCHAR(32) NOT NULL,
    `l3NetworkUuid` varchar(32) NOT NULL,
    `type` varchar(255) NOT NULL,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    CONSTRAINT fkNfvInstGroupL3NetworkRefVONfvInstGroupVO FOREIGN KEY (nfvInstGroupUuid) REFERENCES NfvInstGroupVO (uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`NfvInstVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `nfvInstGroupUuid` varchar(32) NOT NULL,
    `configVersion` int unsigned NOT NULL DEFAULT 0,
    `netOsDistro` VARCHAR(128) NOT NULL,
    `baseOsDistro` VARCHAR(128) NOT NULL,
    `clusterStatus` VARCHAR(32) DEFAULT 'Unknown',
    `statusDetail` VARCHAR(255) DEFAULT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT fkNfvInstVONfvInstGroupVO FOREIGN KEY (nfvInstGroupUuid) REFERENCES NfvInstGroupVO (uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`NfvInstMetaDataVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `agentVersion` varchar(32) DEFAULT NULL,
    `netOsVersion` varchar(32) DEFAULT NULL,
    `baseOsVersion` varchar(32) DEFAULT NULL,
    `kernelVersion` varchar(256) DEFAULT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkNfvInstMetadataVONfvInstVO` FOREIGN KEY (`uuid`) REFERENCES `NfvInstVO` (`uuid`) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`NfvInstGroupConfigTaskVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `nfvInstGroupUuid` varchar(32) NOT NULL,
    `configVersion` int unsigned DEFAULT 0,
    `serviceUuid` varchar(32) NOT NULL,
    `taskName` VARCHAR(255) NOT NULL,
    `path` VARCHAR(255) NOT NULL,
    `taskData` text DEFAULT NULL,
    `checkStatus` BOOLEAN DEFAULT FALSE,
    PRIMARY KEY (`id`),
    CONSTRAINT fkNfvInstGroupConfigTaskVONfvInstGroupVO FOREIGN KEY (nfvInstGroupUuid) REFERENCES NfvInstGroupVO (uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CALL ADD_COLUMN('OvnControllerVmInstanceVO', 'nbClusterStatus', 'VARCHAR(32)', 1, 'Unknown');
CALL ADD_COLUMN('OvnControllerVmInstanceVO', 'sbClusterStatus', 'VARCHAR(32)', 1, 'Unknown');

-- ========================================
-- Upgrade OvnControllerVmOfferingVO and OvnControllerVmInstanceVO hierarchy
-- Only execute if NOT already upgraded
-- ========================================

DELIMITER $$

DROP PROCEDURE IF EXISTS upgrade_ovn_controller_to_nfv_inst$$

CREATE PROCEDURE upgrade_ovn_controller_to_nfv_inst()
BEGIN
    DECLARE already_upgraded INT DEFAULT 0;
    DECLARE has_management_network_column INT DEFAULT 0;
    DECLARE offering_count INT DEFAULT 0;
    DECLARE offering_migrated_count INT DEFAULT 0;
    DECLARE instance_in_nfv_inst_count INT DEFAULT 0;
    DECLARE instance_in_nfv_group_count INT DEFAULT 0;
    
    -- Check if upgrade has already been done by checking any of these conditions:
    -- a. OvnControllerVmOfferingVO no longer has managementNetworkUuid column
    SELECT COUNT(*) INTO has_management_network_column
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'zstack'
      AND TABLE_NAME = 'OvnControllerVmOfferingVO'
      AND COLUMN_NAME = 'managementNetworkUuid';
    
    IF has_management_network_column = 0 THEN
        SET already_upgraded = 1;
    END IF;
    
    -- b. Check if OvnControllerVmOfferingVO records exist in NfvInstOfferingVO
    IF already_upgraded = 0 THEN
        SELECT COUNT(*) INTO offering_count FROM OvnControllerVmOfferingVO;
        
        IF offering_count > 0 THEN
            SELECT COUNT(*) INTO offering_migrated_count
            FROM OvnControllerVmOfferingVO ovo
            INNER JOIN NfvInstOfferingVO nivo ON ovo.uuid = nivo.uuid;
            
            IF offering_migrated_count = offering_count THEN
                SET already_upgraded = 1;
            END IF;
        END IF;
    END IF;
    
    -- c. Check if OvnControllerVmInstanceVO records exist in NfvInstVO
    IF already_upgraded = 0 THEN
        SELECT COUNT(*) INTO instance_in_nfv_inst_count
        FROM OvnControllerVmInstanceVO ovi
        INNER JOIN NfvInstVO niv ON ovi.uuid = niv.uuid;
        
        IF instance_in_nfv_inst_count > 0 THEN
            SET already_upgraded = 1;
        END IF;
    END IF;
    
    -- d. Check if NfvInstGroupVO created for OvnControllerVmInstanceVO
    IF already_upgraded = 0 THEN
        SELECT COUNT(*) INTO instance_in_nfv_group_count
        FROM OvnControllerVmInstanceVO ovi
        INNER JOIN NfvInstVO niv ON ovi.uuid = niv.uuid
        INNER JOIN NfvInstGroupVO nig ON niv.nfvInstGroupUuid = nig.uuid
        WHERE nig.funcType = 'OVN_SDN_CONTROLLER';
        
        IF instance_in_nfv_group_count > 0 THEN
            SET already_upgraded = 1;
        END IF;
    END IF;
    
    -- If any condition is met, skip the upgrade
    IF already_upgraded = 1 THEN
        SELECT 'Upgrade already completed, skipping OVN Controller to NFV Instance migration' AS message;
    ELSE
        -- ========================================
        -- Step 1: Create NfvInstOfferingVO records for existing OvnControllerVmOfferingVO
        -- This must be done BEFORE dropping columns to preserve data
        -- ========================================
        INSERT INTO NfvInstOfferingVO (uuid, managementNetworkUuid, imageUuid, zoneUuid)
        SELECT 
            ovo.uuid,
            ovo.managementNetworkUuid,
            ovo.imageUuid,
            ovo.zoneUuid
        FROM OvnControllerVmOfferingVO ovo
        WHERE NOT EXISTS (
            SELECT 1 FROM NfvInstOfferingVO nivo WHERE nivo.uuid = ovo.uuid
        );
        
        -- ========================================
        -- Step 2: Now safe to drop foreign key from OvnControllerVmOfferingVO
        -- (Columns will be dropped after procedure execution)
        -- ========================================
        IF has_management_network_column > 0 THEN
            -- Check if foreign key exists before dropping
            IF EXISTS (
                SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
                WHERE TABLE_SCHEMA = 'zstack'
                  AND TABLE_NAME = 'OvnControllerVmOfferingVO'
                  AND CONSTRAINT_NAME = 'fkOvnControllerVmOfferingVOL3NetworkEO'
            ) THEN
                ALTER TABLE OvnControllerVmOfferingVO DROP FOREIGN KEY fkOvnControllerVmOfferingVOL3NetworkEO;
            END IF;
        END IF;
        
        -- ========================================
        -- Step 3: Create NfvInstGroupVO for each OvnControllerVmInstanceVO
        -- ========================================
        
        -- First, create a temporary table to store the mapping between instance UUID and generated group UUID
        CREATE TEMPORARY TABLE IF NOT EXISTS temp_ovn_inst_group_mapping (
            inst_uuid VARCHAR(32) NOT NULL PRIMARY KEY,
            group_uuid VARCHAR(32) NOT NULL,
            group_name VARCHAR(255) NOT NULL
        );
        
        -- Generate random UUIDs for each OVN instance and store the mapping
        INSERT INTO temp_ovn_inst_group_mapping (inst_uuid, group_uuid, group_name)
        SELECT 
            ovn.uuid AS inst_uuid,
            REPLACE(UUID(), '-', '') AS group_uuid,
            LEFT(CONCAT('OVN-Controller-Group-', vm.name), 255) AS group_name
        FROM OvnControllerVmInstanceVO ovn
        INNER JOIN VmInstanceVO vm ON ovn.uuid = vm.uuid
        WHERE NOT EXISTS (
            SELECT 1 FROM NfvInstGroupVO nfv_grp WHERE nfv_grp.uuid = ovn.uuid
        );
        
        -- Insert ResourceVO records for each NfvInstGroup
        INSERT INTO ResourceVO (uuid, resourceName, resourceType, concreteResourceType)
        SELECT 
            mapping.group_uuid AS uuid,
            mapping.group_name AS resourceName,
            'NfvInstGroupVO' AS resourceType,
            'org.zstack.network.service.nfvinstgroup.NfvInstGroupVO' AS concreteResourceType
        FROM temp_ovn_inst_group_mapping mapping
        WHERE NOT EXISTS (
            SELECT 1 FROM ResourceVO res WHERE res.uuid = mapping.group_uuid
        );
        
        -- Insert NfvInstGroupVO records
        INSERT INTO NfvInstGroupVO (
            uuid,
            name,
            description,
            nfvInstOfferingUuid,
            instType,
            funcType,
            configVersion,
            netOsDistro,
            baseOsDistro,
            status,
            operationMode,
            vipUuid,
            primaryStorageUuid,
            clusterUuid,
            zoneUuid,
            createDate,
            lastOpDate
        )
        SELECT 
            mapping.group_uuid AS group_uuid,                  -- group uuid: randomly generated
            mapping.group_name AS name,                        -- group name (limit to 255 chars)
            CONCAT('Auto-created group for OVN controller ', vm.name) AS description,
            ovn_off.uuid AS offering_uuid,                     -- nfvInstOfferingUuid
            'KVM' AS inst_type,                                -- instType
            'OVN_SDN_CONTROLLER' AS func_type,                 -- funcType
            0 AS config_version,                               -- configVersion
            'euler' AS net_os_distro,                          -- netOsDistro
            'euler' AS base_os_distro,                         -- baseOsDistro
            'Initializing' AS status,                          -- status
            'Normal' AS operation_mode,                        -- operationMode
            NULL AS vip_uuid,                                  -- vipUuid (will be set later if needed)
            vol.primaryStorageUuid AS ps_uuid,                 -- primaryStorageUuid (from VolumeVO via rootVolumeUuid)
            vm.clusterUuid AS cluster_uuid,                    -- clusterUuid
            vm.zoneUuid AS zone_uuid,                          -- zoneUuid
            vm.createDate AS create_date,                      -- createDate
            vm.lastOpDate AS last_op_date                      -- lastOpDate
        FROM temp_ovn_inst_group_mapping mapping
        INNER JOIN OvnControllerVmInstanceVO ovn ON mapping.inst_uuid = ovn.uuid
        INNER JOIN VmInstanceVO vm ON ovn.uuid = vm.uuid
        LEFT JOIN VolumeVO vol ON vm.rootVolumeUuid = vol.uuid
        LEFT JOIN OvnControllerVmOfferingVO ovn_off ON vm.instanceOfferingUuid = ovn_off.uuid
        WHERE NOT EXISTS (
            SELECT 1 FROM NfvInstGroupVO nfv_grp WHERE nfv_grp.uuid = mapping.group_uuid
        );
        
        -- ========================================
        -- Step 4: Create NfvInstVO records for each OvnControllerVmInstanceVO
        -- ========================================
        INSERT INTO NfvInstVO (
            uuid,
            nfvInstGroupUuid,
            configVersion,
            netOsDistro,
            baseOsDistro,
            clusterStatus,
            statusDetail
        )
        SELECT 
            ovn.uuid AS inst_uuid,
            mapping.group_uuid AS group_uuid,                  -- link to the group we created (using mapping)
            0 AS config_version,                               -- configVersion
            'euler' AS net_os_distro,                          -- netOsDistro (required field)
            'euler' AS base_os_distro,                         -- baseOsDistro (required field)
            'Unknown' AS cluster_status,                       -- clusterStatus
            NULL AS status_detail                              -- statusDetail
        FROM OvnControllerVmInstanceVO ovn
        INNER JOIN temp_ovn_inst_group_mapping mapping ON ovn.uuid = mapping.inst_uuid
        WHERE NOT EXISTS (
            SELECT 1 FROM NfvInstVO nfv WHERE nfv.uuid = ovn.uuid
        );
        
        -- Clean up temporary table
        DROP TEMPORARY TABLE IF EXISTS temp_ovn_inst_group_mapping;
        
        SELECT 'OVN Controller to NFV Instance migration completed successfully' AS message;

        -- Drop columns after data migration (if not already dropped)
        CALL DROP_COLUMN('OvnControllerVmOfferingVO', 'managementNetworkUuid');
        CALL DROP_COLUMN('OvnControllerVmOfferingVO', 'imageUuid');
        CALL DROP_COLUMN('OvnControllerVmOfferingVO', 'zoneUuid');
    END IF;
END$$

DELIMITER ;

-- Execute the upgrade procedure
CALL upgrade_ovn_controller_to_nfv_inst();

-- Drop the procedure after execution
DROP PROCEDURE IF EXISTS upgrade_ovn_controller_to_nfv_inst;

CREATE TABLE IF NOT EXISTS `OvnControllerVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `remoteOvn` tinyint(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT IGNORE INTO OvnControllerVO (uuid, remoteOvn)
SELECT uuid, 0 FROM SdnControllerVO where vendorType = 'Ovn';
