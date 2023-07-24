CREATE TABLE IF NOT EXISTS `zstack`.`XmlHookVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) UNIQUE NOT NULL,
    `description` varchar(2048) NULL,
    `type` varchar(32) NOT NULL,
    `hookScript` text NOT NULL,
    `libvirtVersion` varchar(32) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`XmlHookVmInstanceRefVO` (
    `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
    `xmlHookUuid` varchar(32) NOT NULL,
    `vmInstanceUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`id`),
    UNIQUE KEY `id` (`id`),
    KEY `fkXmlHookVmInstanceRefVOXmlHookVO` (`xmlHookUuid`),
    KEY `fkXmlHookVmInstanceRefVOVmInstanceVO` (`vmInstanceUuid`),
    CONSTRAINT `fkXmlHookVmInstanceRefVO` FOREIGN KEY (`xmlHookUuid`) REFERENCES `XmlHookVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkXmlHookVmInstanceRefVO1` FOREIGN KEY (`vmInstanceUuid`) REFERENCES `ResourceVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;