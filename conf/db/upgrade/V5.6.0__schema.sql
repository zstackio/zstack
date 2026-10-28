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
