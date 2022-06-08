DELIMITER $$
CREATE PROCEDURE Update_Vip_Account()
    BEGIN
        DECLARE vipUuid VARCHAR(32);
        DECLARE eipAccountUuid VARCHAR(32);
        DECLARE vipAccountUuid VARCHAR(32);
        DECLARE done INT DEFAULT FALSE;
        DECLARE cur CURSOR FOR SELECT v.uuid, a.accountUuid, b.accountUuid FROM zstack.EipVO e, zstack.VipVO v, zstack.AccountResourceRefVO a, zstack.AccountResourceRefVO b
                                        WHERE e.vipUuid = v.uuid AND a.resourceUuid = e.uuid AND b.resourceUuid = v.uuid AND a.accountUuid != b.accountUuid;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
        OPEN cur;
        read_loop: LOOP
            FETCH cur INTO vipUuid, eipAccountUuid, vipAccountUuid;
            IF done THEN
                LEAVE read_loop;
            END IF;
            UPDATE zstack.AccountResourceRefVO set accountUuid = eipAccountUuid WHERE accountUuid = vipAccountUuid AND resourceUuid = vipUuid;
        END LOOP;
        CLOSE cur;
    END $$
DELIMITER ;

CALL Update_Vip_Account();
DROP PROCEDURE IF EXISTS Update_Vip_Account;
