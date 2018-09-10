CREATE TABLE `AutoScalingGroupVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `name` VARCHAR(256) NOT NULL,
    `scalingResourceType` VARCHAR(256) NOT NULL,
    `removalPolicy` VARCHAR(256) NOT NULL,
    `minResourceSize` int(10) NOT NULL,
    `maxResourceSize` int(10) NOT NULL,
    `state` VARCHAR(256) NOT NUll,
    `defaultCooldown` LONG NOT NULL,
    `description` VARCHAR(256) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AutoScalingTemplateVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `name` VARCHAR(256) NOT NULL,
    `type` VARCHAR(256) NOT NULL,
    `state` VARCHAR(256) NOT NULL,
    `description` VARCHAR(256) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `AutoScalingTemplateGroupRefVO` (
    `groupUuid` varchar(32) NOT NULL UNIQUE,
    `templateUuid` varchar(32) NOT NULL,
    `lastOpDate` TIMESTAMP NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` TIMESTAMP NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`groupUuid`),
    CONSTRAINT `fkAutoScalingTemplateGroupRefVOAutoScalingGroupVO` FOREIGN KEY (`groupUuid`) REFERENCES `AutoScalingGroupVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkAutoScalingTemplateGroupRefVOAutoScalingTemplateVO` FOREIGN KEY (`templateUuid`) REFERENCES `AutoScalingTemplateVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AutoScalingVmTemplateVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `vmInstanceName` VARCHAR(256) NOT NULL,
    `vmInstanceDescription` VARCHAR(256) DEFAULT NULL,
    `vmInstanceType` VARCHAR(256) NOT NULL,
    `vmInstanceOfferingUuid` VARCHAR(32) NOT NULL,
    `imageUuid` VARCHAR(32) NOT NULL,
    `l3NetworkUuids` text DEFAULT NULL,
    `rootDiskOfferingUuid` VARCHAR(32) DEFAULT NULL,
    `dataDiskOfferingUuids` text DEFAULT NULL,
    `vmInstanceZoneUuid` VARCHAR(32) DEFAULT NULL,
    `vmInstanceClusterUuid` VARCHAR(32) DEFAULT NULL,
    `hostUuid` VARCHAR(32) DEFAULT NULL,
    `primaryStorageUuidForRootVolume` VARCHAR(32) DEFAULT NULL,
    `defaultL3NetworkUuid` VARCHAR(32) DEFAULT NULL,
    `strategy` VARCHAR(32) DEFAULT NULL,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AutoScalingRuleVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `name` VARCHAR(256) NOT NULL,
    `scalingGroupUuid` VARCHAR(32) NOT NULL,
    `type` VARCHAR(256) NOT NULL,
    `description` VARCHAR(256) DEFAULT NULL,
    `cooldown` LONG DEFAULT NULL,
    `state` VARCHAR(256) NOT NULL,
    `status` VARCHAR(256) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkAutoScalingRuleVOAutoScalingGroupVO` FOREIGN KEY (`scalingGroupUuid`) REFERENCES `AutoScalingGroupVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AutoScalingGroupActivityVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `scalingGroupUuid` VARCHAR(32) NOT NULL,
    `activityAction` VARCHAR(128) NOT NULL,
    `scalingGroupRuleUuid` VARCHAR(32) DEFAULT NULL,
    `name` VARCHAR(256) NOT NULL,
    `cause` VARCHAR(128) NOT NULL,
    `status` VARCHAR(128) NOT NULL,
    `activityActionResultMessage` text DEFAULT NULL,
    `description` VARCHAR(256) DEFAULT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    `endDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkAutoScalingGroupActivityVOAutoScalingGroupVO` FOREIGN KEY (`scalingGroupUuid`) REFERENCES `AutoScalingGroupVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkAutoScalingGroupActivityVOAutoScalingRuleVO` FOREIGN KEY (`scalingGroupRuleUuid`) REFERENCES `AutoScalingRuleVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AutoScalingGroupInstanceVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `instanceUuid` VARCHAR(32) NOT NULL UNIQUE,
    `scalingGroupUuid` VARCHAR(32) NOT NULL,
    `templateUuid` VARCHAR(32) DEFAULT NULL,
    `scalingGroupActivityUuid` VARCHAR(32) NOT NULL,
    `status` VARCHAR(64) NOT NULL,
    `healthStatus` VARCHAR(64) NOT NULL,
    `lastOpDate` TIMESTAMP NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` TIMESTAMP NOT NULL DEFAULT '0000-00-00 00:00:00',
    `description` VARCHAR(256) DEFAULT NULL,
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkAutoScalingGroupInstanceVOAutoScalingGroupVO` FOREIGN KEY (`scalingGroupUuid`) REFERENCES `AutoScalingGroupVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkAutoScalingGroupInstanceVOAutoScalingTemplateVO` FOREIGN KEY (`templateUuid`) REFERENCES `AutoScalingTemplateVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkAutoScalingGroupInstanceVOAutoScalingGroupActivityVO` FOREIGN KEY (`scalingGroupActivityUuid`) REFERENCES `AutoScalingGroupActivityVO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AddingNewInstanceRuleVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `AdjustmentType` VARCHAR(256) NOT NULL,
    `adjustmentValue` int(10) NOT NULL,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `RemovalInstanceRuleVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `AdjustmentType` VARCHAR(256) NOT NULL,
    `adjustmentValue` int(10) NOT NULL,
    `removalPolicy` VARCHAR(256) NOT NULL,
    PRIMARY KEY (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AutoScalingRuleTriggerVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `name` VARCHAR(256) NOT NULL,
    `ruleUuid` VARCHAR(32) NOT NULL,
    `type` VARCHAR(256) NOT NULL,
    `description` VARCHAR(256) DEFAULT NULL,
    `state` VARCHAR(256) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkAutoScalingRuleTriggerVOAutoScalingRuleVO` FOREIGN KEY (`ruleUuid`) REFERENCES `AutoScalingRuleVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AutoScalingRuleAlarmTriggerVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE,
    `alarmUuid` VARCHAR(32) NOT NULL UNIQUE,
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkAutoScalingRuleInstanceAlarmVO` FOREIGN KEY (`alarmUuid`) REFERENCES `AlarmVO` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;