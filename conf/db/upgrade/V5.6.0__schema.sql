-- UserVM guest bond state survives NIC removal and VM recycle-bin deletion.
CREATE TABLE IF NOT EXISTS `zstack`.`VmGuestBondVO` (
  `uuid` varchar(32) NOT NULL,
  `vmInstanceUuid` varchar(32) NOT NULL,
  `vmNicUuid` varchar(32) NOT NULL,
  `state` varchar(32) NOT NULL DEFAULT 'Checking',
  `reason` varchar(2048) DEFAULT NULL,
  `operationId` varchar(32) DEFAULT NULL,
  `target` varchar(8192) DEFAULT NULL,
  `createDate` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00',
  `lastOpDate` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`uuid`),
  KEY `idxVmGuestBondVm` (`vmInstanceUuid`),
  CONSTRAINT `fkVmGuestBondVmEO` FOREIGN KEY (`vmInstanceUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CALL ADD_COLUMN('VmGuestBondVO', 'diagnostics', 'longtext', 1, NULL);

CREATE TABLE IF NOT EXISTS `zstack`.`VmVfL2ReservationVO` (
  `vmUuid` varchar(32) NOT NULL,
  `operationId` varchar(32) NOT NULL,
  `hostUuid` varchar(32) NOT NULL,
  `sourceHostUuid` varchar(32) DEFAULT NULL,
  `networks` longtext NOT NULL,
  `lifecycleSettled` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`vmUuid`),
  KEY `idxVfL2Host` (`hostUuid`),
  KEY `idxVfL2SourceHost` (`sourceHostUuid`),
  CONSTRAINT `fkVfL2Vm` FOREIGN KEY (`vmUuid`) REFERENCES `VmInstanceEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CALL ADD_COLUMN('VmVfL2ReservationVO', 'operation', 'VARCHAR(32)', 1, NULL);
CALL ADD_COLUMN('VmVfL2ReservationVO', 'ownerNodeUuid', 'VARCHAR(32)', 1, NULL);
