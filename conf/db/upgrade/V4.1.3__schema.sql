DELIMITER $$
CREATE PROCEDURE upgradeSystemEndpointNotifier()
BEGIN
    DECLARE oldAppUuid VARCHAR(32);
    SELECT `uuid` INTO oldAppUuid FROM `SNSHttpEndpointVO` WHERE `url` = 'http://system-endpoint.XXX_XXX.default' LIMIT 1;
    read_loop: LOOP
        IF oldAppUuid IS NULL THEN
            LEAVE read_loop;
        END IF;
        DELETE FROM `SNSSubscriberVO` WHERE `endpointUuid` = oldAppUuid;
        DELETE FROM `SNSApplicationEndpointVO` WHERE `uuid` = oldAppUuid;
        DELETE FROM `ResourceVO` WHERE `uuid` = oldAppUuid;
        DELETE FROM `SNSHttpEndpointVO` WHERE `uuid` = oldAppUuid;
        DELETE FROM `AccountResourceRefVO` WHERE `resourceUuid` = oldAppUuid;
        DELETE FROM `SharedResourceVO` WHERE `resourceUuid` = oldAppUuid;
        LEAVE read_loop;
    END LOOP;
    SELECT CURTIME();
END $$
DELIMITER ;
CALL upgradeSystemEndpointNotifier();

CREATE TABLE IF NOT EXISTS `zstack`.`AlarmRecordsVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `createTime` bigint(20) NOT NULL,
  `accountUuid` varchar(32) DEFAULT NULL,
  `alarmName` varchar(255) NOT NULL,
  `alarmStatus` varchar(64) DEFAULT NULL,
  `alarmUuid` varchar(32) DEFAULT NULL,
  `comparisonOperator` varchar(128) DEFAULT NULL,
  `context` text,
  `dataUuid` varchar(32) DEFAULT NULL,
  `emergencyLevel` varchar(64) DEFAULT NULL,
  `labels` text,
  `metricName` varchar(256) DEFAULT NULL,
  `metricValue` double DEFAULT NULL,
  `namespace` varchar(256) DEFAULT NULL,
  `period` int(10) unsigned NOT NULL,
  `readStatus` tinyint(1) unsigned NOT NULL DEFAULT '0',
  `resourceType` varchar(256) NOT NULL,
  `resourceUuid` varchar(256) DEFAULT NULL,
  `threshold` double NOT NULL,
  `hour` int(10) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `idxDataUuid` (`dataUuid`),
  KEY `idxCreateTime` (`createTime`),
  KEY `idxAccountUuid` (`accountUuid`),
  KEY `idxAccountUuidCreateTime` (`accountUuid`,`createTime`),
  KEY `idxAlarmUuid` (`alarmUuid`),
  KEY `idxAccountUuidHourEmergencyLevel` (`accountUuid`,`hour`,`emergencyLevel`),
  KEY `idxCreateTimeReadStatusEmergencyLevel` (`createTime`,`emergencyLevel`,`readStatus`,`accountUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`EventRecordsVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `createTime` bigint(20) NOT NULL,
  `accountUuid` varchar(32) DEFAULT NULL,
  `dataUuid` varchar(32) DEFAULT NULL,
  `emergencyLevel` varchar(64) DEFAULT NULL,
  `name` varchar(256) DEFAULT NULL,
  `error` text,
  `labels` text,
  `namespace` varchar(256) DEFAULT NULL,
  `readStatus` tinyint(1) unsigned NOT NULL DEFAULT '0',
  `resourceId` varchar(32) DEFAULT NULL,
  `resourceName` varchar(256) DEFAULT NULL,
  `subscriptionUuid` varchar(32) DEFAULT NULL,
  `hour` int(10) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `idxDataUuid` (`dataUuid`),
  KEY `idxCreateTime` (`createTime`),
  KEY `idxAccountUuid` (`accountUuid`),
  KEY `idxAccountUuidCreateTime` (`accountUuid`,`createTime`),
  KEY `idxName` (`name`(255)),
  KEY `idxSubscriptionUuid` (`subscriptionUuid`),
  KEY `idxAccountUuidHourEmergencyLevel` (`accountUuid`,`hour`,`emergencyLevel`),
  KEY `idxCreateTimeReadStatusEmergencyLevel` (`createTime`,`emergencyLevel`,`readStatus`,`accountUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AuditsVO` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `createTime` bigint(20) NOT NULL,
  `apiName` varchar(2048) NOT NULL,
  `clientBrowser` varchar(64) NOT NULL,
  `clientIp` varchar(64) NOT NULL,
  `duration` int(10) unsigned NOT NULL,
  `error` text,
  `operator` varchar(256) DEFAULT NULL,
  `requestDump` text,
  `resourceType` varchar(256) NOT NULL,
  `resourceUuid` varchar(32) DEFAULT NULL,
  `requestUuid` varchar(32) DEFAULT NULL,
  `responseDump` text,
  `success` tinyint(1) unsigned NOT NULL DEFAULT '0' COMMENT 'api call success or failed',
  `operatorAccountUuid` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id` (`id`),
  KEY `idxCreateTime` (`createTime`),
  KEY `idxResourceUuid` (`resourceUuid`),
  KEY `idxSuccess` (`success`),
  KEY `idxOperatorAccountUuid` (`operatorAccountUuid`),
  KEY `idxRequestUuid` (`requestUuid`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

alter table AliyunProxyVpcVO modify vRouterUuid varchar(32) null;
alter table AliyunProxyVpcVO drop foreign key fkAliyunProxyVpcVOVmInstanceEO;
alter table AliyunProxyVpcVO add constraint fkAliyunProxyVpcVOVmInstanceEO foreign key (vRouterUuid) references VmInstanceEO (uuid) on delete set null;


ALTER TABLE `zstack`.`AccessControlListEntryVO` ADD COLUMN `type` varchar (32) NOT NULL DEFAULT 'IpEntry';
ALTER TABLE `zstack`.`AccessControlListEntryVO` MODIFY COLUMN `ipEntries` varchar (2048) DEFAULT NULL;
ALTER TABLE `zstack`.`AccessControlListEntryVO` ADD COLUMN `name` varchar (32) DEFAULT NULL;
ALTER TABLE `zstack`.`AccessControlListEntryVO` ADD COLUMN `matchMethod` varchar (32) DEFAULT NULL;
ALTER TABLE `zstack`.`AccessControlListEntryVO` ADD COLUMN `criterion` varchar (32) DEFAULT NULL;
ALTER TABLE `zstack`.`AccessControlListEntryVO` ADD COLUMN `domain` varchar (255) DEFAULT NULL;
ALTER TABLE `zstack`.`AccessControlListEntryVO` ADD COLUMN `url` varchar (255) DEFAULT NULL;
ALTER TABLE `zstack`.`AccessControlListEntryVO` ADD COLUMN `redirectRule` varchar (1024) DEFAULT NULL;

ALTER TABLE `zstack`.`LoadBalancerListenerACLRefVO` ADD COLUMN `serverGroupUuid` varchar (32) DEFAULT NULL;
ALTER TABLE `zstack`.`LoadBalancerListenerACLRefVO` ADD CONSTRAINT fkLoadBalancerListenerACLRefVOLoadBalancerServerGroupVO FOREIGN KEY (serverGroupUuid) REFERENCES `zstack`.`LoadBalancerServerGroupVO` (uuid) ON DELETE CASCADE;

ALTER TABLE `zstack`.`LoadBalancerListenerACLRefVO` DROP FOREIGN KEY fkLoadbalancerListenerACLRefVOAccessControlListVO;
ALTER TABLE `zstack`.`LoadBalancerListenerACLRefVO` ADD CONSTRAINT fkLoadbalancerListenerACLRefVOAccessControlListVO FOREIGN KEY (aclUuid) REFERENCES `zstack`.`AccessControlListVO` (uuid) ON DELETE CASCADE;

ALTER TABLE QuotaVO MODIFY COLUMN `value` bigint DEFAULT 0;
