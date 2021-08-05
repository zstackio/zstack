CREATE TABLE  `zstack`.`EventLogVO` (
    `id`           BIGINT UNSIGNED NOT NULL UNIQUE AUTO_INCREMENT,
    `content`      text NOT NULL,
    `type`         varchar(32) NOT NULL,
    `category`     varchar(32) NOT NULL,
    `trackingId`   varchar(32) DEFAULT NULL,
    `resourceUuid` varchar(32) DEFAULT NULL,
    `resourceType` varchar(255) DEFAULT NULL,
    `time`         BIGINT UNSIGNED not NULL,
    `createDate`   timestamp,
    INDEX idxEventLogVOResourceUuid (resourceUuid),
    INDEX idxEventLogVOCategory (category),
    INDEX idxEventLogVOTime (time),
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
