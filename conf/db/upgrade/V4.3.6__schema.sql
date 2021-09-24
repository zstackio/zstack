ALTER TABLE `zstack`.`LicenseHistoryVO` ADD COLUMN `hash` char(32) DEFAULT 'unknown';
DROP INDEX idxLicenseHistoryVOUploadDate ON LicenseHistoryVO;
CREATE INDEX idxLicenseHistoryVOHash ON LicenseHistoryVO (hash);

DELETE FROM SystemTagVO WHERE tag LIKE 'bootOrder::%' AND resourceType = 'VmInstanceVO' AND uuid NOT IN (SELECT id FROM
  (SELECT min(uuid) AS id FROM SystemTagVO WHERE tag LIKE 'bootOrder::%' GROUP BY resourceUuid)
  AS table0);
DELETE FROM SystemTagVO WHERE tag LIKE 'bootOrderOnce::%' AND resourceType = 'VmInstanceVO' AND uuid NOT IN (SELECT id FROM
  (SELECT min(uuid) AS id FROM SystemTagVO WHERE tag LIKE 'bootOrderOnce::%' GROUP BY resourceUuid)
  AS table0);
UPDATE SystemTagVO SET inherent = 0 WHERE resourceType = 'VmInstanceVO' AND (tag LIKE 'bootOrder::%' OR tag LIKE 'bootOrderOnce::%');

CREATE TABLE IF NOT EXISTS `zstack`.`HostPhysicalMemoryVO` (
    `uuid` varchar(32) NOT NULL UNIQUE,
    `manufacturer` varchar(255) DEFAULT NULL,
    `size` varchar(32) DEFAULT NULL,
    `locator` varchar(255) DEFAULT NULL,
    `serialNumber` varchar(255) NOT NULL,
    `speed` varchar(32) DEFAULT NULL,
    `clockSpeed` varchar(32) DEFAULT NULL,
    `rank` varchar(32) DEFAULT NULL,
    `voltage` varchar(32) DEFAULT NULL,
    `hostUuid` varchar(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkHostPhysicalMemoryVOHostVO` FOREIGN KEY (`hostUuid`) REFERENCES `zstack`.`HostEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
