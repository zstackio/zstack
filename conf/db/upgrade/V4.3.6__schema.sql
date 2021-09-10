ALTER TABLE `zstack`.`LicenseHistoryVO` ADD COLUMN `hash` char(32) DEFAULT 'unknown';
DROP INDEX idxLicenseHistoryVOUploadDate ON LicenseHistoryVO;
CREATE INDEX idxLicenseHistoryVOHash ON LicenseHistoryVO (hash);