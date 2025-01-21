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

-- Delete old UserTagVO of AI::Image-Generation
DELETE FROM UserTagVO WHERE uuid = 'a7ec68923efe447d9119ba7b6df2b54c';

ALTER TABLE `zstack`.`HostHaStateVO` ADD CONSTRAINT `HostHaStateVO_HostEO_uuid_fk` FOREIGN KEY (`uuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE ;