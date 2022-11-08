alter table LicenseHistoryVO modify COLUMN `userName` varchar(64) NOT NULL;

ALTER TABLE LicenseHistoryVO ADD COLUMN capacity int(10) NOT NULL;