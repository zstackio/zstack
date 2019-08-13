CREATE TABLE  `zstack`.`FileVerificationVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `path` varchar(256) NOT NULL,
    `node` varchar(32) NOT NULL,
    `type` varchar(32) NOT NULL,
    `digest` varchar(512) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8;