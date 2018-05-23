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

