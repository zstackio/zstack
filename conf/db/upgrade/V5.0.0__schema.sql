CREATE TABLE  `zstack`.`SNSWeComEndpointVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `url` varchar(1024) NOT NULL,
    `atAll` int(1) unsigned NOT NULL,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`SNSWeComAtPersonVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `userId` varchar(64) NOT NULL,
    `endpointUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`SNSFeiShuEndpointVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `url` varchar(1024) NOT NULL,
    `atAll` int(1) unsigned NOT NULL,
    `secret` varchar(128) default '' null,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`SNSFeiShuAtPersonVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `userId` varchar(64) NOT NULL,
    `endpointUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

alter table SNSDingTalkEndpointVO
    add secret varchar(128) default '' null;

alter table SNSDingTalkAtPersonVO
    add lastOpDate timestamp ON UPDATE CURRENT_TIMESTAMP;

alter table SNSDingTalkAtPersonVO
    add createDate timestamp NOT NULL DEFAULT '0000-00-00 00:00:00';