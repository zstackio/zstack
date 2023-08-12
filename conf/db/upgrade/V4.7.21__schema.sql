CREATE TABLE IF NOT EXISTS `zstack`.`SnmpAgentConfigVO` (
    `uuid` varchar(32) NOT NULL UNIQUE COMMENT 'uuid',
    `version` varchar(32) NOT NULL COMMENT 'snmp authentication version',
    `readCommunity` varchar(32) DEFAULT NULL,
    `userName` varchar(32) DEFAULT NULL,
    `authAlgorithm` varchar(32) DEFAULT NULL,
    `authPassword` varchar(32) DEFAULT NULL,
    `privacyAlgorithm` varchar(32) DEFAULT NULL,
    `privacyPassword` varchar(32) DEFAULT NULL,
    `port` int(10) DEFAULT NULL,
    `securityLevel` varchar(32) DEFAULT NULL,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`SNSSnmpTrapReceiverVO` (
    `uuid` varchar(32) NOT NULL,
    `ipAddress` varchar(128) NOT NULL,
    `port` smallint unsigned NOT NULL,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY `ukipAddrPort` (`ipAddress`,`port`) USING BTREE,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`SNSSnmpEndpointVO` (
    `uuid` varchar(32) NOT NULL,
    `trapReceiverUuid` varchar(32) DEFAULT NULL,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT `fkSNSSnmpEndpointVOSNSApplicationEndpointVO` FOREIGN KEY (`trapReceiverUuid`) REFERENCES SNSSnmpTrapReceiverVO(`uuid`) ON UPDATE RESTRICT ON DELETE CASCADE,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;