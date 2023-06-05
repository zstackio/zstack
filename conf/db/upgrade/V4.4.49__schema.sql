DELIMITER $$
CREATE PROCEDURE UpdateHygonClusterVmCpuModeConfig()
    BEGIN
        DECLARE done INT DEFAULT FALSE;
        DECLARE tag_uuid VARCHAR(32);
        DECLARE resource_uuid VARCHAR(32);
        DECLARE resource_type varchar(64);
        DECLARE config_name varchar(255);
        DECLARE config_description varchar(255);
        DECLARE config_category varchar(64);
        DECLARE config_value varchar(64);
        DECLARE cur CURSOR FOR SELECT uuid FROM zstack.ClusterVO;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
        SET config_name = 'vm.cpuMode';
        SET config_description = 'the default configuration after the management node is upgraded (only for hygon cluster)';
        SET config_category = 'kvm';
        SET resource_type = 'ClusterVO';
        SET config_value = 'Hygon_Customized';
        SET tag_uuid = REPLACE(UUID(), '-', '');

        OPEN cur;

        read_loop: LOOP
            FETCH cur INTO resource_uuid;

            IF done THEN
                LEAVE read_loop;
            END IF;

            SELECT COUNT(DISTINCT tag) INTO @hygon_tag_count FROM SystemTagVO WHERE tag LIKE 'hostCpuModelName::Hygon%' AND resourceUuid IN (SELECT uuid FROM HostVO WHERE clusterUuid = resource_uuid);

            SELECT COUNT(*) INTO @config_exist FROM ResourceConfigVO WHERE name = config_name AND category = config_category AND resourceUuid = resource_uuid AND resourceType = resource_type;

            IF @hygon_tag_count = 1 THEN
                IF @config_exist = 1 THEN
                    UPDATE ResourceConfigVO SET value = config_value WHERE name = config_name AND category = config_category AND resourceUuid = resource_uuid AND resourceType = resource_type;
                ELSE
                    INSERT INTO ResourceConfigVO (uuid, name, description, category, value, resourceUuid, resourceType, lastOpDate, createDate)
                            VALUES (tag_uuid, config_name, config_description, config_category, config_value, resource_uuid, resource_type, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());
                END IF;
            END IF;
        END LOOP;

        CLOSE cur;
    END $$
DELIMITER ;
CALL UpdateHygonClusterVmCpuModeConfig();
DROP PROCEDURE IF EXISTS UpdateHygonClusterVmCpuModeConfig;
