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

CREATE TABLE IF NOT EXISTS `zstack`.`FlkSecSecretResourcePoolVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `encryptResult` varchar(64) DEFAULT NULL,
    `activatedToken` varchar(32) DEFAULT NULL,
    `protectToken` varchar(32) DEFAULT NULL,
    `hmacToken` varchar(32) DEFAULT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT fkFlkSecSecretResourcePoolVOSecretResourcePoolVO FOREIGN KEY (uuid) REFERENCES SecretResourcePoolVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`FlkSecSecurityMachineVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `port` int unsigned NOT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT fkFlkSecSecurityMachineVOSecurityMachineVO FOREIGN KEY (uuid) REFERENCES SecurityMachineVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`CCSCertificateVO` MODIFY COLUMN issuerDN varchar(255) NOT NULL;
ALTER TABLE `zstack`.`CCSCertificateVO` MODIFY COLUMN subjectDN varchar(255) NOT NULL;
ALTER TABLE `zstack`.`CCSCertificateVO` MODIFY COLUMN serNumber varchar(128) NOT NULL;

ALTER TABLE `zstack`.`ESXHostVO` ADD COLUMN `esxiVersion` varchar(32);

ALTER TABLE `zstack`.`CephOsdGroupVO` MODIFY COLUMN `osds` text NOT NULL;

CREATE TABLE IF NOT EXISTS `zstack`.`GuestToolsStateVO` (
    `vmInstanceUuid` varchar(32) NOT NULL UNIQUE,
    `state` varchar(32) NOT NULL DEFAULT 'Unknown',
    `version` varchar(32),
    `platform` varchar(32),
    `osType` varchar(32),
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`vmInstanceUuid`),
    CONSTRAINT `fkGuestToolsStateVOVmInstanceEO` FOREIGN KEY (`vmInstanceUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON UPDATE RESTRICT ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;
