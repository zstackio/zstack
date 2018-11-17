# create VpcRouterVmVO from ApplianceVmVO
DELIMITER $$
CREATE PROCEDURE generateVpcRouterVmVO()
    BEGIN
        DECLARE vrUuid varchar(32);
        DECLARE done INT DEFAULT FALSE;
        DECLARE cur CURSOR FOR SELECT uuid FROM zstack.ApplianceVmVO where applianceVmType = 'vpcvrouter_temp';
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
        OPEN cur;
        read_loop: LOOP
            FETCH cur INTO vrUuid;
            IF done THEN
                LEAVE read_loop;
            END IF;

            INSERT INTO zstack.VpcRouterVmVO (uuid) values (vrUuid);
            UPDATE zstack.ResourceVO set resourceType='VpcRouterVmVO' where uuid= vrUuid;

        END LOOP;
        CLOSE cur;
        # work around a bug of mysql : jira.mariadb.org/browse/MDEV-4602
        SELECT CURTIME();
    END $$
DELIMITER ;

CALL generateVpcRouterVmVO();
DROP PROCEDURE IF EXISTS generateVpcRouterVmVO;
update zstack.ApplianceVmVO set applianceVmType='vpcvrouter' where applianceVmType='vpcvrouter_temp';