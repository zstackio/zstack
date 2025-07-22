CREATE TABLE  `zstack`.`L3NetworkSequenceNumberVO` (
    `id` int unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`L3NetworkEO` ADD COLUMN `internalId` INT(32) unsigned DEFAULT 0;
DROP VIEW IF EXISTS `zstack`.`L3NetworkVO`;
CREATE VIEW `zstack`.`L3NetworkVO` AS SELECT uuid, name, internalId, description, state, type, zoneUuid, l2NetworkUuid, `system`, dnsDomain, createDate, lastOpDate, category, ipVersion, enableIPAM, isolated FROM `zstack`.`L3NetworkEO` WHERE deleted IS NULL;

ALTER TABLE SecurityGroupRuleVO MODIFY COLUMN `dstPortRange` varchar(1024) DEFAULT NULL;
ALTER TABLE SecurityGroupRuleVO MODIFY COLUMN `srcPortRange` varchar(1024) DEFAULT NULL;

CREATE TABLE IF NOT EXISTS `zstack`.`SAML2ClientVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `idpMetadataBase64` TEXT,
    `spX509Certificate` TEXT,
    `spPrivateKey` TEXT,
    `spMetadataUrl` varchar(256) DEFAULT NULL,
    `state` varchar(32) NOT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkSAMLClientVOSSOClientVO` FOREIGN KEY (`uuid`) REFERENCES `SSOClientVO` (`uuid`) ON UPDATE RESTRICT ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;