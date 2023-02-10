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