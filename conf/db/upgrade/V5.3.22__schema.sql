CALL ADD_COLUMN('ModelServiceInstanceVO', 'clusterId', 'INT', 1, NULL);

CALL ADD_COLUMN('ModelServiceInstanceGroupVO', 'yaml', 'mediumtext', 1, NULL);

DROP PROCEDURE IF EXISTS update_instance_group_yaml;

DELIMITER $$

CREATE PROCEDURE update_instance_group_yaml()
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE group_uuid VARCHAR(255);
    DECLARE group_yaml TEXT;
    DECLARE instance_yaml TEXT;

    DECLARE group_cursor CURSOR FOR
        SELECT uuid, yaml FROM ModelServiceInstanceGroupVO;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

    OPEN group_cursor;

    group_loop: LOOP
        FETCH group_cursor INTO group_uuid, group_yaml;

        IF done THEN
            LEAVE group_loop;
        END IF;

        IF group_yaml IS NULL OR group_yaml = '' THEN
            SELECT yaml INTO instance_yaml
            FROM ModelServiceInstanceVO
            WHERE modelServiceGroupUuid = group_uuid
            LIMIT 1;

            IF instance_yaml IS NOT NULL AND instance_yaml != '' THEN
                UPDATE ModelServiceInstanceGroupVO
                SET yaml = instance_yaml
                WHERE uuid = group_uuid;

                SELECT CONCAT('updated group_uuid: ', group_uuid, ' yaml');
            END IF;
        ELSE
            SELECT CONCAT('group_uuid: ', group_uuid, ' yaml is not null, skip');
        END IF;
    END LOOP;

    CLOSE group_cursor;

    SELECT 'update_instance_group_yaml done';
END$$

DELIMITER ;

CALL update_instance_group_yaml();