ALTER TABLE AutoScalingRuleSchedulerJobTriggerVO DROP FOREIGN KEY fkAutoScalingRuleSchedulerJobTriggerVO;
CALL ADD_CONSTRAINT('AutoScalingRuleSchedulerJobTriggerVO', 'fkAutoScalingRuleSchedulerJobTriggerVO', 'schedulerJobUuid', 'SchedulerJobVO', 'uuid', 'CASCADE');

ALTER TABLE `zstack`.`ExternalPrimaryStorageVO` MODIFY COLUMN `config` TEXT DEFAULT NULL;
ALTER TABLE `zstack`.`HostNetworkInterfaceLldpRefVO` MODIFY COLUMN `systemName` VARCHAR(255) NOT NULL;

CREATE TABLE IF NOT EXISTS `zstack`.`ExternalPrimaryStorageHostRefVO` (
    `id`       BIGINT UNSIGNED UNIQUE,
    `hostId`   INT          DEFAULT NULL,
    `protocol` varchar(128) DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;

SET @row_number = 0;
INSERT INTO ExternalPrimaryStorageHostRefVO (id, hostId, protocol)
SELECT
    p.id,
    (@row_number := @row_number + 1) as hostId,
    e.defaultProtocol as protocol
FROM PrimaryStorageHostRefVO p LEFT JOIN ExternalPrimaryStorageVO e ON p.primaryStorageUuid = e.uuid
ORDER BY p.id;

CREATE TABLE IF NOT EXISTS `zstack`.`CbtTaskVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) NOT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `status` varchar(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`CbtTaskResourceRefVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `taskUuid` varchar(32) NOT NULL,
    `resourceUuid` varchar(32) NOT NULL,
    `resourceType` varchar(255) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`id`),
    INDEX `idxCbtTaskResourceRefVOtaskUuid` (`taskUuid`),
    INDEX `idxCbtTaskResourceRefVOresourceUuid` (`resourceUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;