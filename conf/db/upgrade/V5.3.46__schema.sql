CREATE TABLE IF NOT EXISTS `zstack`.`SAML2ClientVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `idpMetadataBase64` TEXT,
    `spX509Certificate` TEXT,
    `spPrivateKey` TEXT,
    `state` varchar(32) NOT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT `fkSAMLClientVOSSOClientVO` FOREIGN KEY (`uuid`) REFERENCES `SSOClientVO` (`uuid`) ON UPDATE RESTRICT ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;