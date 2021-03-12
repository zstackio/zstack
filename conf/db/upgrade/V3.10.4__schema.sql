CREATE TABLE IF NOT EXISTS `zstack`.`AutoScalingRuleSchedulerJobTriggerVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `schedulerJobUuid` VARCHAR(32) NOT NULL UNIQUE,
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkAutoScalingRuleSchedulerJobTriggerVO` FOREIGN KEY (`schedulerJobUuid`) REFERENCES `SchedulerJobVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
