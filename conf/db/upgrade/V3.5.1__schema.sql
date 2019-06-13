CREATE TABLE `EventSubscriptionDataVO` (
  `uuid` varchar(32) NOT NULL,
  `createDateInLong` bigint(20) unsigned NOT NULL,
  `accountUuid` varchar(32) NOT NULL,
  `readStatus` varchar(32) NOT NULL,
  PRIMARY KEY (`uuid`),
  KEY `accountUuid` (`accountUuid`),
  KEY `createDateInLong` (`createDateInLong`),
  KEY `idxAccountUuidReadStatus` (`accountUuid`,`readStatus`,`createDateInLong`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `AlarmDataVO` (
  `uuid` varchar(32) NOT NULL,
  `createDateInLong` bigint(20) unsigned NOT NULL,
  `accountUuid` varchar(32) NOT NULL,
  `readStatus` varchar(32) NOT NULL,
  PRIMARY KEY (`uuid`),
  KEY `accountUuid` (`accountUuid`),
  KEY `createDateInLong` (`createDateInLong`),
  KEY `idxAccountUuidReadStatus` (`accountUuid`,`readStatus`,`createDateInLong`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;