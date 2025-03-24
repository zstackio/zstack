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

DROP TABLE IF EXISTS HostHaStateVO;
CREATE TABLE `HostHaStateVO` (
    `id` BIGINT UNSIGNED NOT NULL UNIQUE AUTO_INCREMENT,
    `hostUuid` VARCHAR(32) NOT NULL,
    `primaryStorageUuid` VARCHAR(32) NOT NULL,
    `state` varchar(32),
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`id`),
    CONSTRAINT `HostHaStateVO_HostEO_uuid_fk` FOREIGN KEY (`hostUuid`) REFERENCES `HostEO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `HostHaStateVO_PrimaryStorageEO_uuid_fk` FOREIGN KEY (`primaryStorageUuid`) REFERENCES `PrimaryStorageEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
