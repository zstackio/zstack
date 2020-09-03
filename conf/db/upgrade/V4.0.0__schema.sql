CREATE TABLE IF NOT EXISTS `zstack`.`VpcFirewallIpSetTemplateVO`
(
    `uuid`        varchar(32)  NOT NULL,
    `name`        varchar(255) NOT NULL,
    `sourceValue` varchar(2048)         DEFAULT NULL,
    `destValue`   varchar(2048)         DEFAULT NULL,
    `type`        varchar(255) NOT NULL,
    `createDate`  timestamp    NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate`  timestamp    NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `uuid` (`uuid`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`VpcFirewallRuleTemplateVO`
(
    `uuid`         varchar(32)  NOT NULL,
    `action`       varchar(255) NOT NULL,
    `name`         varchar(255) NOT NULL,
    `protocol`     varchar(255)          DEFAULT NULL,
    `sourcePort`   varchar(255)          DEFAULT NULL,
    `destPort`     varchar(255)          DEFAULT NULL,
    `sourceIp`     varchar(2048)         DEFAULT NULL,
    `destIp`       varchar(2048)         DEFAULT NULL,
    `ruleNumber`   int(10)      NOT NULL,
    `icmpTypeName` varchar(255)          DEFAULT NULL,
    `allowStates`  varchar(255)          DEFAULT NULL,
    `tcpFlag`      varchar(255)          DEFAULT NULL,
    `enableLog`    tinyint(1)   NOT NULL DEFAULT '0',
    `state`        varchar(32)  NOT NULL DEFAULT '0',
    `isDefault`    tinyint(1)   NOT NULL DEFAULT '0',
    `description`  varchar(2048)         DEFAULT NULL,
    `createDate`   timestamp    NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate`   timestamp    NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `uuid` (`uuid`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;

ALTER TABLE `zstack`.`VpcFirewallRuleSetVO` ADD COLUMN `isApplied` boolean NOT NULL DEFAULT TRUE;
ALTER TABLE `zstack`.`VpcFirewallRuleSetVO` MODIFY `actionType` varchar(255) DEFAULT NULL;
ALTER TABLE `zstack`.`VpcFirewallRuleVO` ADD COLUMN `isApplied` boolean NOT NULL DEFAULT TRUE;
ALTER TABLE `zstack`.`VpcFirewallRuleVO` ADD COLUMN `expired` boolean NOT NULL DEFAULT FALSE;

ALTER TABLE `zstack`.`VpcFirewallRuleSetVO` DROP FOREIGN KEY fkVpcFirewallRuleSetVOVpcFirewallVO;
ALTER TABLE `zstack`.`VpcFirewallRuleVO` DROP FOREIGN KEY fkVpcFirewallRuleVOVpcFirewallVO;
ALTER TABLE `zstack`.`VpcFirewallRuleVO` DROP FOREIGN KEY fkVpcFirewallRuleVOVpcFirewallRuleSetVO;
ALTER TABLE `VpcFirewallRuleSetVO` DROP INDEX `fkVpcFirewallRuleSetVOVpcFirewallVO`;
ALTER TABLE `VpcFirewallRuleVO` DROP INDEX `fkVpcFirewallRuleVOVpcFirewallVO`;
ALTER TABLE `VpcFirewallRuleVO` DROP INDEX `fkVpcFirewallRuleVOVpcFirewallRuleSetVO`;
ALTER TABLE `zstack`.`VpcFirewallRuleSetVO` DROP COLUMN `vyosName`;
ALTER TABLE `zstack`.`VpcFirewallRuleSetVO` DROP COLUMN `vpcFirewallUuid`;
ALTER TABLE `zstack`.`VpcFirewallRuleVO` DROP COLUMN `vpcFirewallUuid`;
ALTER TABLE `zstack`.`VpcFirewallRuleVO` DROP COLUMN `ruleSetName`;