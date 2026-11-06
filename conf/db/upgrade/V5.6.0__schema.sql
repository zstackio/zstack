ALTER TABLE `ScsiLunVmInstanceRefVO` ADD COLUMN `diskMode` VARCHAR(32) NOT NULL DEFAULT 'scsi-block';

UPDATE `zstack`.`AffinityGroupVO` AS ag
SET ag.zoneUuid = COALESCE(
    (SELECT MIN(vm.zoneUuid)
     FROM `zstack`.`AffinityGroupUsageVO` AS ref
     JOIN `zstack`.`VmInstanceVO` AS vm ON vm.uuid = ref.resourceUuid
     WHERE ref.affinityGroupUuid = ag.uuid
     HAVING COUNT(DISTINCT vm.zoneUuid) = 1),
    (SELECT MIN(uuid) FROM `zstack`.`ZoneVO` HAVING COUNT(*) = 1))
WHERE ag.zoneUuid IS NULL;

UPDATE `zstack`.`VmSchedulingRuleGroupVO` AS ruleGroup
JOIN `zstack`.`AffinityGroupVO` AS ag ON ag.uuid = ruleGroup.srcUuid
SET ruleGroup.zoneUuid = ag.zoneUuid
WHERE ruleGroup.zoneUuid IS NULL AND ag.zoneUuid IS NOT NULL;

DROP PROCEDURE IF EXISTS mergeDuplicateScsiLuns;
DELIMITER $$
CREATE PROCEDURE mergeDuplicateScsiLuns()
BEGIN
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        DROP TEMPORARY TABLE IF EXISTS ScsiLunUpgradeGroup, ScsiLunUpgradeMap;
        RESIGNAL;
    END;

    START TRANSACTION;
    CREATE TEMPORARY TABLE ScsiLunUpgradeGroup AS
    SELECT l.source, COALESCE(i.iscsiTargetUuid, f.fiberChannelStorageUuid) AS scopeUuid,
           BINARY l.wwid AS wwid,
           COALESCE(MIN(CASE WHEN v.scsiLunUuid IS NOT NULL THEN l.uuid END), MIN(l.uuid)) AS keepUuid
    FROM LunVO l
    LEFT JOIN IscsiLunVO i ON i.uuid = l.uuid AND l.source = 'iSCSI'
    LEFT JOIN FiberChannelLunVO f ON f.uuid = l.uuid AND l.source = 'fiberChannel'
    LEFT JOIN (SELECT DISTINCT scsiLunUuid FROM ScsiLunVmInstanceRefVO) v ON v.scsiLunUuid = l.uuid
    WHERE l.source IN ('iSCSI', 'fiberChannel') AND l.wwid <> ''
      AND COALESCE(i.iscsiTargetUuid, f.fiberChannelStorageUuid) IS NOT NULL
    GROUP BY l.source, scopeUuid, BINARY l.wwid HAVING COUNT(*) > 1;

    CREATE TEMPORARY TABLE ScsiLunUpgradeMap AS
    SELECT l.uuid AS oldUuid, g.keepUuid
    FROM LunVO l
    LEFT JOIN IscsiLunVO i ON i.uuid = l.uuid AND l.source = 'iSCSI'
    LEFT JOIN FiberChannelLunVO f ON f.uuid = l.uuid AND l.source = 'fiberChannel'
    JOIN ScsiLunUpgradeGroup g ON g.source = l.source AND g.wwid = BINARY l.wwid
      AND g.scopeUuid = COALESCE(i.iscsiTargetUuid, f.fiberChannelStorageUuid)
    WHERE l.uuid <> g.keepUuid;

    DELETE r FROM ScsiLunHostRefVO r
    JOIN ScsiLunUpgradeMap m ON m.oldUuid = r.scsiLunUuid
    JOIN ScsiLunHostRefVO k ON k.scsiLunUuid = m.keepUuid AND k.hostUuid = r.hostUuid;
    UPDATE ScsiLunHostRefVO r JOIN ScsiLunUpgradeMap m ON m.oldUuid = r.scsiLunUuid
    SET r.scsiLunUuid = m.keepUuid;
    DELETE r FROM ScsiLunHostRefVO r
    JOIN ScsiLunHostRefVO k ON k.scsiLunUuid = r.scsiLunUuid AND k.hostUuid = r.hostUuid AND k.id < r.id
    JOIN ScsiLunUpgradeGroup g ON g.keepUuid = r.scsiLunUuid;

    UPDATE ScsiLunVmInstanceRefVO r JOIN ScsiLunUpgradeMap m ON m.oldUuid = r.scsiLunUuid
    SET r.scsiLunUuid = m.keepUuid, r.lastOpDate = r.lastOpDate;

    DELETE l FROM IscsiLunVO l JOIN ScsiLunUpgradeMap m ON m.oldUuid = l.uuid;
    DELETE l FROM FiberChannelLunVO l JOIN ScsiLunUpgradeMap m ON m.oldUuid = l.uuid;
    DELETE l FROM LunVO l JOIN ScsiLunUpgradeMap m ON m.oldUuid = l.uuid;
    DELETE r FROM ResourceVO r JOIN ScsiLunUpgradeMap m ON m.oldUuid = r.uuid;
    DROP TEMPORARY TABLE ScsiLunUpgradeMap, ScsiLunUpgradeGroup;
    COMMIT;
END$$
DELIMITER ;
CALL mergeDuplicateScsiLuns();
DROP PROCEDURE mergeDuplicateScsiLuns;
