use zstack;

DELIMITER $$

DROP FUNCTION IF EXISTS `Json_getKeyValue` $$

CREATE FUNCTION `Json_getKeyValue`(
    in_JsonArray VARCHAR(4096),
    in_KeyName VARCHAR(64)
) RETURNS VARCHAR(4096) CHARSET utf8

BEGIN
    DECLARE vs_return, vs_JsonArray, vs_JsonString, vs_Json, vs_KeyName VARCHAR(4096);
    DECLARE vi_pos1, vi_pos2 SMALLINT UNSIGNED;

    SET vs_JsonArray = TRIM(in_JsonArray);
    SET vs_KeyName = TRIM(in_KeyName);

    IF vs_JsonArray = '' OR vs_JsonArray IS NULL
        OR vs_KeyName = '' OR vs_KeyName IS NULL
    THEN
        SET vs_return = NULL;
    ELSE
        SET vs_JsonArray = REPLACE(REPLACE(vs_JsonArray, '[', ''), ']', '');
        SET vs_JsonString = CONCAT("'", vs_JsonArray, "'");
        SET vs_json = SUBSTRING_INDEX(SUBSTRING_INDEX(vs_JsonString,'}',1), '{', -1);

        IF vs_json = '' OR vs_json IS NULL THEN
            SET vs_return = NULL;
        ELSE
            SET vs_KeyName = CONCAT('"', vs_KeyName, '":');
            SET vi_pos1 = INSTR(vs_json, vs_KeyName);

            IF vi_pos1 > 0 THEN
                SET vi_pos1 = vi_pos1 + CHAR_LENGTH(vs_KeyName);
                SET vi_pos2 = LOCATE('","', vs_json, vi_pos1);

                IF vi_pos2 = 0 THEN
                    SET vi_pos2 = CHAR_LENGTH(vs_json) + 1;
                END IF;

            SET vs_return = REPLACE(MID(vs_json, vi_pos1, vi_pos2 - vi_pos1), '"', '');
            END IF;
        END IF;
    END IF;
    RETURN(vs_return);
END$$
DELIMITER  ;

SET FOREIGN_KEY_CHECKS = 0;

DROP PROCEDURE IF EXISTS cleanupUsedIpVO;
DELIMITER $$
CREATE PROCEDURE cleanupUsedIpVO()
BEGIN
    DECLARE curUsedIpUuid VARCHAR(32);
    DECLARE vipCount INT DEFAULT 0;
    DECLARE vmNicCount INT DEFAULT 0;
    DECLARE dhcpCount INT DEFAULT 0;
    DECLARE done INT DEFAULT FALSE;
    DECLARE cur CURSOR FOR SELECT uuid FROM `zstack`.`UsedIpVO` usedIp;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO curUsedIpUuid;
        IF done THEN
            LEAVE read_loop;
        END IF;

        SELECT COUNT(*) INTO vipCount FROM VipVO WHERE usedIpUuid = curUsedIpUuid;
        IF (vipCount > 0) THEN
            ITERATE read_loop;
        END IF;

        SELECT COUNT(*) INTO vmNicCount FROM VmNicVO WHERE usedIpUuid = curUsedIpUuid;
        IF (vmNicCount > 0) THEN
            ITERATE read_loop;
        END IF;

        SELECT COUNT(*) INTO dhcpCount FROM SystemTagVO WHERE resourceType='L3NetworkVO'
         AND tag LIKE CONCAT('flatNetwork::DhcpServer::%::ipUuid::', curUsedIpUuid,  '%');
        IF (dhcpCount > 0) THEN
            ITERATE read_loop;
        END IF;

        DELETE FROM UsedIpVO WHERE uuid = curUsedIpUuid;
    END LOOP;
    CLOSE cur;
    SELECT CURTIME();
END $$
DELIMITER ;

DROP PROCEDURE IF EXISTS `ADD_COLUMN`;

DELIMITER $$
CREATE PROCEDURE `ADD_COLUMN`(
    IN tb_name VARCHAR(64),
    IN col_name VARCHAR(64),
    IN col_data_type VARCHAR(64),
    IN allow_null BOOL,
    IN default_value VARCHAR(255)
)
BEGIN
	DECLARE alter_sql VARCHAR(1000);
    IF NOT EXISTS( SELECT 1
                           FROM INFORMATION_SCHEMA.COLUMNS
                           WHERE table_name = tb_name
                                AND table_schema = 'zstack'
                                AND column_name = col_name) THEN
                SET @alter_sql = CONCAT('ALTER TABLE zstack.', tb_name, ' ADD COLUMN ', col_name, ' ', col_data_type);
                IF NOT allow_null THEN
                    SET @alter_sql = CONCAT(@alter_sql, ' NOT NULL');
                END IF;
                IF default_value IS NOT NULL THEN
        				  SET @alter_sql = CONCAT(@alter_sql, ' DEFAULT ''', default_value, '''');
                ELSE
        				  SET @alter_sql = CONCAT(@alter_sql, ' DEFAULT NULL');
                END IF;
                SELECT @alter_sql;
                PREPARE stmt FROM @alter_sql;
                EXECUTE stmt;
                DEALLOCATE PREPARE stmt;
    END IF;

SELECT CURTIME();
END$$
DELIMITER ;

DROP PROCEDURE IF EXISTS `ADD_CONSTRAINT`;

DELIMITER $$
CREATE PROCEDURE `ADD_CONSTRAINT`(
    IN tb_name VARCHAR(64),
    IN cons_name VARCHAR(255),
    IN references_col VARCHAR(64),
    IN referencing_table VARCHAR(64),
    IN referencing_col VARCHAR(64),
    IN on_delete VARCHAR(64)
)
BEGIN
	DECLARE alter_sql VARCHAR(1000);
    IF NOT EXISTS( SELECT 1
                           FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
                           WHERE table_name = tb_name
                                AND table_schema = 'zstack'
                                AND CONSTRAINT_NAME = cons_name) THEN
                SET @alter_sql = CONCAT('ALTER TABLE zstack.', tb_name, ' ADD CONSTRAINT ', cons_name,
                        ' FOREIGN KEY (', references_col, ') REFERENCES ', 'zstack.', referencing_table, ' (', referencing_col,
                        ') ON DELETE ', on_delete);
                SELECT @alter_sql;
                PREPARE stmt FROM @alter_sql;
                EXECUTE stmt;
                DEALLOCATE PREPARE stmt;
    END IF;

SELECT CURTIME();
END$$
DELIMITER ;