-- ZSTAC-75429: scope AI ModelCenter-derived resources by zone.
CALL ADD_COLUMN('ModelVO', 'zoneUuid', 'VARCHAR(32)', 1, NULL);
CALL ADD_COLUMN('ModelServiceVO', 'zoneUuid', 'VARCHAR(32)', 1, NULL);
CALL ADD_COLUMN('DatasetVO', 'zoneUuid', 'VARCHAR(32)', 1, NULL);
CALL ADD_COLUMN('ModelServiceInstanceGroupVO', 'zoneUuid', 'VARCHAR(32)', 1, NULL);

UPDATE `zstack`.`ModelCenterVO` mc
INNER JOIN (
    SELECT ms.`modelCenterUuid`, MIN(vm.`zoneUuid`) AS `zoneUuid`, COUNT(DISTINCT vm.`zoneUuid`) AS `zoneCount`
    FROM `zstack`.`ModelServiceVO` ms
    INNER JOIN `zstack`.`ModelServiceInstanceGroupVO` g ON g.`modelServiceUuid` = ms.`uuid`
    INNER JOIN `zstack`.`ModelServiceInstanceVO` i ON i.`modelServiceGroupUuid` = g.`uuid`
    INNER JOIN `zstack`.`VmInstanceVO` vm ON vm.`uuid` = i.`vmInstanceUuid`
    WHERE vm.`zoneUuid` IS NOT NULL
      AND ms.`modelCenterUuid` IS NOT NULL
    GROUP BY ms.`modelCenterUuid`
) inferred ON inferred.`modelCenterUuid` = mc.`uuid`
SET mc.`zoneUuid` = inferred.`zoneUuid`
WHERE mc.`zoneUuid` IS NULL
  AND inferred.`zoneCount` = 1;

UPDATE `zstack`.`ModelVO` m
INNER JOIN `zstack`.`ModelCenterVO` mc ON m.`modelCenterUuid` = mc.`uuid`
SET m.`zoneUuid` = mc.`zoneUuid`
WHERE m.`zoneUuid` IS NULL;

UPDATE `zstack`.`ModelServiceVO` ms
INNER JOIN `zstack`.`ModelCenterVO` mc ON ms.`modelCenterUuid` = mc.`uuid`
SET ms.`zoneUuid` = mc.`zoneUuid`
WHERE ms.`zoneUuid` IS NULL;

UPDATE `zstack`.`DatasetVO` d
INNER JOIN `zstack`.`ModelCenterVO` mc ON d.`modelCenterUuid` = mc.`uuid`
SET d.`zoneUuid` = mc.`zoneUuid`
WHERE d.`zoneUuid` IS NULL;

UPDATE `zstack`.`ModelServiceVO` ms
INNER JOIN (
    SELECT g.`modelServiceUuid`, MIN(vm.`zoneUuid`) AS `zoneUuid`, COUNT(DISTINCT vm.`zoneUuid`) AS `zoneCount`
    FROM `zstack`.`ModelServiceInstanceGroupVO` g
    INNER JOIN `zstack`.`ModelServiceInstanceVO` i ON i.`modelServiceGroupUuid` = g.`uuid`
    INNER JOIN `zstack`.`VmInstanceVO` vm ON vm.`uuid` = i.`vmInstanceUuid`
    WHERE vm.`zoneUuid` IS NOT NULL
      AND g.`modelServiceUuid` IS NOT NULL
    GROUP BY g.`modelServiceUuid`
) inferred ON inferred.`modelServiceUuid` = ms.`uuid`
SET ms.`zoneUuid` = inferred.`zoneUuid`
WHERE ms.`zoneUuid` IS NULL
  AND inferred.`zoneCount` = 1;

UPDATE `zstack`.`ModelVO` m
INNER JOIN (
    SELECT g.`modelUuid`, MIN(vm.`zoneUuid`) AS `zoneUuid`, COUNT(DISTINCT vm.`zoneUuid`) AS `zoneCount`
    FROM `zstack`.`ModelServiceInstanceGroupVO` g
    INNER JOIN `zstack`.`ModelServiceInstanceVO` i ON i.`modelServiceGroupUuid` = g.`uuid`
    INNER JOIN `zstack`.`VmInstanceVO` vm ON vm.`uuid` = i.`vmInstanceUuid`
    WHERE vm.`zoneUuid` IS NOT NULL
      AND g.`modelUuid` IS NOT NULL
    GROUP BY g.`modelUuid`
) inferred ON inferred.`modelUuid` = m.`uuid`
SET m.`zoneUuid` = inferred.`zoneUuid`
WHERE m.`zoneUuid` IS NULL
  AND inferred.`zoneCount` = 1;

UPDATE `zstack`.`ModelServiceInstanceGroupVO` g
LEFT JOIN `zstack`.`ModelServiceVO` ms ON g.`modelServiceUuid` = ms.`uuid`
LEFT JOIN `zstack`.`ModelVO` m ON g.`modelUuid` = m.`uuid`
SET g.`zoneUuid` = COALESCE(ms.`zoneUuid`, m.`zoneUuid`)
WHERE g.`zoneUuid` IS NULL;

UPDATE `zstack`.`ModelServiceInstanceGroupVO` g
INNER JOIN (
    SELECT i.`modelServiceGroupUuid`, MIN(vm.`zoneUuid`) AS `zoneUuid`, COUNT(DISTINCT vm.`zoneUuid`) AS `zoneCount`
    FROM `zstack`.`ModelServiceInstanceVO` i
    INNER JOIN `zstack`.`VmInstanceVO` vm ON vm.`uuid` = i.`vmInstanceUuid`
    WHERE vm.`zoneUuid` IS NOT NULL
    GROUP BY i.`modelServiceGroupUuid`
) inferred ON inferred.`modelServiceGroupUuid` = g.`uuid`
SET g.`zoneUuid` = inferred.`zoneUuid`
WHERE g.`zoneUuid` IS NULL
  AND inferred.`zoneCount` = 1;

CALL ADD_CONSTRAINT('ModelVO', 'fkModelVOZoneVO', 'zoneUuid', 'ZoneEO', 'uuid', 'RESTRICT');
CALL ADD_CONSTRAINT('ModelServiceVO', 'fkModelServiceVOZoneVO', 'zoneUuid', 'ZoneEO', 'uuid', 'RESTRICT');
CALL ADD_CONSTRAINT('DatasetVO', 'fkDatasetVOZoneVO', 'zoneUuid', 'ZoneEO', 'uuid', 'RESTRICT');
CALL ADD_CONSTRAINT('ModelServiceInstanceGroupVO', 'fkModelServiceInstanceGroupVOZoneVO', 'zoneUuid', 'ZoneEO', 'uuid', 'RESTRICT');

-- ZSTAC-84111: Persist Zaku health state on NativeClusterVO for query and manual recovery.
CALL ADD_COLUMN('NativeClusterVO', 'zakuHealthStatus', 'VARCHAR(32)', 1, 'Unknown');
UPDATE `zstack`.`NativeClusterVO` SET `zakuHealthStatus` = 'Unknown' WHERE `zakuHealthStatus` IS NULL;
