CREATE TABLE IF NOT EXISTS `zstack`.`SnmpAgentVO` (
    `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'uuid',
    `version` varchar(32) NOT NULL COMMENT 'snmp authentication version',
    `readCommunity` varchar(32) DEFAULT NULL,
    `userName` varchar(32) DEFAULT NULL,
    `authAlgorithm` varchar(32) DEFAULT NULL,
    `authPassword` varchar(32) DEFAULT NULL,
    `privacyAlgorithm` varchar(32) DEFAULT NULL,
    `privacyPassword` varchar(32) DEFAULT NULL,
    `status` varchar(32) NOT NULL COMMENT 'SNMP agent status. status is enable which means start snmp with mn start.',
    `port` int(10) DEFAULT NULL,
    `securityLevel` varchar(32) DEFAULT NULL,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`SNSSnmpPlatformVO`
(
    `uuid`        varchar(32)       NOT NULL,
    `snmpAddress` varchar(128)      NOT NULL,
    `snmpPort`    smallint unsigned NOT NULL,
    `createDate`  timestamp         NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate`  timestamp         NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `ukipAddrPort` (`snmpAddress`, `snmpPort`) USING BTREE,
    PRIMARY KEY (`uuid`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;