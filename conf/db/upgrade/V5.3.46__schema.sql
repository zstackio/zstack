-- Migration script to change AccountId to ProjectId for AuditVO
-- Based on changeAccountIdToProjectIdForAduitVO function from NamespaceEventManagerImpl.java
-- Uses IAM2ProjectAccountRefVO to get mapping between AccountVO.uuid and IAM2ProjectVO.uuid

DELIMITER $$
DROP PROCEDURE IF EXISTS changeAccountIdToProjectIdForAuditVO$$
CREATE PROCEDURE changeAccountIdToProjectIdForAuditVO()
pro_label: BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_accountUuid VARCHAR(32);
    DECLARE v_projectUuid VARCHAR(32);
    DECLARE v_update_count1 INT DEFAULT 0;
    DECLARE v_update_count2 INT DEFAULT 0;
    DECLARE v_total_processed INT DEFAULT 0;

    -- Cursor to iterate through IAM2ProjectAccountRefVO records
    -- This table contains the mapping between accountUuid and projectUuid
    DECLARE project_cursor CURSOR FOR
        SELECT accountUuid, projectUuid
        FROM IAM2ProjectAccountRefVO;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    -- Check if there are any project account references to process
    IF (SELECT COUNT(*) FROM IAM2ProjectAccountRefVO) = 0 THEN
        SELECT 'No IAM2ProjectAccountRefVO records found, skipping migration.' AS message;
        LEAVE pro_label;
    END IF;

    SELECT 'Starting migration of AuditsVO records from AccountUuid to ProjectUuid...' AS message;
    SELECT CONCAT('Total account-project mappings to process: ', (SELECT COUNT(*) FROM IAM2ProjectAccountRefVO)) AS info;

    OPEN project_cursor;

    read_loop: LOOP
        FETCH project_cursor INTO v_accountUuid, v_projectUuid;

        IF done THEN
            LEAVE read_loop;
        END IF;

        -- First SQL operation: Update records where resourceType = 'AccountVO'
        -- Change resourceUuid from accountUuid to projectUuid
        -- Change resourceType from 'AccountVO' to 'IAM2ProjectVO'
        -- This corresponds to: SQL.New(AuditsVO.class).eq(AuditsVO_.resourceType, AccountVO.class.getSimpleName())
        --                     .set(AuditsVO_.resourceUuid, projectUuid).set(AuditsVO_.resourceType, IAM2ProjectVO.class.getSimpleName())
        UPDATE AuditsVO
        SET resourceUuid = v_projectUuid,
            resourceType = 'IAM2ProjectVO'
        WHERE apiName = 'org.zstack.header.identity.APIUpdateQuotaMsg'
          AND resourceUuid = v_accountUuid
          AND resourceType = 'AccountVO';

        SET v_update_count1 = ROW_COUNT();

        -- Second SQL operation: Update records where resourceType != 'AccountVO'
        -- Change only resourceUuid from accountUuid to projectUuid (keep original resourceType)
        -- This corresponds to: SQL.New(AuditsVO.class).notEq(AuditsVO_.resourceType, AccountVO.class.getSimpleName())
        --                     .set(AuditsVO_.resourceUuid, projectUuid)
        UPDATE AuditsVO
        SET resourceUuid = v_projectUuid
        WHERE apiName = 'org.zstack.header.identity.APIUpdateQuotaMsg'
          AND resourceUuid = v_accountUuid
          AND resourceType != 'AccountVO';

        SET v_update_count2 = ROW_COUNT();
        SET v_total_processed = v_total_processed + 1;

        -- Log progress for each account-project pair (optional, can be removed for performance)
        IF (v_update_count1 > 0 OR v_update_count2 > 0) THEN
            SELECT CONCAT('Processed mapping: Account[', v_accountUuid, '] -> Project[', v_projectUuid, ']',
                         ', AccountVO records updated: ', v_update_count1,
                         ', Other records updated: ', v_update_count2) AS progress;
        END IF;

    END LOOP;

    CLOSE project_cursor;

    SELECT CONCAT('Migration completed successfully. Total account-project pairs processed: ', v_total_processed) AS message;

END$$

DELIMITER ;
CALL changeAccountIdToProjectIdForAuditVO();
DROP PROCEDURE IF EXISTS changeAccountIdToProjectIdForAuditVO;
