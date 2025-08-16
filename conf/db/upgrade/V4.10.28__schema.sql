-- Feature: Progress Refactor | ZSV-9610

CREATE TABLE `zstack`.`ActionProgressVO` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT,
    `apiId` char(32) NOT NULL,
    `content` varchar(255) DEFAULT NULL,
    `opaque` text,
    `createTime` bigint unsigned NOT NULL,
    `lastOpTime` bigint unsigned NOT NULL,
    `currentStep` bigint unsigned DEFAULT 0,
    `totalStep` bigint unsigned DEFAULT 1,
    PRIMARY KEY (`id`),
    KEY `idxActionProgressVOApiId` (`apiId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Others
