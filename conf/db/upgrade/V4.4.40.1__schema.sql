CREATE TABLE `AgentVersionVO` (
  `uuid` varchar(32) NOT NULL PRIMARY KEY,
  `agentType` varchar(255) DEFAULT NULL,
  `currentVersion` varchar(255) DEFAULT NULL,
  `expectVersion` varchar(255) DEFAULT NULL,
  `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
  `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00'
) ENGINE=InnoDB DEFAULT CHARSET=utf8;