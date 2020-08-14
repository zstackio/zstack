
ALTER TABLE `zstack`.`LoadBalancerVO` DROP FOREIGN KEY `fkLoadBalancerVOVipVO`;
ALTER TABLE `zstack`.`LoadBalancerVO` ADD CONSTRAINT `fkLoadBalancerVOVipVO` FOREIGN KEY (`vipUuid`) REFERENCES `zstack`.`VipVO` (`uuid`) ON DELETE CASCADE;

CREATE TABLE IF NOT EXISTS `zstack`.`SNSMicrosoftTeamsEndpointVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `url` varchar(1024) NOT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT fkSNSMicrosoftTeamsEndpointVOSNSApplicationEndpointVO FOREIGN KEY (uuid) REFERENCES SNSApplicationEndpointVO (uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ThirdpartyPlatformVO` (
  `uuid` varchar(32) NOT NULL,
  `name` varchar(255) NOT NULL,
  `type` varchar(255) NOT NULL,
  `state` varchar(255) NOT NULL,
  `url` varchar(512) NOT NULL,
  `template` varchar(4096) NOT NULL,
  `description` varchar(2048) DEFAULT NULL,
  `lastSyncDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ThirdpartyOriginalAlertVO` (
  `uuid` varchar(32) NOT NULL,
  `thirdpartyPlatformUuid` varchar(32) NOT NULL,
  `product` varchar(255) NOT NULL,
  `service` varchar(255) DEFAULT NULL,
  `metric` varchar(512) DEFAULT NULL,
  `alertLevel` varchar(64) NOT NULL,
  `alertTime` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `dimensions` varchar(4096) DEFAULT NULL,
  `message` varchar(4096) NOT NULL,
  `dataSource` varchar(255) NOT NULL,
  `sourceText` text DEFAULT NULL,
  `readStatus` varchar(32) NOT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  PRIMARY KEY (`uuid`),
  UNIQUE KEY `uuid` (`uuid`),
  CONSTRAINT `fkThirdpartyAlertVOThirdpartyPlatformVO` FOREIGN KEY (`thirdpartyPlatformUuid`) REFERENCES `ThirdpartyPlatformVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`SNSEndpointThirdpartyAlertHistoryVO` (
  `endpointUuid` varchar(32) NOT NULL,
  `alertUuid` varchar(32) NOT NULL,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00'
) ENGINE=InnoDB DEFAULT CHARSET=utf8;