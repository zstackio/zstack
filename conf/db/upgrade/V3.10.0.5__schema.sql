CREATE TABLE IF NOT EXISTS `zstack`.`ReplayMessageVO` (
    `id`           BIGINT UNSIGNED NOT NULL UNIQUE AUTO_INCREMENT,
    `msgDump`      TEXT,
    `locationType` VARCHAR(256)    NOT NULL,
    `locationUuid` VARCHAR(32)     NOT NULL,
    `groupUuid`    VARCHAR(32),
    `resourceUuid` VARCHAR(32)     NOT NULL,
    `manageJobUuid`VARCHAR(32),
    `lastOpDate`   TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `createDate`   TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8;