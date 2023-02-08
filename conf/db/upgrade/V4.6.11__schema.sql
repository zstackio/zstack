ALTER TABLE `zstack`.`L2NetworkEO` MODIFY `vSwitchType` varchar(32) NOT NULL DEFAULT 'LinuxBridge' AFTER `type`;
ALTER TABLE `zstack`.`L2NetworkEO` ADD COLUMN `virtualNetworkId` int unsigned NOT NULL DEFAULT 0 AFTER `vSwitchType`;
UPDATE `zstack`.`L2NetworkEO` l2 INNER JOIN `zstack`.`L2VlanNetworkVO` vlan ON l2.uuid = vlan.uuid SET l2.virtualNetworkId = vlan.vlan;
UPDATE `zstack`.`L2NetworkEO` l2 INNER JOIN `zstack`.`VxlanNetworkVO` vxlan ON l2.uuid = vxlan.uuid SET l2.virtualNetworkId = vxlan.vni;
DROP VIEW IF EXISTS `zstack`.L2NetworkVO;
CREATE VIEW `zstack`.`L2NetworkVO` AS SELECT uuid, name, description, type, vSwitchType, virtualNetworkId, zoneUuid, physicalInterface, createDate, lastOpDate FROM `zstack`.`L2NetworkEO` WHERE deleted IS NULL;
