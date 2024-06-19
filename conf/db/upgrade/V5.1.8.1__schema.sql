CREATE TABLE IF NOT EXISTS `zstack`.`ModelCenterVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(128) DEFAULT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `url` varchar(2048) DEFAULT NULL,
    `parameters` varchar(128) DEFAULT NULL,
    `managementIp` varchar(128) NOT NULL,
    `managementPort` int(16) not NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ModelVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) DEFAULT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `modelCenterUuid` varchar(32) NOT NULL,
    `parameters` mediumtext DEFAULT NULL,
    `installPath` varchar(2048) DEFAULT NULL,
    `introduction` mediumtext DEFAULT NULL,
    `logo` mediumtext DEFAULT NULL,
    `version` varchar(255) DEFAULT NULL,
    `vendor` varchar(255) DEFAULT NULL,
    `size` bigint(20) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`uuid`),
    CONSTRAINT fkModelVOModelCenterVO FOREIGN KEY (modelCenterUuid) REFERENCES ModelCenterVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ModelServiceVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `name` varchar(255) DEFAULT NULL,
    `description` varchar(2048) DEFAULT NULL,
    `yaml` mediumtext NOT NULL,
    `requestCpu` int(10) NOT NULL,
    `requestMemory` bigint(20) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ModelServiceRefVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `modelUuid` varchar(32) NOT NULL,
    `modelServiceUuid` varchar(32) NOT NULL,
    PRIMARY KEY  (`uuid`),
    CONSTRAINT fkModelRefVO FOREIGN KEY (modelUuid) REFERENCES ModelVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT fkModelServiceRefVO FOREIGN KEY (modelServiceUuid) REFERENCES ModelServiceVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE  `zstack`.`ModelServiceInstanceGroupVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `modelServiceUuid` varchar(32) NOT NULL,
    `status` varchar(255) NOT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    CONSTRAINT fkModelServiceInstanceGroupVOModelServiceModelServiceVO FOREIGN KEY (modelServiceUuid) REFERENCES ModelServiceVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE,
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `zstack`.`ModelServiceInstanceVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `description` varchar(2048) DEFAULT NULL,
    `yaml` mediumtext NOT NULL,
    `status` varchar(255) NOT NULL,
    `url` varchar(2048) NOT NULL,
    `modelServiceGroupUuid` varchar(32) NOT NULL,
    `vmInstanceUuid` varchar(32) DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY  (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--CREATE TABLE  `zstack`.`VmModelServiceInstanceVO` (
--    `uuid` varchar(32) NOT NULL UNIQUE,
--    `modelServiceInstanceGroupUuid` varchar(32) DEFAULT NULL,
--    PRIMARY KEY  (`uuid`)
--) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT INTO `TagPatternVO` VALUES
('3e88dfda748f4706a497143ff0c9d8bc','AI::Others','AI::Others','System Defined tag',"#9A3BE4",'simple','2024-06-18 09:52:59','2024-06-18 09:52:59'),
('402ce2d5dc614bd5a583254768369682','AI::Image-Generation','AI::Image-Generation','System Defined tag',"#DFFCC6",'simple','2024-06-18 09:51:45','2024-06-18 09:51:45'),
('730bbcd2e7a44d1687a56d518313c726','AI::Text-Generation','AI::Text-Generation','System Defined tag',"#CEEFFF",'simple','2024-06-18 09:51:05','2024-06-18 09:51:05'),
('7396ea323f9f474dace9b634bc634e76','AI::Text-to-Video','AI::Text-to-Video','System Defined tag',"#FFE4D8",'simple','2024-06-18 09:52:18','2024-06-18 09:52:18'),
('c093252e47794e428e634c084c45b19b','AI::Text-to-Audio','AI::Text-to-Audio','System Defined tag',"#F3FCB0",'simple','2024-06-18 09:52:40','2024-06-18 09:52:40');

INSERT INTO ResourceVO (`uuid`, `resourceName`, `resourceType`, `concreteResourceType`) VALUES
 ('3e88dfda748f4706a497143ff0c9d8bc', 'AI::Others', 'TagPatternVO', 'org.zstack.header.tag.TagPatternVO'),
 ('402ce2d5dc614bd5a583254768369682', 'AI::Image-Generation', 'TagPatternVO', 'org.zstack.header.tag.TagPatternVO'),
 ('730bbcd2e7a44d1687a56d518313c726', 'AI::Text-Generation', 'TagPatternVO', 'org.zstack.header.tag.TagPatternVO'),
 ('7396ea323f9f474dace9b634bc634e76', 'AI::Text-to-Video', 'TagPatternVO', 'org.zstack.header.tag.TagPatternVO'),
 ('c093252e47794e428e634c084c45b19b', 'AI::Text-to-Audio', 'TagPatternVO', 'org.zstack.header.tag.TagPatternVO');