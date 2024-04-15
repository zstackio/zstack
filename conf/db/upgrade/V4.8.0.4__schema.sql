CREATE TABLE `zstack`.`TemplateVmInstanceVO` (
    `uuid` char(32) NOT NULL UNIQUE,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `zstack`.`TemplateVmInstanceCacheVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `templateVmInstanceUuid` char(32) NOT NULL,
    `cacheVmInstanceUuid` char(32) NOT NULL,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;