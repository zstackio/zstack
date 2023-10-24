CREATE TABLE IF NOT EXISTS `zstack`.`BlockPrimaryStorageHostRefVO` (
    `id` BIGINT UNSIGNED NOT NULL UNIQUE AUTO_INCREMENT,
    `initiatorName` varchar(256) DEFAULT NULL,
    `metadata` text DEFAULT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fkBlockPrimaryStorageHostRefVOPrimaryStorageHostRefVO` FOREIGN KEY (`id`) REFERENCES `zstack`.`PrimaryStorageHostRefVO` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

DROP PROCEDURE IF EXISTS `AlterBlockPrimaryStorageTable`;
DELIMITER $$
CREATE PROCEDURE AlterBlockPrimaryStorageTable()
    BEGIN
        IF EXISTS( SELECT NULL
                    FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE table_name = 'BlockPrimaryStorageVO'
                            AND table_schema = 'zstack'
                            AND column_name = 'encryptGatewayIp') THEN
            ALTER TABLE `zstack`.`BlockPrimaryStorageVO` DROP `encryptGatewayIp`;
        END IF;
        IF EXISTS( SELECT NULL
                    FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE table_name = 'BlockPrimaryStorageVO'
                            AND table_schema = 'zstack'
                            AND column_name = 'encryptGatewayPort') THEN
            ALTER TABLE `zstack`.`BlockPrimaryStorageVO` DROP `encryptGatewayPort`;
        END IF;
        IF EXISTS( SELECT NULL
                    FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE table_name = 'BlockPrimaryStorageVO'
                            AND table_schema = 'zstack'
                            AND column_name = 'encryptGatewayUsername') THEN
            ALTER TABLE `zstack`.`BlockPrimaryStorageVO` DROP `encryptGatewayUsername`;
        END IF;
        IF EXISTS( SELECT NULL
                    FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE table_name = 'BlockPrimaryStorageVO'
                            AND table_schema = 'zstack'
                            AND column_name = 'encryptGatewayPassword') THEN
            ALTER TABLE `zstack`.`BlockPrimaryStorageVO` DROP `encryptGatewayPassword`;
        END IF;
    END $$
DELIMITER ;
call AlterBlockPrimaryStorageTable;
DROP PROCEDURE IF EXISTS `AlterBlockPrimaryStorageTable`;

DROP PROCEDURE IF EXISTS `AlterBlockScsiLunTable`;
DELIMITER $$
CREATE PROCEDURE AlterBlockScsiLunTable()
    BEGIN
        IF EXISTS( SELECT NULL
                    FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE table_name = 'BlockScsiLunVO'
                            AND table_schema = 'zstack'
                            AND column_name = 'type') THEN
            ALTER TABLE `zstack`.`BlockScsiLunVO` DROP `type`;
        END IF;
        IF EXISTS( SELECT NULL
                    FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE table_name = 'BlockScsiLunVO'
                            AND table_schema = 'zstack'
                            AND column_name = 'id'
                            AND column_type like 'smallint%') THEN
            ALTER TABLE `zstack`.`BlockScsiLunVO` MODIFY COLUMN `id` int unsigned default 0;
        END IF;
        IF EXISTS( SELECT NULL
                    FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE table_name = 'BlockScsiLunVO'
                            AND table_schema = 'zstack'
                            AND column_name = 'lunType'
                            AND is_nullable = 'YES') THEN
            ALTER TABLE `zstack`.`BlockScsiLunVO` MODIFY COLUMN lunType varchar(256) DEFAULT NULL;
        END IF;
        IF EXISTS( SELECT NULL
                    FROM INFORMATION_SCHEMA.COLUMNS
                    WHERE table_name = 'BlockScsiLunVO'
                            AND table_schema = 'zstack'
                            AND column_name = 'lunMapId'
                            AND column_type like 'smallint%') THEN
            ALTER TABLE `zstack`.`BlockScsiLunVO` MODIFY COLUMN `lunMapId` int unsigned default 0;
        END IF;
    END $$
DELIMITER ;
call AlterBlockScsiLunTable;
DROP PROCEDURE IF EXISTS `AlterBlockScsiLunTable`;

DELIMITER $$
CREATE PROCEDURE checkAllBlockHostInPrimaryHostRef()
    BEGIN
        DECLARE hostUuid VARCHAR(32);
        DECLARE primaryStorageUuid VARCHAR(32);
        DECLARE done INT DEFAULT FALSE;
        DECLARE cur CURSOR FOR SELECT hiref.hostUuid, hiref.primaryStorageUuid FROM HostInitiatorRefVO hiref;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;

        OPEN cur;
        read_loop: LOOP
            FETCH cur INTO hostUuid, primaryStorageUuid;
            IF done THEN
                LEAVE read_loop;
            END IF;
            IF (select count(*) from PrimaryStorageHostRefVO pshref where pshref.hostUuid = hostUuid and pshref.primaryStorageUuid = primaryStorageUuid) = 0 THEN
                BEGIN
                    INSERT INTO zstack.PrimaryStorageHostRefVO (`primaryStorageUuid`, `hostUuid`, `status`, `lastOpDate`, `createDate`) values (primaryStorageUuid, hostUuid, 'Disconnected', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());
                END;
            END IF;
        END LOOP;
        CLOSE cur;
        SELECT CURTIME();
    END $$
DELIMITER ;

DELIMITER $$
CREATE PROCEDURE migrateBlockPrimaryHostRef()
    BEGIN
        DECLARE initiatorName VARCHAR(256);
        DECLARE psId BIGINT(20);
        DECLARE metadata text;
        DECLARE psUuid VARCHAR(32);
        DECLARE done INT DEFAULT FALSE;
        DECLARE cur CURSOR FOR SELECT  hostInitiatorRef.initiatorName, hostInitiatorRef.metadata, primaryStorageHostRef.id
             FROM zstack.HostInitiatorRefVO hostInitiatorRef, zstack.PrimaryStorageHostRefVO primaryStorageHostRef
             where hostInitiatorRef.hostUuid = primaryStorageHostRef.hostUuid and hostInitiatorRef.primaryStorageUuid = primaryStorageHostRef.primaryStorageUuid;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done =TRUE;
        OPEN cur;
        read_loop: LOOP
            FETCH cur INTO initiatorName, metadata, psId;
            IF done THEN
                LEAVE read_loop;
            END IF;
            IF (select count(*) from BlockPrimaryStorageHostRefVO bpshref where id = psId) = 0 THEN
                BEGIN
                    INSERT INTO zstack.BlockPrimaryStorageHostRefVO(id, initiatorName, metadata) values(psId, initiatorName, metadata);
                END;
            END IF;
        END LOOP;
        CLOSE cur;
        SELECT CURTIME();
    END $$
DELIMITER ;

DELIMITER $$
CREATE PROCEDURE checkHostInitiatorRefVO()
    BEGIN
        IF (SELECT count(*) FROM information_schema.columns WHERE table_name = 'HostInitiatorRefVO' AND column_name = 'primaryStorageUuid') != 0 THEN
            call checkAllBlockHostInPrimaryHostRef();
            call migrateBlockPrimaryHostRef();
        END IF;
    END $$
DELIMITER ;
call checkHostInitiatorRefVO();
DROP PROCEDURE IF EXISTS migrateBlockPrimaryHostRef;
DROP PROCEDURE IF EXISTS checkAllBlockHostInPrimaryHostRef;
DROP PROCEDURE IF EXISTS checkHostInitiatorRefVO;
DROP TABLE IF EXISTS HostInitiatorRefVO;
