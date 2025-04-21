CALL INSERT_COLUMN('AlarmRecordsVO', 'operatorAccountUuid', 'char(32)', 1, NULL, 'readStatus');
CALL INSERT_COLUMN('EventRecordsVO', 'operatorAccountUuid', 'char(32)', 1, NULL, 'readStatus');

UPDATE `zstack`.`ResourceVO` set `resourceType` = 'ThirdPartyAccountSourceVO' where uuid in (select uuid from ThirdPartyAccountSourceVO);

UPDATE `zstack`.`AlarmVO` SET `name` = 'Data Storage Available Capacity' WHERE `name` = 'Primary Storage Available Capacity';
UPDATE `zstack`.`AlarmVO` SET `name` = 'Data Storage Available Physical Capacity' WHERE `name` = 'Primary Storage Available Physical Capacity';

DELETE FROM `zstack`.`ResourceConfigVO` WHERE `category`='sharedblock' AND `name`='qcow2.allocation';

DELETE FROM `zstack`.`SystemTagVO` WHERE `tag`='vmNumaEnable::false' AND `resourceType`='VmInstanceVO';

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
