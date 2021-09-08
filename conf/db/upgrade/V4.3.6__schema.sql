ALTER TABLE `zstack`.`LicenseHistoryVO` ADD COLUMN `hash` char(32) DEFAULT 'unknown';
DROP INDEX idxLicenseHistoryVOUploadDate ON LicenseHistoryVO;
CREATE INDEX idxLicenseHistoryVOHash ON LicenseHistoryVO (hash);

DELETE FROM SystemTagVO WHERE tag LIKE 'bootOrder::%' AND resourceType = 'VmInstanceVO' AND uuid NOT IN (SELECT id FROM
  (SELECT min(uuid) AS id FROM SystemTagVO WHERE tag LIKE 'bootOrder::%' GROUP BY resourceUuid)
  AS table0);
DELETE FROM SystemTagVO WHERE tag LIKE 'bootOrderOnce::%' AND resourceType = 'VmInstanceVO' AND uuid NOT IN (SELECT id FROM
  (SELECT min(uuid) AS id FROM SystemTagVO WHERE tag LIKE 'bootOrderOnce::%' GROUP BY resourceUuid)
  AS table0);
UPDATE SystemTagVO SET inherent = 0 WHERE resourceType = 'VmInstanceVO' AND (tag LIKE 'bootOrder::%' OR tag LIKE 'bootOrderOnce::%');

DELIMITER $$
CREATE PROCEDURE setApplianceVmCpuModeToNone()
    BEGIN
        DECLARE vmUuid VARCHAR(32);
        DECLARE done INT DEFAULT FALSE;
        DECLARE foundInConfig INT DEFAULT 0;
        DECLARE resourceConfigUuid VARCHAR(32);
        DECLARE configDescription VARCHAR(1024);
        DECLARE vmCursor CURSOR FOR SELECT uuid from `zstack`.`VmInstanceVO` WHERE type = "ApplianceVm";
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

        SET configDescription = 'the cpu mode option, which could be used to enable nested virtualization, options are [none, host-model, host-passthrough, Haswell, Haswell-noTSX, Broadwell, Broadwell-noTSX, SandyBridge, IvyBridge, Conroe, Penryn, Nehalem, Westmere, Opteron_G1, Opteron_G2, Opteron_G3, Opteron_G4]. none: not use nested virtualization; host-model/host-passthrough will enable nested virtualization. When using host-passthrough, VM will see same CPU model in Host /proc/cpuinfo. When using host-model or host-passthrough, VM migration might be failed, due to mismatched CPU model. To use nested virtualization, user need to do some pre-configuration. Firstly, the /sys/module/kvm_intel/parameters/nested should be set as Y; Secondly, the /usr/libexec/qemu-kvm binary should support nested feature as well.';

        OPEN vmCursor;
        read_loop: LOOP
            FETCH vmCursor INTO vmUuid;
            IF done THEN
                LEAVE read_loop;
            END IF;

            SET resourceConfigUuid = (REPLACE(UUID(), '-', ''));
            SELECT COUNT(*) INTO foundInConfig FROM `zstack`.`ResourceConfigVO` WHERE resourceUuid = vmUuid AND name = "vm.cpuMode";

            IF (foundInConfig = 0) THEN
                INSERT `zstack`.`ResourceConfigVO`(uuid, name, description, category, value, resourceUuid, resourceType, createDate, lastOpDate)
                VALUES (resourceConfigUuid, "vm.cpuMode", configDescription, "kvm", "none", vmUuid ,"VmInstanceVO", NOW(), NOW());
            END IF;

        END LOOP;
        CLOSE vmCursor;
        SELECT CURTIME();
    END $$
DELIMITER ;

call setApplianceVmCpuModeToNone();
DROP PROCEDURE IF EXISTS setApplianceVmCpuModeToNone;

DELIMITER $$
CREATE PROCEDURE moveClusterCpuModeFromSystemTagToResourceConfig()
    BEGIN
        DECLARE done INT DEFAULT FALSE;
        DECLARE cpuModeTag VARCHAR(64);
        DECLARE clusterUuid VARCHAR(32);
        DECLARE foundInConfig INT DEFAULT 0;
        DECLARE resourceConfigUuid VARCHAR(32);
        DECLARE configDescription VARCHAR(1024);
        DECLARE tagCursor CURSOR FOR SELECT resourceUuid, tag from `zstack`.`SystemTagVO` WHERE tag LIKE '%clusterKVMCpuModel::%';
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

        SET configDescription = 'the cpu mode option, which could be used to enable nested virtualization, options are [none, host-model, host-passthrough, Haswell, Haswell-noTSX, Broadwell, Broadwell-noTSX, SandyBridge, IvyBridge, Conroe, Penryn, Nehalem, Westmere, Opteron_G1, Opteron_G2, Opteron_G3, Opteron_G4]. none: not use nested virtualization; host-model/host-passthrough will enable nested virtualization. When using host-passthrough, VM will see same CPU model in Host /proc/cpuinfo. When using host-model or host-passthrough, VM migration might be failed, due to mismatched CPU model. To use nested virtualization, user need to do some pre-configuration. Firstly, the /sys/module/kvm_intel/parameters/nested should be set as Y; Secondly, the /usr/libexec/qemu-kvm binary should support nested feature as well.';

        OPEN tagCursor;
        read_loop: LOOP
            FETCH tagCursor INTO clusterUuid, cpuModeTag;
            IF done THEN
                LEAVE read_loop;
            END IF;

            SET resourceConfigUuid = (REPLACE(UUID(), '-', ''));
            SELECT COUNT(*) INTO foundInConfig FROM `zstack`.`ResourceConfigVO` WHERE resourceUuid = clusterUuid AND name = "vm.cpuMode";

            IF (foundInConfig = 0) THEN
                DELETE FROM `zstack`.`SystemTagVO` WHERE tag LIKE '%clusterKVMCpuModel::%' AND resourceUuid = clusterUuid;
                INSERT `zstack`.`ResourceConfigVO`(uuid, name, description, category, value, resourceUuid, resourceType, createDate, lastOpDate)
                VALUES (resourceConfigUuid, "vm.cpuMode", configDescription, "kvm", substring(cpuModeTag, LENGTH('clusterKVMCpuModel::') + 1), clusterUuid ,"ClusterVO", NOW(), NOW());
            END IF;

        END LOOP;
        CLOSE tagCursor;
        SELECT CURTIME();
    END $$
DELIMITER ;

call moveClusterCpuModeFromSystemTagToResourceConfig();
DROP PROCEDURE IF EXISTS moveClusterCpuModeFromSystemTagToResourceConfig;

DELIMITER $$
CREATE PROCEDURE deleteVmLevelCpuModeIfAlreadySetClusterLevelCpuMode()
    BEGIN
        DECLARE vmUuid VARCHAR(32);
        DECLARE done INT DEFAULT FALSE;
        DECLARE vmClusterUuid VARCHAR(32);
        DECLARE foundVmCpuModeInConfig INT DEFAULT 0;
        DECLARE foundClusterCpuModeInConfig INT DEFAULT 0;
        DECLARE vmCursor CURSOR FOR SELECT uuid, clusterUuid from `zstack`.`VmInstanceVO` WHERE type = "UserVm";
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

        OPEN vmCursor;
        read_loop: LOOP
            FETCH vmCursor INTO vmUuid, vmClusterUuid;
            IF done THEN
                LEAVE read_loop;
            END IF;

            SELECT COUNT(*) INTO foundVmCpuModeInConfig FROM `zstack`.`ResourceConfigVO` WHERE resourceUuid = vmUuid AND name = "vm.cpuMode";
            SELECT COUNT(*) INTO foundClusterCpuModeInConfig FROM `zstack`.`ResourceConfigVO` WHERE resourceUuid = vmClusterUuid AND name = "vm.cpuMode";

            IF (foundVmCpuModeInConfig = 1 AND foundClusterCpuModeInConfig = 1) THEN
                DELETE FROM `zstack`.`ResourceConfigVO` WHERE resourceUuid = vmUuid AND name = "vm.cpuMode";
            END IF;

        END LOOP;
        CLOSE vmCursor;
        SELECT CURTIME();
    END $$
DELIMITER ;

call deleteVmLevelCpuModeIfAlreadySetClusterLevelCpuMode();
DROP PROCEDURE IF EXISTS deleteVmLevelCpuModeIfAlreadySetClusterLevelCpuMode;

CREATE TABLE IF NOT EXISTS `zstack`.`HostPhysicalMemoryVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `manufacturer` varchar(255) DEFAULT NULL,
    `size` varchar(32) DEFAULT NULL,
    `locator` varchar(255) DEFAULT NULL,
    `serialNumber` varchar(255) NOT NULL,
    `speed` varchar(32) DEFAULT NULL,
    `clockSpeed` varchar(32) DEFAULT NULL,
    `rank` varchar(32) DEFAULT NULL,
    `voltage` varchar(32) DEFAULT NULL,
    `hostUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkHostPhysicalMemoryVOHostVO` FOREIGN KEY (`hostUuid`) REFERENCES `zstack`.`HostEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;


DROP PROCEDURE IF EXISTS `Alter_Ceph_Table`;
DELIMITER $$
CREATE PROCEDURE Alter_Ceph_Table()
    BEGIN
        IF NOT EXISTS( SELECT NULL
                       FROM INFORMATION_SCHEMA.COLUMNS
                       WHERE table_name = 'CephPrimaryStoragePoolVO'
                             AND table_schema = 'zstack'
                             AND column_name = 'securityPolicy')  THEN

            ALTER TABLE `zstack`.`CephPrimaryStoragePoolVO`
                ADD COLUMN `securityPolicy` varchar(255) DEFAULT 'Copy',
                ADD COLUMN `diskUtilization` FLOAT;
            UPDATE `zstack`.`CephPrimaryStoragePoolVO` SET `diskUtilization` = (SELECT format(1 / `replicatedSize`, 3));

            ALTER TABLE `zstack`.`CephBackupStorageVO`
                ADD COLUMN `poolSecurityPolicy` varchar(255) DEFAULT 'Copy',
                ADD COLUMN `poolDiskUtilization` FLOAT;
            UPDATE `zstack`.`CephBackupStorageVO` SET `poolDiskUtilization` = (SELECT format(1 / `poolReplicatedSize`, 3));
        END IF;
    END $$
DELIMITER ;

CALL Alter_Ceph_Table();
DROP PROCEDURE Alter_Ceph_Table;

UPDATE `zstack`.`BareMetal2ChassisVO` SET status = "IPxeBootFailed" WHERE status = "iPxeBootFailed";
UPDATE `zstack`.`BareMetal2ChassisVO` SET status = "IPxeBooting" WHERE status = "iPxeBooting";

ALTER TABLE QuotaVO MODIFY COLUMN `value` bigint DEFAULT 0;
