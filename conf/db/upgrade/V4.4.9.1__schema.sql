CREATE TABLE IF NOT EXISTS `zstack`.`SecurityLevelResourceRefVO` (
    `resourceUuid` VARCHAR(32) NOT NULL UNIQUE,
    `securityLevel` VARCHAR(12) NOT NULL,
    PRIMARY KEY (`resourceUuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `zstack`.`RoleVO` ADD COLUMN `rootUuid` VARCHAR(32) DEFAULT NULL;