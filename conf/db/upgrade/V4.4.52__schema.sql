DELIMITER $$
CREATE PROCEDURE CheckAndCreateResourceConfig()
    BEGIN
        DECLARE done INT DEFAULT FALSE;
        DECLARE resouce_uuid VARCHAR(32);
        DECLARE config_uuid varchar(32);
        DECLARE config_name varchar(255);
        DECLARE config_category varchar(64);
        DECLARE config_resource_type varchar(64);
        DECLARE config_value varchar(64);
        DECLARE config_description varchar(255);
        DECLARE cur CURSOR FOR SELECT uuid FROM zstack.IAM2ProjectVO;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
        SET config_name = 'iam2.force.enable.securityGroup';
        SET config_category = 'iam2';
        SET config_resource_type = 'IAM2ProjectVO';
        SET config_value = 'false';
        SET config_description = 'instances under the project need to bind the security group switch';

        OPEN cur;

        read_loop: LOOP
            FETCH cur INTO resouce_uuid;

            IF done THEN
                LEAVE read_loop;
            END IF;

            SELECT COUNT(*) INTO @count FROM ResourceConfigVO WHERE resourceUuid = resouce_uuid AND name = config_name;

            IF @count = 0 THEN
                SET config_uuid = REPLACE(UUID(),'-','');
                INSERT INTO ResourceConfigVO (uuid, name, description, category, value, resourceUuid, resourceType)
                VALUES (config_uuid, config_name, config_description, config_category, config_value, resouce_uuid, config_resource_type);
            END IF;
        END LOOP;

        CLOSE cur;
    END $$
DELIMITER ;
CALL CheckAndCreateResourceConfig();
DROP PROCEDURE IF EXISTS CheckAndCreateResourceConfig;
ALTER TABLE `zstack`.`HostNumaNodeVO` MODIFY COLUMN `nodeCPUs` TEXT NOT NULL;
ALTER TABLE `zstack`.`VmInstanceNumaNodeVO` MODIFY COLUMN `vNodeCPUs` TEXT NOT NULL;
