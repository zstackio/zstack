CREATE TABLE IF NOT EXISTS `zstack`.`AiSiNoSecretResourcePoolVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `managementIp` varchar(32) NOT NULL,
    `port` int unsigned NOT NULL,
    `route` varchar(32) NOT NULL,
    `clientID` varchar(32) NOT NULL,
    `clientSecrete` varchar(32) NOT NULL,
    `appId` varchar(8) NOT NULL,
    `keyNumSM2` varchar(8) NOT NULL,
    `keyNumSM4` varchar(8) NOT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT fkAiSiNoSecretResourcePoolVOSecretResourcePoolVO FOREIGN KEY (uuid) REFERENCES SecretResourcePoolVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
ALTER TABLE SecretResourcePoolVO ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT 'Connected';

CREATE TABLE IF NOT EXISTS `zstack`.`SanSecSecretResourcePoolVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `keyIndex` varchar(128) DEFAULT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT fkSanSecSecretResourcePoolVOSecretResourcePoolVO FOREIGN KEY (uuid) REFERENCES SecretResourcePoolVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`SanSecSecretResourcePoolVO` ADD COLUMN `managementIp` varchar(128) DEFAULT NULL;
ALTER TABLE `zstack`.`SanSecSecretResourcePoolVO` ADD COLUMN `port` int unsigned DEFAULT NULL;
ALTER TABLE `zstack`.`SanSecSecretResourcePoolVO` ADD COLUMN `username` varchar(128) DEFAULT NULL;
ALTER TABLE `zstack`.`SanSecSecretResourcePoolVO` ADD COLUMN `password` varchar(128) DEFAULT NULL;
ALTER TABLE `zstack`.`SanSecSecretResourcePoolVO` ADD COLUMN `sm3Key` varchar(128) DEFAULT NULL;
ALTER TABLE `zstack`.`SanSecSecretResourcePoolVO` ADD COLUMN `sm4Key` varchar(128) DEFAULT NULL;

ALTER TABLE `zstack`.`CCSCertificateVO` MODIFY COLUMN issuerDN varchar(255) NOT NULL;
ALTER TABLE `zstack`.`CCSCertificateVO` MODIFY COLUMN subjectDN varchar(255) NOT NULL;
ALTER TABLE `zstack`.`CCSCertificateVO` MODIFY COLUMN serNumber varchar(128) unsigned NOT NULL;