package org.zstack.test.integration.db.schema

import groovy.sql.Sql
import org.zstack.core.db.DatabaseFacade
import org.zstack.testlib.SubCase

import java.sql.SQLException

class FileVerificationRecordsUpgradeCase extends SubCase {
    Sql sql
    String recordsName
    String registrationName
    String procedureName
    String replacementIndexName
    String recordsTable
    String registrationTable
    boolean recordsCreated
    boolean registrationCreated
    List<String> migrationStatements

    @Override
    void setup() {
    }

    @Override
    void environment() {
    }

    @Override
    void test() {
        File migration = new File("../conf/db/upgrade/V4.4.52.6__schema.sql")
        assert migration.isFile(): "upgrade SQL is missing: ${migration.absolutePath}"
        String suffix = UUID.randomUUID().toString().substring(0, 8)
        recordsName = "fvUpgrade${suffix}Records"
        registrationName = "fvUpgrade${suffix}Parent"
        procedureName = "fvUpgrade${suffix}Repair"
        replacementIndexName = "idx${recordsName}FileVerificationUuid"
        recordsTable = "`zstack`.`${recordsName}`"
        registrationTable = "`zstack`.`${registrationName}`"
        String source = migration.getText("UTF-8")
                .replace("FileVerificationRecordsVO", recordsName)
                .replace("repairFileVerificationRecordsIndex", procedureName)
        migrationStatements = parseStatements(source)
        sql = new Sql(bean(DatabaseFacade.class).dataSource.connection)
        boolean ownsNames = false
        try {
            // Run only against the native UT database; production tables are never altered.
            assert sql.connection.catalog == "zstack"
            assert sql.firstRow("""SELECT COUNT(*) AS total FROM information_schema.TABLES
                WHERE TABLE_SCHEMA = 'zstack' AND TABLE_NAME IN (?, ?)""", [recordsName, registrationName]).total == 0
            assert sql.firstRow("""SELECT COUNT(*) AS total FROM information_schema.ROUTINES
                WHERE ROUTINE_SCHEMA = 'zstack' AND ROUTINE_NAME = ?""", [procedureName]).total == 0
            ownsNames = true
            [
                    [indexes: "UNIQUE KEY `fileVerificationUuid` (`fileVerificationUuid`)", retained: []],
                    [indexes: "UNIQUE KEY `legacyRegistration` (`fileVerificationUuid`), " +
                            "KEY `existingRegistrationIndex` (`fileVerificationUuid`)",
                     retained: ["existingRegistrationIndex"]],
                    [indexes: "KEY `fileVerificationUuid` (`fileVerificationUuid`)",
                     retained: ["fileVerificationUuid"]],
                    [indexes: "UNIQUE KEY `legacyRegistration` (`fileVerificationUuid`), " +
                            "KEY `${replacementIndexName}` (`id`), " +
                            "UNIQUE KEY `preserveComposite` (`fileVerificationUuid`, `reason`)",
                     retained: [replacementIndexName, "preserveComposite"]]
            ].each { scenario ->
                testUpgrade(scenario.indexes as String, scenario.retained as List<String>)
            }
        } finally {
            try {
                if (ownsNames) {
                    try {
                        execute("DROP PROCEDURE IF EXISTS `zstack`.`${procedureName}`")
                    } finally {
                        dropFixtureTables()
                    }
                }
            } finally {
                sql.close()
            }
        }
    }

    void testUpgrade(String indexes, List<String> retainedIndexes) {
        dropFixtureTables()
        execute("CREATE TABLE ${registrationTable} (`uuid` varchar(32) NOT NULL PRIMARY KEY) ENGINE=InnoDB")
        registrationCreated = true
        execute("""CREATE TABLE ${recordsTable} (
            `id` bigint unsigned NOT NULL AUTO_INCREMENT,
            `fileVerificationUuid` varchar(32) NOT NULL,
            `reason` varchar(64) NOT NULL,
            `recoverFlag` tinyint(1) NOT NULL,
            PRIMARY KEY (`id`),
            ${indexes},
            CONSTRAINT `${procedureName}FK` FOREIGN KEY (`fileVerificationUuid`)
                REFERENCES ${registrationTable} (`uuid`) ON DELETE CASCADE
        ) ENGINE=InnoDB""")
        recordsCreated = true
        String uuid = "0123456789abcdef0123456789abcdef"
        execute("INSERT INTO ${registrationTable} (`uuid`) VALUES ('${uuid}')")
        execute("INSERT INTO ${recordsTable} (`fileVerificationUuid`, `reason`, `recoverFlag`) " +
                "VALUES ('${uuid}', 'original', 1)")

        migrationStatements.each { execute(it) }
        assertIndexes(retainedIndexes)
        def original = sql.firstRow("SELECT * FROM ${recordsTable}".toString())
        assert original.id == 1
        assert original.fileVerificationUuid == uuid
        assert original.reason == "original"
        assert original.recoverFlag
        execute("INSERT INTO ${recordsTable} (`fileVerificationUuid`, `reason`, `recoverFlag`) " +
                "VALUES ('${uuid}', 'second', 0)")

        // Re-running the actual upgrade must preserve both history rows and all other constraints.
        migrationStatements.each { execute(it) }
        assertIndexes(retainedIndexes)
        execute("INSERT INTO ${recordsTable} (`fileVerificationUuid`, `reason`, `recoverFlag`) " +
                "VALUES ('${uuid}', 'third', 1)")
        def history = sql.rows("SELECT * FROM ${recordsTable} ORDER BY id".toString())
        assert history*.id == [1, 2, 3]
        assert history*.fileVerificationUuid == [uuid, uuid, uuid]
        assert history*.reason == ["original", "second", "third"]
        assert history[0].recoverFlag && !history[1].recoverFlag && history[2].recoverFlag

        def foreignKeys = sql.rows("""SELECT REFERENCED_TABLE_NAME, DELETE_RULE
            FROM information_schema.REFERENTIAL_CONSTRAINTS
            WHERE CONSTRAINT_SCHEMA = 'zstack' AND TABLE_NAME = ?""", [recordsName])
        assert foreignKeys.size() == 1
        assert foreignKeys[0].REFERENCED_TABLE_NAME == registrationName
        assert foreignKeys[0].DELETE_RULE == "CASCADE"
        boolean orphanRejected = false
        try {
            execute("INSERT INTO ${recordsTable} (`fileVerificationUuid`, `reason`, `recoverFlag`) " +
                    "VALUES ('ffffffffffffffffffffffffffffffff', 'orphan', 0)")
        } catch (SQLException e) {
            assert e.SQLState == "23000" && e.errorCode == 1452
            orphanRejected = true
        }
        assert orphanRejected
        execute("DELETE FROM ${registrationTable} WHERE uuid = '${uuid}'")
        assert sql.firstRow("SELECT COUNT(*) AS total FROM ${recordsTable}".toString()).total == 0
    }

    void assertIndexes(List<String> retainedIndexes) {
        def indexes = sql.rows("""SELECT INDEX_NAME, NON_UNIQUE, COLUMN_NAME, SEQ_IN_INDEX, SUB_PART
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = 'zstack' AND TABLE_NAME = ?""", [recordsName])
        assert indexes.find { it.INDEX_NAME == "PRIMARY" && it.COLUMN_NAME == "id" && it.NON_UNIQUE == 0 }
        assert indexes.find {
            it.NON_UNIQUE == 1 && it.SEQ_IN_INDEX == 1 &&
                    it.COLUMN_NAME == "fileVerificationUuid" && it.SUB_PART == null
        }
        assert !indexes.groupBy { it.INDEX_NAME }.values().any { columns ->
            columns.size() == 1 && columns[0].NON_UNIQUE == 0 &&
                    columns[0].COLUMN_NAME == "fileVerificationUuid" && columns[0].SUB_PART == null
        }
        assert retainedIndexes.every { name -> indexes.any { it.INDEX_NAME == name } }
        if (retainedIndexes.contains("preserveComposite")) {
            def composite = indexes.findAll { it.INDEX_NAME == "preserveComposite" }.sort { it.SEQ_IN_INDEX }
            assert composite*.COLUMN_NAME == ["fileVerificationUuid", "reason"]
            assert composite.every { it.NON_UNIQUE == 0 }
        }
    }

    void execute(String statement) {
        sql.execute(statement)
    }

    void dropFixtureTables() {
        try {
            if (recordsCreated) {
                execute("DROP TABLE ${recordsTable}")
                recordsCreated = false
            }
        } finally {
            if (registrationCreated) {
                execute("DROP TABLE ${registrationTable}")
                registrationCreated = false
            }
        }
    }

    // JDBC executes CREATE PROCEDURE as one statement; DELIMITER is a mysql client directive.
    static List<String> parseStatements(String source) {
        String delimiter = ";"
        StringBuilder pending = new StringBuilder()
        List<String> statements = []
        source.eachLine { line ->
            String trimmed = line.trim()
            if (trimmed.startsWith("DELIMITER ")) {
                assert pending.toString().trim().empty
                delimiter = trimmed.substring("DELIMITER ".length()).trim()
            } else if (!trimmed.empty || pending.length() > 0) {
                pending.append(line).append('\n')
                if (trimmed.endsWith(delimiter)) {
                    String statement = pending.toString().trim()
                    statements.add(statement.substring(0, statement.length() - delimiter.length()).trim())
                    pending.setLength(0)
                }
            }
        }
        assert pending.toString().trim().empty: "unterminated upgrade SQL statement"
        assert !statements.empty
        return statements
    }

    @Override
    void clean() {
    }
}
