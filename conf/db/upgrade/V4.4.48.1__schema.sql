CREATE TABLE IF NOT EXISTS `zstack`.`BlockPrimaryStorageVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `vendorName` varchar(256) NOt NULL,
    `metadata` text DEFAULT NULL,
    `encryptGatewayIp` varchar(64) DEFAULT NULL,
    `encryptGatewayPort` smallint unsigned DEFAULT 8443,
    `encryptGatewayUsername` varchar(256) DEFAULT NULL,
    `encryptGatewayPassword` varchar(256) DEFAULT NULL,
    PRIMARY KEY (`uuid`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`BlockScsiLunVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `target` varchar(256) DEFAULT NULL,
    `name` VARCHAR(256) DEFAULT NULL,
    `id` smallint unsigned NOT NULL,
    `wwn` VARCHAR(256) DEFAULT NULL,
    `type` VARCHAR(128) NOT NULL,
    `size` bigint unsigned NOT NULL,
    `lunMapId` smallint unsigned DEFAULT 0,
    `lunInitSnapshotID` bigint unsigned DEFAULT 0,
    `usedSize` bigint(20) unsigned DEFAULT 0,
    `encryptedId` smallint unsigned DEFAULT 0,
    `encryptedWwn` varchar(256) DEFAULT NULL,
    `lunType` varchar(256) NOT NULL,
    `volumeUuid` varchar(32) DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkScsiLunVOVolumeVO` FOREIGN KEY (`volumeUuid`) REFERENCES `zstack`.`VolumeEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`BlockScsiLunVO` MODIFY COLUMN id smallint unsigned DEFAULT 0;
ALTER TABLE `zstack`.`BlockScsiLunVO` MODIFY COLUMN type VARCHAR(128) DEFAULT NULL;
ALTER TABLE `zstack`.`BlockScsiLunVO` MODIFY COLUMN lunType varchar(256) DEFAULT NULL;

CREATE TABLE IF NOT EXISTS `zstack`.`HostInitiatorRefVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `hostUuid` varchar(32) NOT NULL UNIQUE,
    `initiatorName` varchar(256) NOT NULL,
    `metadata` text DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkHostInitiatorRefVOHostVo` FOREIGN KEY (`hostUuid`) REFERENCES `zstack`.`HostEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`BlockPrimaryStorageVO` DROP `encryptGatewayIp`;
ALTER TABLE `zstack`.`BlockPrimaryStorageVO` DROP `encryptGatewayPort`;
ALTER TABLE `zstack`.`BlockPrimaryStorageVO` DROP `encryptGatewayUsername`;
ALTER TABLE `zstack`.`BlockPrimaryStorageVO` DROP `encryptGatewayPassword`;
ALTER TABLE `zstack`.`BlockScsiLunVO` DROP `type`;
ALTER TABLE `zstack`.`BlockScsiLunVO` MODIFY COLUMN `id` int unsigned default 0;
ALTER TABLE `zstack`.`BlockScsiLunVO` MODIFY COLUMN `lunMapId` int unsigned default 0;

CREATE TABLE IF NOT EXISTS `zstack`.`BlockPrimaryStorageHostRefVO` (
    `id` BIGINT UNSIGNED NOT NULL UNIQUE AUTO_INCREMENT,
    `initiatorName` varchar(256) DEFAULT NULL,
    `metadata` text DEFAULT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fkBlockPrimaryStorageHostRefVOPrimaryStorageHostRefVO` FOREIGN KEY (`id`) REFERENCES `zstack`.`PrimaryStorageHostRefVO` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DELIMITER $$
CREATE PROCEDURE checkAllBlockHostInPrimaryHostRef()
    BEGIN
        DECLARE hostUuid VARCHAR(32);
        DECLARE primaryStorageUuid VARCHAR(32);
        DECLARE done INT DEFAULT FALSE;
        DECLARE cur CURSOR FOR SELECT hiref.hostUuid, hiref.primaryStorageUuid FROM HostInitiatorRefVO hiref;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

        OPEN cur;
        read_loop: LOOP
            FETCH cur INTO hostUuid, primaryStorageUuid;
            IF done THEN
                LEAVE read_loop;
            END IF;
            IF (select count(*) from PrimaryStorageHostRefVO pshref where pshref.hostUuid = hostUuid and pshref.primaryStorageUuid = primaryStorageUuid) = 0 THEN
                BEGIN
                    INSERT INTO zstack.PrimaryStorageHostRefVO (`primaryStorageUuid`, `hostUuid`, `status`, `lastOpDate`, `createDate`) values (primaryStorageUuid, hostUuid, 'Disconnected', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());
                END;
            END IF;
        END LOOP;
        CLOSE cur;
        SELECT CURTIME();
    END $$
DELIMITER ;

DELIMITER $$
CREATE PROCEDURE migrateBlockPrimaryHostRef()
    BEGIN
        DECLARE initiatorName VARCHAR(256);
        DECLARE psId BIGINT(20);
        DECLARE metadata text;
        DECLARE psUuid VARCHAR(32);
        DECLARE done INT DEFAULT FALSE;
        DECLARE cur CURSOR FOR SELECT  hostInitiatorRef.initiatorName, hostInitiatorRef.metadata, primaryStorageHostRef.id
             FROM zstack.HostInitiatorRefVO hostInitiatorRef, zstack.PrimaryStorageHostRefVO primaryStorageHostRef
             where hostInitiatorRef.hostUuid = primaryStorageHostRef.hostUuid and hostInitiatorRef.primaryStorageUuid = primaryStorageHostRef.primaryStorageUuid;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done =TRUE;
        OPEN cur;
        read_loop: LOOP
            FETCH cur INTO initiatorName, metadata, psId;
            IF done THEN
                LEAVE read_loop;
            END IF;
            IF (select count(*) from BlockPrimaryStorageHostRefVO bpshref where id = psId) = 0 THEN
                BEGIN
                    INSERT INTO zstack.BlockPrimaryStorageHostRefVO(id, initiatorName, metadata) values(psId, initiatorName, metadata);
                END;
            END IF;
        END LOOP;
        CLOSE cur;
        SELECT CURTIME();
    END $$
DELIMITER ;

DELIMITER $$
CREATE PROCEDURE checkHostInitiatorRefVO()
    BEGIN
        IF (SELECT count(*) FROM information_schema.columns WHERE table_name = 'HostInitiatorRefVO' AND column_name = 'primaryStorageUuid') != 0 THEN
            call checkAllBlockHostInPrimaryHostRef();
            call migrateBlockPrimaryHostRef();
        END IF;
    END $$
DELIMITER ;
call checkHostInitiatorRefVO();
DROP PROCEDURE IF EXISTS migrateBlockPrimaryHostRef;
DROP PROCEDURE IF EXISTS checkAllBlockHostInPrimaryHostRef;
DROP PROCEDURE IF EXISTS checkHostInitiatorRefVO;
DROP TABLE HostInitiatorRefVO;

CREATE TABLE IF NOT EXISTS `zstack`.`VxlanHostMappingVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `vxlanUuid` varchar(32) NOT NULL,
    `hostUuid` varchar(32) NOT NULL,
    `vlanId` int,
    `physicalInterface` varchar(32),
    `createDate`   timestamp    NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate`   timestamp    NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY  (`id`),
    CONSTRAINT `fkVxlanHostMappingVOVxlanNetworkVO` FOREIGN KEY (`vxlanUuid`) REFERENCES `VxlanNetworkVO` (`uuid`) ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT `fkVxlanHostMappingVOHostEO` FOREIGN KEY (`hostUuid`) REFERENCES `HostEO` (`uuid`) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`VxlanClusterMappingVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `vxlanUuid` varchar(32) NOT NULL,
    `clusterUuid` varchar(32) NOT NULL,
    `vlanId` int,
    `physicalInterface` varchar(32),
    `createDate`   timestamp    NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate`   timestamp    NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY  (`id`),
    CONSTRAINT `fkVxlanClusterMappingVOVxlanNetworkVO` FOREIGN KEY (`vxlanUuid`) REFERENCES `VxlanNetworkVO` (`uuid`) ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT `fkVxlanClusterMappingVOClusterEO` FOREIGN KEY (`clusterUuid`) REFERENCES `ClusterEO` (`uuid`) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

UPDATE `zstack`.`GlobalConfigVO` SET value="enable", defaultValue="enable" WHERE category="storageDevice" AND name="enable.multipath" AND value="true";
UPDATE `zstack`.`GlobalConfigVO` SET value="ignore", defaultValue="enable" WHERE category="storageDevice" AND name="enable.multipath" AND value="false";

DELIMITER $$
CREATE PROCEDURE UpdateHygonClusterVmCpuModeConfig()
    BEGIN
        DECLARE done INT DEFAULT FALSE;
        DECLARE tag_uuid VARCHAR(32);
        DECLARE resource_uuid VARCHAR(32);
        DECLARE resource_type varchar(64);
        DECLARE config_name varchar(255);
        DECLARE config_description varchar(255);
        DECLARE config_category varchar(64);
        DECLARE config_value varchar(64);
        DECLARE cur CURSOR FOR SELECT uuid FROM zstack.ClusterVO;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
        SET config_name = 'vm.cpuMode';
        SET config_description = 'the default configuration after the management node is upgraded (only for hygon cluster)';
        SET config_category = 'kvm';
        SET resource_type = 'ClusterVO';
        SET config_value = 'Hygon_Customized';

        OPEN cur;

        read_loop: LOOP
            FETCH cur INTO resource_uuid;

            IF done THEN
                LEAVE read_loop;
            END IF;

            SELECT COUNT(DISTINCT tag) INTO @hygon_tag_count FROM SystemTagVO WHERE tag LIKE 'hostCpuModelName::Hygon%' AND resourceUuid IN (SELECT uuid FROM HostVO WHERE clusterUuid = resource_uuid);

            SELECT COUNT(*) INTO @config_exist FROM ResourceConfigVO WHERE name = config_name AND category = config_category AND resourceUuid = resource_uuid AND resourceType = resource_type;

            IF @hygon_tag_count = 1 THEN
                IF @config_exist = 1 THEN
                    UPDATE ResourceConfigVO SET value = config_value WHERE name = config_name AND category = config_category AND resourceUuid = resource_uuid AND resourceType = resource_type;
                ELSE
                    SET tag_uuid = REPLACE(UUID(), '-', '');
                    INSERT INTO ResourceConfigVO (uuid, name, description, category, value, resourceUuid, resourceType, lastOpDate, createDate)
                            VALUES (tag_uuid, config_name, config_description, config_category, config_value, resource_uuid, resource_type, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());
                END IF;
            END IF;
        END LOOP;

        CLOSE cur;
    END $$
DELIMITER ;
CALL UpdateHygonClusterVmCpuModeConfig();
DROP PROCEDURE IF EXISTS UpdateHygonClusterVmCpuModeConfig;
