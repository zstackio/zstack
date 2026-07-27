CREATE TABLE IF NOT EXISTS `NetworkSecurityPolicyScheduleVO` (
    `uuid` varchar(32) NOT NULL,
    `name` varchar(255) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `resourceType` varchar(32) NOT NULL,
    `resourceUuid` varchar(32) NOT NULL,
    `timeType` varchar(32) NOT NULL,
    `repeatType` varchar(32) NOT NULL,
    `startDate` date NOT NULL,
    `endDate` date NOT NULL,
    `startTime` time NOT NULL,
    `endTime` time NOT NULL,
    `weekDays` varchar(32) DEFAULT NULL,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00'
        ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `SecurityGroupVO`
    ADD COLUMN `scheduleUuid` varchar(32) DEFAULT NULL;

ALTER TABLE `VpcFirewallRuleSetVO`
    ADD COLUMN `scheduleUuid` varchar(32) DEFAULT NULL;
