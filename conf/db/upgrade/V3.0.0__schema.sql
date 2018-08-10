ALTER TABLE `CaptchaVO` DROP COLUMN `attempts`;

CREATE TABLE `LoginAttemptsVO` (
    `uuid` VARCHAR(32) NOT NULL,
    `targetResourceIdentity` VARCHAR(256) NOT NULL,
    `attempts` int(10) unsigned DEFAULT 0,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    UNIQUE KEY `uuid` (`uuid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `zstack`.`IAM2TicketFlowCollectionVO` (
  `uuid` varchar(32) NOT NULL,
  `projectUuid` varchar(32) NOT NULL,
  PRIMARY KEY  (`uuid`),
	CONSTRAINT fkIAM2TicketFlowCollectionVOTicketFlowCollectionVO FOREIGN KEY (uuid) REFERENCES TicketFlowCollectionVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE,
	CONSTRAINT fkIAM2TicketFlowCollectionVOIAM2ProjectVO FOREIGN KEY (projectUuid) REFERENCES IAM2ProjectVO (uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

ALTER TABLE `TicketFlowCollectionVO` ADD COLUMN `state` varchar(64) NOT NULL;
ALTER TABLE `TicketFlowCollectionVO` ADD COLUMN `status` varchar(64) NOT NULL;
ALTER TABLE `TicketFlowCollectionVO` ADD COLUMN `type` varchar(64) NOT NULL;

CREATE TABLE `zstack`.`IAM2TicketFlowVO` (
  `uuid` varchar(32) NOT NULL,
  `approverUuid` varchar(32) NOT NULL,
  `valid` tinyint(1) unsigned NOT NULL,
  PRIMARY KEY  (`uuid`),
	CONSTRAINT fkIAM2TicketFlowVOTicketFlowVO FOREIGN KEY (uuid) REFERENCES TicketFlowVO (uuid) ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
