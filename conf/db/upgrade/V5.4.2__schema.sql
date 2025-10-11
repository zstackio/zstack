CALL ADD_COLUMN('ModelVO', 'versionSemver', 'varchar(255)', 1, 'NULL');
CALL ADD_COLUMN('ModelVO', 'isLatestVersion', 'tinyint(1)', 1, '0');
CALL ADD_COLUMN('ModelVO', 'artifactChecksum', 'varchar(255)', 1, 'NULL');
CALL ADD_COLUMN('ModelVO', 'artifactSizeBytes', 'bigint', 1, '0');
CALL ADD_COLUMN('ModelVO', 'architectureType', 'varchar(255)', 1, 'NULL');
CALL ADD_COLUMN('ModelVO', 'frameworkVersion', 'varchar(255)', 1, 'NULL');
CALL ADD_COLUMN('ModelVO', 'requiredAccelerator', 'varchar(255)', 1, 'NULL');
