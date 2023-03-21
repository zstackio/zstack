ALTER TABLE `zstack`.`SNSTopicVO` ADD COLUMN `locale` varchar(32);

ALTER TABLE `zstack`.`HostNumaNodeVO` MODIFY COLUMN `nodeCPUs` TEXT NOT NULL;
ALTER TABLE `zstack`.`VmInstanceNumaNodeVO` MODIFY COLUMN `vNodeCPUs` TEXT NOT NULL;


ALTER TABLE `zstack`.`UsedIpVO` MODIFY COLUMN `ipRangeUuid` varchar(32) DEFAULT NULL;

ALTER TABLE `zstack`.`L3NetworkEO` ADD COLUMN `enableIPAM` boolean NOT NULL DEFAULT TRUE;
DROP VIEW IF EXISTS `zstack`.`L3NetworkVO`;
CREATE VIEW `zstack`.`L3NetworkVO` AS SELECT uuid, name, description, state, type, zoneUuid, l2NetworkUuid, system, dnsDomain, createDate, lastOpDate, category, ipVersion, enableIPAM FROM `zstack`.`L3NetworkEO` WHERE deleted IS NULL;
ALTER TABLE `zstack`.`UsedIpVO` DROP FOREIGN KEY fkUsedIpVOVmNicVO;
ALTER TABLE `zstack`.`UsedIpVO` ADD CONSTRAINT fkUsedIpVOVmNicVO FOREIGN KEY (vmNicUuid) REFERENCES VmNicVO (uuid) ON DELETE CASCADE;

INSERT INTO SystemTagVO (`uuid`, `resourceUuid`, `resourceType`, `inherent`, `type`, `tag`, `createDate`, `lastOpDate`)
SELECT REPLACE(UUID(),'-',''), vm.uuid, 'VmInstanceVO', 0, 'System', 'vRingBufferSize::256::256', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()
FROM VmInstanceVO vm LEFT JOIN SystemTagVO st ON st.resourceUuid = vm.uuid AND st.tag LIKE 'vRingBufferSize::%' WHERE vm.state = 'running' AND st.uuid IS NULL;