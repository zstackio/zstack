DROP PROCEDURE IF EXISTS `zstack`.`repairFileVerificationRecordsIndex`;

DELIMITER $$
CREATE PROCEDURE `zstack`.`repairFileVerificationRecordsIndex`()
BEGIN
    DECLARE oldIndexName varchar(64);
    DECLARE replacementIndexName varchar(64) DEFAULT 'idxFileVerificationRecordsVOFileVerificationUuid';
    DECLARE replacementSuffix int DEFAULT 0;

    repairIndexes: LOOP
        -- The original table definition made each registration's audit history unique.
        -- Only remove that full-column, single-column constraint, regardless of its name.
        SELECT MIN(legacyIndex.INDEX_NAME) INTO oldIndexName
        FROM (
            SELECT INDEX_NAME
            FROM information_schema.statistics
            WHERE TABLE_SCHEMA = 'zstack'
              AND TABLE_NAME = 'FileVerificationRecordsVO'
              AND INDEX_NAME <> 'PRIMARY'
              AND NON_UNIQUE = 0
            GROUP BY INDEX_NAME
            HAVING COUNT(*) = 1
               AND MAX(COLUMN_NAME) = 'fileVerificationUuid'
               AND MAX(SUB_PART) IS NULL
        ) AS legacyIndex;

        IF oldIndexName IS NULL THEN
            LEAVE repairIndexes;
        END IF;

        -- Keep an index suitable for the existing foreign key before dropping UNIQUE.
        -- Reuse indexes from fresh installations or an earlier field repair.
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.statistics
            WHERE TABLE_SCHEMA = 'zstack'
              AND TABLE_NAME = 'FileVerificationRecordsVO'
              AND NON_UNIQUE = 1
              AND SEQ_IN_INDEX = 1
              AND COLUMN_NAME = 'fileVerificationUuid'
              AND SUB_PART IS NULL
              AND INDEX_TYPE = 'BTREE'
        ) THEN
            WHILE EXISTS (
                SELECT 1 FROM information_schema.statistics
                WHERE TABLE_SCHEMA = 'zstack'
                  AND TABLE_NAME = 'FileVerificationRecordsVO'
                  AND INDEX_NAME = replacementIndexName
            ) DO
                SET replacementSuffix = replacementSuffix + 1;
                SET replacementIndexName = CONCAT('idxFileVerificationRecordsVOFileVerificationUuid_', replacementSuffix);
            END WHILE;

            SET @fileVerificationIndexSql = CONCAT(
                'ALTER TABLE `zstack`.`FileVerificationRecordsVO` ADD INDEX `',
                replacementIndexName, '` (`fileVerificationUuid`)');
            PREPARE fileVerificationIndexStatement FROM @fileVerificationIndexSql;
            EXECUTE fileVerificationIndexStatement;
            DEALLOCATE PREPARE fileVerificationIndexStatement;
        END IF;

        SET @fileVerificationIndexSql = CONCAT(
            'ALTER TABLE `zstack`.`FileVerificationRecordsVO` DROP INDEX `',
            REPLACE(oldIndexName, '`', '``'), '`');
        PREPARE fileVerificationIndexStatement FROM @fileVerificationIndexSql;
        EXECUTE fileVerificationIndexStatement;
        DEALLOCATE PREPARE fileVerificationIndexStatement;
    END LOOP;

    SET @fileVerificationIndexSql = NULL;
END $$
DELIMITER ;

CALL `zstack`.`repairFileVerificationRecordsIndex`();
DROP PROCEDURE IF EXISTS `zstack`.`repairFileVerificationRecordsIndex`;
