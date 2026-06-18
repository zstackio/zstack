package org.zstack.test.unittest.core

import org.junit.Test

class DatabaseUpgradeSqlCase {
    private static String readRepoFile(String path) {
        File dir = new File(System.getProperty("user.dir")).canonicalFile
        while (dir != null) {
            File file = new File(dir, path)
            if (file.exists()) {
                return file.text
            }
            dir = dir.parentFile
        }

        throw new FileNotFoundException(path)
    }

    @Test
    void testLicenseHistoryNullHashCleanupRunsBeforeVersionedMigrations() {
        String beforeMigrate = readRepoFile("conf/db/upgrade/beforeMigrate.sql")

        assert beforeMigrate.contains("CREATE PROCEDURE `fixLicenseHistoryNullHash`()")
        assert beforeMigrate.contains("FROM INFORMATION_SCHEMA.COLUMNS")
        assert beforeMigrate.contains("table_name = 'LicenseHistoryVO'")
        assert beforeMigrate.contains("column_name = 'hash'")
        assert beforeMigrate.contains("UPDATE `zstack`.`LicenseHistoryVO`")
        assert beforeMigrate.contains("SET `hash` = 'unknown'")
        assert beforeMigrate.contains("WHERE `hash` IS NULL")
        assert beforeMigrate.indexOf("CALL `fixLicenseHistoryNullHash`()") >
                beforeMigrate.indexOf("CREATE PROCEDURE `fixLicenseHistoryNullHash`()")
    }

    @Test
    void testLicenseHistoryNullHashFixDoesNotModifyHistoricalMigration() {
        String v440 = readRepoFile("conf/db/upgrade/V4.4.0__schema.sql")
        String beforeValidate = readRepoFile("conf/db/upgrade/beforeValidate.sql")

        assert !v440.contains("SET `hash` = 'unknown'")
        assert !v440.contains("WHERE `hash` IS NULL")
        assert !beforeValidate.contains("V4.4.0__schema.sql")
    }
}
