
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

CREATE TABLE IF NOT EXISTS `zstack`.`IAM2ProjectResourceRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `resourceUuid` varchar(32) NOT NULL,
    `resourceType` varchar(128) NOT NULL,
    `projectUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`id`),
    CONSTRAINT `fkIAM2ProjectResourceRefVOResourceVO` FOREIGN KEY (`resourceUuid`) REFERENCES `ResourceVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkIAM2ProjectResourceRefVOIAM2ProjectVO` FOREIGN KEY (`projectUuid`) REFERENCES `IAM2ProjectVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

