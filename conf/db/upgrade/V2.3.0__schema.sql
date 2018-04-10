#vpc ipsec
CREATE TABLE IF NOT EXISTS `IPsecL3NetworkRefVO` (
    `uuid` VARCHAR(32) NOT NULL UNIQUE COMMENT 'uuid',
    `connectionUuid` VARCHAR(32) NOT NULL,
    `l3NetworkUuid` VARCHAR(32) NOT NULL,
    `lastOpDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
    PRIMARY KEY (`uuid`),
    CONSTRAINT `fkIPsecL3NetworkRefVOIPsecConnectionVO` FOREIGN KEY (`connectionUuid`) REFERENCES `zstack`.`IPsecConnectionVO` (`uuid`) ON DELETE CASCADE,
    CONSTRAINT `fkIPsecL3NetworkRefVOL3NetworkEO` FOREIGN KEY (`l3NetworkUuid`) REFERENCES `zstack`.`L3NetworkEO` (`uuid`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DELIMITER $$
CREATE PROCEDURE migrateIPsecL3Network()
    BEGIN
        DECLARE uuid VARCHAR(32);
        DECLARE connectionUuid VARCHAR(32);
        DECLARE l3NetworkUuid VARCHAR(32);
        DECLARE done INT DEFAULT FALSE;
        DECLARE cur CURSOR FOR SELECT ipsec.uuid, ipsec.l3NetworkUuid FROM  zstack.IPsecConnectionVO ipsec;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
        OPEN cur;
        read_loop: LOOP
            FETCH cur INTO connectionUuid, l3NetworkUuid;
            IF done THEN
                LEAVE read_loop;
            END IF;

            SET uuid = REPLACE(UUID(), '-', '');
            SELECT uuid, connectionUuid, l3NetworkUuid;
            INSERT INTO zstack.IPsecL3NetworkRefVO (uuid, connectionUuid, l3NetworkUuid, lastOpDate, createDate)
                                     values(uuid, connectionUuid, l3NetworkUuid, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());
        END LOOP;
        CLOSE cur;
        # work around a bug of mysql : jira.mariadb.org/browse/MDEV-4602
        SELECT CURTIME();
    END $$
DELIMITER ;

call migrateIPsecL3Network();
DROP PROCEDURE IF EXISTS migrateIPsecL3Network;
ALTER TABLE zstack.IPsecConnectionVO DROP FOREIGN KEY fkIPsecConnectionVOL3NetworkVO;
ALTER TABLE zstack.IPsecConnectionVO DROP COLUMN l3NetworkUuid;