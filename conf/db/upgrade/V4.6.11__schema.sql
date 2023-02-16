ALTER TABLE `zstack`.`L2NetworkEO` MODIFY `vSwitchType` varchar(32) NOT NULL DEFAULT 'LinuxBridge' AFTER `type`;
ALTER TABLE `zstack`.`L2NetworkEO` ADD COLUMN `virtualNetworkId` int unsigned NOT NULL DEFAULT 0 AFTER `vSwitchType`;
UPDATE `zstack`.`L2NetworkEO` l2 INNER JOIN `zstack`.`L2VlanNetworkVO` vlan ON l2.uuid = vlan.uuid SET l2.virtualNetworkId = vlan.vlan;
UPDATE `zstack`.`L2NetworkEO` l2 INNER JOIN `zstack`.`VxlanNetworkVO` vxlan ON l2.uuid = vxlan.uuid SET l2.virtualNetworkId = vxlan.vni;
DROP VIEW IF EXISTS `zstack`.L2NetworkVO;
CREATE VIEW `zstack`.`L2NetworkVO` AS SELECT uuid, name, description, type, vSwitchType, virtualNetworkId, zoneUuid, physicalInterface, createDate, lastOpDate FROM `zstack`.`L2NetworkEO` WHERE deleted IS NULL;

DELIMITER $$
CREATE PROCEDURE addColumnsToSNSTextTemplateVO()
    BEGIN
        IF NOT EXISTS( SELECT 1
                       FROM INFORMATION_SCHEMA.COLUMNS
                       WHERE table_name = 'SNSTextTemplateVO'
                             AND table_schema = 'zstack'
                             AND column_name = 'subject') THEN

           ALTER TABLE `zstack`.`SNSTextTemplateVO` ADD COLUMN `subject` VARCHAR(2048);
        END IF;
        IF NOT EXISTS( SELECT 1
                       FROM INFORMATION_SCHEMA.COLUMNS
                       WHERE table_name = 'SNSTextTemplateVO'
                             AND table_schema = 'zstack'
                             AND column_name = 'recoverySubject') THEN

           ALTER TABLE `zstack`.`SNSTextTemplateVO` ADD COLUMN `recoverySubject` VARCHAR(2048);
        END IF;
    END $$
DELIMITER ;

call addColumnsToSNSTextTemplateVO();
DROP PROCEDURE IF EXISTS addColumnsToSNSTextTemplateVO;