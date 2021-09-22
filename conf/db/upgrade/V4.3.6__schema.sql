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
