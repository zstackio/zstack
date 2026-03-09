ALTER TABLE AutoScalingRuleSchedulerJobTriggerVO DROP FOREIGN KEY fkAutoScalingRuleSchedulerJobTriggerVO;
CALL ADD_CONSTRAINT('AutoScalingRuleSchedulerJobTriggerVO', 'fkAutoScalingRuleSchedulerJobTriggerVO', 'schedulerJobUuid', 'SchedulerJobVO', 'uuid', 'CASCADE');

ALTER TABLE `zstack`.`ExternalPrimaryStorageVO` MODIFY COLUMN `config` TEXT DEFAULT NULL;
ALTER TABLE `zstack`.`HostNetworkInterfaceLldpRefVO` MODIFY COLUMN `systemName` VARCHAR(255) NOT NULL;

CREATE TABLE IF NOT EXISTS `zstack`.`ExternalPrimaryStorageHostRefVO` (
    `id`       BIGINT UNSIGNED UNIQUE,
    `hostId`   INT          DEFAULT NULL,
    `protocol` varchar(128) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;

SET @row_number = 0;
INSERT INTO ExternalPrimaryStorageHostRefVO (id, hostId, protocol)
SELECT
    p.id,
    (@row_number := @row_number + 1) as hostId,
    e.defaultProtocol as protocol
FROM PrimaryStorageHostRefVO p LEFT JOIN ExternalPrimaryStorageVO e ON p.primaryStorageUuid = e.uuid
ORDER BY p.id;

-- Delete old UserTagVO of AI::Image-Generation
DELETE FROM UserTagVO WHERE uuid = 'a7ec68923efe447d9119ba7b6df2b54c';

DELETE ref FROM `zstack`.`VolumeSnapshotReferenceVO` ref
                    INNER JOIN `zstack`.`VolumeEO` vol ON vol.uuid = ref.referenceVolumeUuid
WHERE ref.referenceType = 'VolumeVO'
  AND ref.referenceVolumeUuid = ref.referenceUuid
  AND ref.referenceInstallUrl NOT LIKE CONCAT('%', SUBSTRING_INDEX(vol.installPath, '/', -1), '%');

DROP PROCEDURE IF EXISTS ModifyApplicationDevelopmentServiceVO;
DELIMITER $$

CREATE PROCEDURE ModifyApplicationDevelopmentServiceVO()
BEGIN
    START TRANSACTION;

    CREATE TABLE IF NOT EXISTS `zstack`.`ApplicationDevelopmentServiceVO_temp` (
        `uuid` varchar(32) NOT NULL UNIQUE,
        `deploymentStatus` varchar(255) NOT NULL,
        PRIMARY KEY (`uuid`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

    INSERT INTO `zstack`.`ApplicationDevelopmentServiceVO_temp` (uuid, deploymentStatus)
    SELECT modelServiceGroupUuid, deploymentStatus
    FROM `zstack`.`ApplicationDevelopmentServiceVO`
    WHERE modelServiceGroupUuid IS NOT NULL;

    DROP TABLE `zstack`.`ApplicationDevelopmentServiceVO`;

    RENAME TABLE `zstack`.`ApplicationDevelopmentServiceVO_temp` TO `zstack`.`ApplicationDevelopmentServiceVO`;

    COMMIT;
    SELECT CURTIME();
END $$

DELIMITER ;

CALL ModifyApplicationDevelopmentServiceVO();

CALL ADD_COLUMN('ModelVO', 'modelId', 'VARCHAR(255)', 1, NULL);
CALL ADD_COLUMN('ModelServiceInstanceGroupVO', 'description', 'VARCHAR(2048)', 1, NULL);
CALL ADD_COLUMN('ModelServiceVO', 'source', 'VARCHAR(32)', 1, NULL);
CALL ADD_COLUMN('ModelServiceVO', 'readme', 'TEXT', 1, NULL);

-- Delete ZStack-default-inference-template
DELETE FROM `zstack`.`ModelServiceVO` WHERE `uuid` = '97e66447fa4246649dcc41b72b412407';
-- Delete qwen chat
DELETE FROM `zstack`.`ModelServiceVO` WHERE `uuid` = '0446d8fd9487403cc12e7645f5r68d04';
-- Delete xtts
DELETE FROM `zstack`.`ModelServiceVO` WHERE `uuid` = 'e944c98c4a154f53a86f34eb0fcd093c';
-- Delete sdxl
DELETE FROM `zstack`.`ModelServiceVO` WHERE `uuid` = '80fab6f2f3d444e1a0b39702dcc62bac';
-- Delete blip image
DELETE FROM `zstack`.`ModelServiceVO` WHERE `uuid` = '2ad69dc6cebf405f9e0d750bb50e120c';
-- Delete stable video
DELETE FROM `zstack`.`ModelServiceVO` WHERE `uuid` = 'c65d3019cb3f400f80e5e2a10dcaf861';
-- Delete yolo
DELETE FROM `zstack`.`ModelServiceVO` WHERE `uuid` = '0b714f4d8c5c43ca86c3a6caa58358a7';

ALTER TABLE `zstack`.`BaremetalNicVO` modify column mac varchar(255) DEFAULT NULL;

-- framework field changed to LLM frameworks not service sources
-- 1. Change the origin framework value to source field
-- 2. If source is Bentoml change framework to BentoML
-- 3. Else change framework to Other

UPDATE `zstack`.`ModelServiceVO` SET source = framework WHERE source is NULL
    AND framework in ('HuggingFace', 'Bentoml', 'Other');

UPDATE `zstack`.`ModelServiceVO` SET source = 'Other' WHERE source is NULL
    AND framework not in ('HuggingFace', 'Bentoml', 'Other');

UPDATE `zstack`.`ModelServiceVO` SET source = 'Bentoml' WHERE source = 'BentoML';
UPDATE `zstack`.`ModelServiceVO` SET framework = 'BentoML' WHERE source = 'Bentoml'
    AND framework not in ('vLLM', 'Diffusers', 'Transformers', 'sentence_transformers', 'llama.cpp', 'BentoML', 'Other', 'Ollama');

Update ModelServiceVO set framework = 'Other' where framework not in
    ('vLLM', 'Diffusers', 'Transformers', 'sentence_transformers', 'llama.cpp', 'BentoML', 'Other', 'Ollama')
    AND source != 'Bentoml';
Update ModelServiceVO set framework = 'Other' where framework is NULL;

DROP PROCEDURE IF EXISTS CreateResourceConfigForBindingVms;
DELIMITER $$
CREATE PROCEDURE CreateResourceConfigForBindingVms()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE vmUuid VARCHAR(128);

    DECLARE vmCursor CURSOR FOR
        SELECT resourceUuid
        FROM SystemTagVO
        WHERE resourceType = 'VmInstanceVO'
          AND tag LIKE 'resourceBindings::Cluster:%';

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN vmCursor;

    read_loop: LOOP
        FETCH vmCursor INTO vmUuid;

        IF done THEN
            LEAVE read_loop;
        END IF;

        IF NOT EXISTS (
            SELECT 1
            FROM ResourceConfigVO
            WHERE resourceType = 'VmInstanceVO'
              AND resourceUuid = vmUuid
              AND category = 'vm'
              AND name = 'vm.ha.across.clusters'
        ) THEN
            INSERT INTO ResourceConfigVO (uuid, name, category, value, resourceUuid, resourceType, lastOpDate, createDate)
            VALUES (REPLACE(UUID(),'-',''), 'vm.ha.across.clusters', 'vm', 'false', vmUuid, 'VmInstanceVO', NOW(), NOW());
        END IF;
    END LOOP;

    CLOSE vmCursor;
END $$
DELIMITER ;
call CreateResourceConfigForBindingVms();
DROP PROCEDURE IF EXISTS CreateResourceConfigForBindingVms;

DELETE FROM `SSOServerTokenVO`;
ALTER TABLE `zstack`.`SSOServerTokenVO` ADD sessionUuid VARCHAR(32) DEFAULT NULL;
ALTER TABLE `zstack`.`SSOServerTokenVO` ADD CONSTRAINT `fkSSOServerTokenVOSessionVO` FOREIGN KEY (`sessionUuid`) REFERENCES `SessionVO` (`uuid`) ON DELETE CASCADE;

CALL ADD_COLUMN('SdnControllerVO', 'status', 'VARCHAR(32)', 0, 'Connected');
CALL ADD_COLUMN('HostNetworkInterfaceVO', 'driverType', 'VARCHAR(32)', 1, NULL);

CREATE TABLE IF NOT EXISTS `zstack`.`SdnControllerHostRefVO` (
    `id` BIGINT UNSIGNED NOT NULL UNIQUE AUTO_INCREMENT,
    `sdnControllerUuid` varchar(32) NOT NULL,
    `hostUuid`  varchar(32) NOT NULL,
    `vSwitchType`  varchar(255) NOT NULL,
    `vtepIp`  varchar(128) DEFAULT NULL,
    `netmask`  varchar(128) DEFAULT NULL,
    `nicPciAddresses`  varchar(1024) DEFAULT NULL,
    `nicDrivers`  varchar(1024) DEFAULT NULL,
    `bondMode`  varchar(64) DEFAULT NULL,
    `lacpMode`  varchar(64) DEFAULT NULL,
    CONSTRAINT fkSdnControllerHostRefVOSdnControllerVO FOREIGN KEY (sdnControllerUuid) REFERENCES SdnControllerVO (uuid) ON DELETE CASCADE,
    CONSTRAINT fkSdnControllerHostRefVOHostEO FOREIGN KEY (hostUuid) REFERENCES HostEO (uuid) ON DELETE CASCADE,
    CONSTRAINT ukSdnControllerHostRefVO UNIQUE (`sdnControllerUuid`,`hostUuid`, `vSwitchType`),
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS  `zstack`.`OvnControllerVmOfferingVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `managementNetworkUuid` varchar(32) NOT NULL,
    `imageUuid` varchar(32) NOT NULL,
    `zoneUuid` varchar(32) NOT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT fkOvnControllerVmOfferingVOL3NetworkEO FOREIGN KEY (managementNetworkUuid) REFERENCES `zstack`.`L3NetworkEO` (uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS  `zstack`.`OvnControllerVmInstanceVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

UPDATE `zstack`.`L2NetworkVO` set vSwitchType='TfL2Network' where type='TfL2Network';
