DROP PROCEDURE IF EXISTS addResourceConfigForVirtualRouter;
DELIMITER $$
CREATE PROCEDURE addResourceConfigForVirtualRouter()
BEGIN
    DECLARE virtualRouterUuid VARCHAR(32);
    DECLARE ruuid VARCHAR(32);
    DECLARE done INT DEFAULT FALSE;
    DECLARE cur CURSOR FOR SELECT uuid FROM `zstack`.`VirtualRouterVmVO`;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    OPEN cur;
    read_loop: LOOP
        FETCH cur INTO virtualRouterUuid;
        IF done THEN
            LEAVE read_loop;
        END IF;

        SET ruuid = REPLACE(UUID(), '-', '');
        INSERT INTO zstack.ResourceConfigVO (uuid, name, description, category, value, resourceUuid, resourceType, lastOpDate, createDate)
        values(ruuid, "vm.ha.strategy", "vm.ha.strategy", "ha", "Force", virtualRouterUuid, "VmInstanceVO", CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());

    END LOOP;
    CLOSE cur;

    # work around a bug of mysql : jira.mariadb.org/browse/MDEV-4602
    SELECT CURTIME();
END $$
DELIMITER ;

CALL addResourceConfigForVirtualRouter();
DROP PROCEDURE IF EXISTS addResourceConfigForVirtualRouter;