DROP PROCEDURE IF EXISTS `Alter_SCSI_Table`;
DELIMITER $$
CREATE PROCEDURE Alter_SCSI_Table()
BEGIN
    IF NOT EXISTS( SELECT NULL
                   FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE table_name = 'ScsiLunHostRefVO'
                     AND table_schema = 'zstack'
                     AND column_name = 'path')  THEN

        ALTER TABLE `zstack`.`ScsiLunHostRefVO`
            ADD COLUMN `hctl` VARCHAR(64) DEFAULT NULL,
            ADD COLUMN `path` VARCHAR(128) DEFAULT NULL;

        UPDATE `zstack`.`ScsiLunHostRefVO` ref
            INNER JOIN `zstack`.`ScsiLunVO` lun ON ref.scsiLunUuid = lun.uuid
        SET ref.path = lun.path, ref.hctl = lun.hctl;

    END IF;
END $$
DELIMITER ;

CALL Alter_SCSI_Table();
DROP PROCEDURE Alter_SCSI_Table;

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
