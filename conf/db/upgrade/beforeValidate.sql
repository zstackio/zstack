-- Update `schema_version' since we've changed SQL to match the expectation
-- of the newer version MariaDB.
--
-- WARNING: `schema_version' table doesn't exist on a clean environment, thus
-- after 'flyway clean' we prepared an empty table after 'flyway baseline'.
DELIMITER $$

-- One released 5.5.38 dataset build predates the final ZNS schema in V5.5.38.
-- Converge only that exact checksum before accepting the published migration.
-- Unknown histories remain subject to Flyway validation. Do not replay V5.5.38:
-- its other differences concern legacy capability tables removed by V5.5.48;
-- the dataset tables are preserved and widened by V5.5.48.2 instead.
DROP PROCEDURE IF EXISTS `zstack`.`converge_known_5538_schema` $$
CREATE PROCEDURE `zstack`.`converge_known_5538_schema`()
compat: BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables
                   WHERE table_schema = 'zstack' AND table_name = 'schema_version') THEN
        LEAVE compat;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM `zstack`.`schema_version`
                   WHERE `script` = 'V5.5.38__schema.sql' AND `success` = 1
                     AND `checksum` = 835350201) THEN
        LEAVE compat;
    END IF;
    IF EXISTS (SELECT 1 FROM `zstack`.`schema_version`
               WHERE `script` = 'V5.5.48__schema.sql' AND `success` = 1) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Known V5.5.38 compatibility must precede V5.5.48';
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = 'zstack' AND table_name = 'ZnsControllerVO'
                 AND column_name = 'peerManifestSupported' AND data_type <> 'tinyint') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Known V5.5.38 peerManifestSupported type is inconsistent';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = 'zstack' AND table_name = 'ZnsControllerVO'
                     AND column_name = 'peerManifestSupported') THEN
        ALTER TABLE `zstack`.`ZnsControllerVO`
            ADD COLUMN `peerManifestSupported` TINYINT(1) NULL;
    END IF;
    UPDATE `zstack`.`ZnsControllerVO`
    SET `peerManifestSupported` = CASE
        WHEN `compatibilityProofDigest` IS NOT NULL AND `compatibilityProofDigest` <> '' THEN 0
        ELSE 1 END
    WHERE `peerManifestSupported` IS NULL;
    ALTER TABLE `zstack`.`ZnsControllerVO`
        MODIFY COLUMN `peerManifestSupported` TINYINT(1) NOT NULL;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = 'zstack' AND table_name = 'ZnsTransportZoneVO'
                     AND column_name = 'siteUuid') THEN
        ALTER TABLE `zstack`.`ZnsTransportZoneVO` ADD COLUMN `siteUuid` VARCHAR(36) DEFAULT NULL;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = 'zstack' AND table_name = 'ZnsTenantRouterVO'
                     AND column_name = 'siteUuid') THEN
        ALTER TABLE `zstack`.`ZnsTenantRouterVO` ADD COLUMN `siteUuid` VARCHAR(36) DEFAULT NULL;
    END IF;

    CREATE TABLE IF NOT EXISTS `zstack`.`ZnsSiteVO` (
        `uuid` varchar(32) NOT NULL,
        `sdnControllerUuid` varchar(32) NOT NULL,
        `znsResourceUuid` varchar(36) NOT NULL,
        `name` varchar(255) NOT NULL,
        `description` varchar(1024) DEFAULT NULL,
        `createDate` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00',
        `lastOpDate` timestamp NOT NULL DEFAULT '2000-01-01 00:00:00' ON UPDATE CURRENT_TIMESTAMP,
        PRIMARY KEY (`uuid`),
        UNIQUE KEY `uk_zns_site_controller_uuid` (`sdnControllerUuid`, `znsResourceUuid`),
        KEY `idx_zns_site_controller` (`sdnControllerUuid`),
        CONSTRAINT `fkZnsSiteVOSdnControllerVO`
            FOREIGN KEY (`sdnControllerUuid`) REFERENCES `zstack`.`SdnControllerVO` (`uuid`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

    CREATE TABLE IF NOT EXISTS `zstack`.`ZnsResourceEventReceiptVO` (
        `uuid` varchar(32) NOT NULL,
        `controllerUuid` varchar(32) NOT NULL,
        `eventId` varchar(64) NOT NULL,
        `eventCursor` bigint NOT NULL,
        `payloadDigest` varchar(64) NOT NULL,
        `taskUuid` varchar(32) NOT NULL,
        `receivedAt` timestamp NOT NULL,
        PRIMARY KEY (`uuid`),
        UNIQUE KEY `ukZnsResourceEventReceiptEvent` (`controllerUuid`, `eventId`),
        UNIQUE KEY `ukZnsResourceEventReceiptCursor` (`controllerUuid`, `eventCursor`),
        CONSTRAINT `fkZnsResourceEventReceiptController` FOREIGN KEY (`controllerUuid`)
            REFERENCES `SdnControllerVO` (`uuid`) ON DELETE CASCADE,
        CONSTRAINT `fkZnsResourceEventReceiptTask` FOREIGN KEY (`taskUuid`)
            REFERENCES `ZnsNetworkNotificationVO` (`uuid`) ON DELETE CASCADE
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8;

    -- A pre-existing or partially upgraded table must satisfy the same contract.
    -- Never acknowledge the checksum for an incompatible table or weaken its keys.
    IF (SELECT COUNT(*) FROM information_schema.columns
        WHERE table_schema = 'zstack' AND (
            (table_name = 'ZnsControllerVO' AND column_name = 'peerManifestSupported'
             AND data_type = 'tinyint' AND is_nullable = 'NO') OR
            (table_name IN ('ZnsTransportZoneVO', 'ZnsTenantRouterVO') AND column_name = 'siteUuid'
             AND data_type = 'varchar' AND character_set_name = 'utf8' AND collation_name = 'utf8_general_ci' AND character_maximum_length = 36 AND is_nullable = 'YES')
        )) <> 3 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Known V5.5.38 compatibility columns are inconsistent';
    END IF;
    IF (SELECT COUNT(*) FROM information_schema.columns
        WHERE table_schema = 'zstack' AND table_name = 'ZnsSiteVO' AND (
            (column_name = 'uuid' AND data_type = 'varchar' AND character_set_name = 'utf8' AND collation_name = 'utf8_general_ci' AND character_maximum_length = 32 AND is_nullable = 'NO') OR
            (column_name = 'sdnControllerUuid' AND data_type = 'varchar' AND character_set_name = 'utf8' AND collation_name = 'utf8_general_ci' AND character_maximum_length = 32 AND is_nullable = 'NO') OR
            (column_name = 'znsResourceUuid' AND data_type = 'varchar' AND character_set_name = 'utf8' AND collation_name = 'utf8_general_ci' AND character_maximum_length = 36 AND is_nullable = 'NO') OR
            (column_name = 'name' AND data_type = 'varchar' AND character_set_name = 'utf8' AND collation_name = 'utf8_general_ci' AND character_maximum_length = 255 AND is_nullable = 'NO') OR
            (column_name = 'description' AND data_type = 'varchar' AND character_set_name = 'utf8' AND collation_name = 'utf8_general_ci' AND character_maximum_length = 1024 AND is_nullable = 'YES') OR
            (column_name = 'createDate' AND data_type = 'timestamp' AND is_nullable = 'NO') OR
            (column_name = 'lastOpDate' AND data_type = 'timestamp' AND is_nullable = 'NO')
        )) <> 7 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Known V5.5.38 ZnsSiteVO columns are inconsistent';
    END IF;
    IF (SELECT COUNT(*) FROM information_schema.columns
        WHERE table_schema = 'zstack' AND table_name = 'ZnsResourceEventReceiptVO' AND (
            (column_name = 'uuid' AND data_type = 'varchar' AND character_set_name = 'utf8' AND collation_name = 'utf8_general_ci' AND character_maximum_length = 32 AND is_nullable = 'NO') OR
            (column_name = 'controllerUuid' AND data_type = 'varchar' AND character_set_name = 'utf8' AND collation_name = 'utf8_general_ci' AND character_maximum_length = 32 AND is_nullable = 'NO') OR
            (column_name = 'eventId' AND data_type = 'varchar' AND character_set_name = 'utf8' AND collation_name = 'utf8_general_ci' AND character_maximum_length = 64 AND is_nullable = 'NO') OR
            (column_name = 'eventCursor' AND data_type = 'bigint' AND column_type NOT LIKE '%unsigned%') OR
            (column_name = 'payloadDigest' AND data_type = 'varchar' AND character_set_name = 'utf8' AND collation_name = 'utf8_general_ci' AND character_maximum_length = 64 AND is_nullable = 'NO') OR
            (column_name = 'taskUuid' AND data_type = 'varchar' AND character_set_name = 'utf8' AND collation_name = 'utf8_general_ci' AND character_maximum_length = 32 AND is_nullable = 'NO') OR
            (column_name = 'receivedAt' AND data_type = 'timestamp' AND is_nullable = 'NO')
        )) <> 7 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Known V5.5.38 ZnsResourceEventReceiptVO columns are inconsistent';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
                   WHERE table_schema = 'zstack' AND table_name = 'ZnsSiteVO'
                     AND non_unique = 0 AND index_name = 'PRIMARY'
                   GROUP BY index_name HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index) = 'uuid') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Known V5.5.38 ZnsSiteVO unique key is inconsistent';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
                   WHERE table_schema = 'zstack' AND table_name = 'ZnsSiteVO'
                     AND non_unique = 0
                   GROUP BY index_name HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index) = 'sdnControllerUuid,znsResourceUuid') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Known V5.5.38 ZnsSiteVO unique key is inconsistent';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
                   WHERE table_schema = 'zstack' AND table_name = 'ZnsResourceEventReceiptVO'
                     AND non_unique = 0 AND index_name = 'PRIMARY'
                   GROUP BY index_name HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index) = 'uuid') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Known V5.5.38 ZnsResourceEventReceiptVO unique key is inconsistent';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
                   WHERE table_schema = 'zstack' AND table_name = 'ZnsResourceEventReceiptVO'
                     AND non_unique = 0
                   GROUP BY index_name HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index) = 'controllerUuid,eventId') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Known V5.5.38 ZnsResourceEventReceiptVO unique key is inconsistent';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
                   WHERE table_schema = 'zstack' AND table_name = 'ZnsResourceEventReceiptVO'
                     AND non_unique = 0
                   GROUP BY index_name HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index) = 'controllerUuid,eventCursor') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Known V5.5.38 ZnsResourceEventReceiptVO unique key is inconsistent';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.key_column_usage k
                   JOIN information_schema.referential_constraints c
                     ON c.constraint_schema = k.constraint_schema AND c.table_name = k.table_name
                    AND c.constraint_name = k.constraint_name
                   WHERE k.table_schema = 'zstack' AND k.table_name = 'ZnsSiteVO'
                     AND k.column_name = 'sdnControllerUuid' AND k.referenced_table_schema = 'zstack'
                     AND k.referenced_table_name = 'SdnControllerVO' AND k.referenced_column_name = 'uuid'
                     AND c.delete_rule = 'CASCADE' AND k.ordinal_position = 1
                     AND NOT EXISTS (SELECT 1 FROM information_schema.key_column_usage k2
                                     WHERE k2.constraint_schema = k.constraint_schema
                                       AND k2.table_name = k.table_name AND k2.constraint_name = k.constraint_name
                                       AND k2.ordinal_position > 1)) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Known V5.5.38 ZnsSiteVO foreign key is inconsistent';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.key_column_usage k
                   JOIN information_schema.referential_constraints c
                     ON c.constraint_schema = k.constraint_schema AND c.table_name = k.table_name
                    AND c.constraint_name = k.constraint_name
                   WHERE k.table_schema = 'zstack' AND k.table_name = 'ZnsResourceEventReceiptVO'
                     AND k.column_name = 'controllerUuid' AND k.referenced_table_schema = 'zstack'
                     AND k.referenced_table_name = 'SdnControllerVO' AND k.referenced_column_name = 'uuid'
                     AND c.delete_rule = 'CASCADE' AND k.ordinal_position = 1
                     AND NOT EXISTS (SELECT 1 FROM information_schema.key_column_usage k2
                                     WHERE k2.constraint_schema = k.constraint_schema
                                       AND k2.table_name = k.table_name AND k2.constraint_name = k.constraint_name
                                       AND k2.ordinal_position > 1)) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Known V5.5.38 ZnsResourceEventReceiptVO foreign key is inconsistent';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.key_column_usage k
                   JOIN information_schema.referential_constraints c
                     ON c.constraint_schema = k.constraint_schema AND c.table_name = k.table_name
                    AND c.constraint_name = k.constraint_name
                   WHERE k.table_schema = 'zstack' AND k.table_name = 'ZnsResourceEventReceiptVO'
                     AND k.column_name = 'taskUuid' AND k.referenced_table_schema = 'zstack'
                     AND k.referenced_table_name = 'ZnsNetworkNotificationVO' AND k.referenced_column_name = 'uuid'
                     AND c.delete_rule = 'CASCADE' AND k.ordinal_position = 1
                     AND NOT EXISTS (SELECT 1 FROM information_schema.key_column_usage k2
                                     WHERE k2.constraint_schema = k.constraint_schema
                                       AND k2.table_name = k.table_name AND k2.constraint_name = k.constraint_name
                                       AND k2.ordinal_position > 1)) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Known V5.5.38 ZnsResourceEventReceiptVO foreign key is inconsistent';
    END IF;

    UPDATE `zstack`.`schema_version` SET `checksum` = -2089553698
    WHERE `script` = 'V5.5.38__schema.sql' AND `success` = 1 AND `checksum` = 835350201;
END $$

CALL `zstack`.`converge_known_5538_schema`() $$
DROP PROCEDURE `zstack`.`converge_known_5538_schema` $$


DROP PROCEDURE IF EXISTS `zstack`.`update_schema_checksum` $$

CREATE PROCEDURE `zstack`.`update_schema_checksum`()
BEGIN
    IF EXISTS(SELECT table_name FROM information_schema.tables WHERE table_name = 'schema_version')
    THEN
        update `zstack`.`schema_version` set `checksum`=1083194846  where `script`='V1.6__schema.sql'   and `checksum` <> 1083194846;
        update `zstack`.`schema_version` set `checksum`=-1569422253 where `script`='V2.1.0__schema.sql' and `checksum` <> -1569422253;
        update `zstack`.`schema_version` set `checksum`=1564279419  where `script`='V3.0.0__schema.sql' and `checksum` <> 1564279419;
        update `zstack`.`schema_version` set `checksum`=-50951064  where `script`='V3.3.0__schema.sql' and `checksum` <> -50951064;
        update `zstack`.`schema_version` set `checksum`=2098205637  where `script`='V3.4.0__schema.sql' and `checksum` <> 2098205637;
        update `zstack`.`schema_version` set `checksum`=152820878  where `script`='V3.6.0__schema.sql' and `checksum` <> 152820878;
        update `zstack`.`schema_version` set `checksum`=565652311   where `script`='V3.7.2__schema.sql' and `checksum` <> 565652311;
        update `zstack`.`schema_version` set `checksum`=1556501192  where `script`='V3.9.0.2__schema.sql' and `checksum` <> 1556501192;
        update `zstack`.`schema_version` set `checksum`=-143027462   where `script`='V3.9.1__schema.sql' and `checksum` <> -143027462;
        update `zstack`.`schema_version` set `checksum`=514679307  where `script`='V3.10.0__schema.sql' and `checksum` <> 514679307;
        update `zstack`.`schema_version` set `checksum`=-1316015634   where `script`='V4.0.0__schema.sql' and `checksum` <> -1316015634;
        update `zstack`.`schema_version` set `checksum`=1243948617   where `script`='V4.3.8.1__schema.sql' and `checksum` <> 1243948617;
        update `zstack`.`schema_version` set `checksum`=-395682061   where `script`='V4.3.35__schema.sql' and `checksum` <> -395682061;
        update `zstack`.`schema_version` set `checksum`=-540021638   where `script`='V4.4.6__schema.sql' and `checksum` <> -540021638;
        update `zstack`.`schema_version` set `checksum`=-698734653   where `script`='V4.5.11__schema.sql' and `checksum` <> -698734653;
        update `zstack`.`schema_version` set `checksum`=-2137714083   where `script`='V4.7.0__schema.sql' and `checksum` <> -2137714083;
        update `zstack`.`schema_version` set `checksum`=-1493191986   where `script`='V0.6__schema.sql' and `checksum` <> -1493191986;
        update `zstack`.`schema_version` set `checksum`=286222955   where `script`='V1.3__schema.sql' and `checksum` <> 286222955;
        update `zstack`.`schema_version` set `checksum`=390362109   where `script`='V1.7__schema.sql' and `checksum` <> 390362109;
        update `zstack`.`schema_version` set `checksum`=-1453565511   where `script`='V2.1.0__schema.sql' and `checksum` <> -1453565511;
        update `zstack`.`schema_version` set `checksum`=672814727   where `script`='V2.2.0__schema.sql' and `checksum` <> 672814727;
        update `zstack`.`schema_version` set `checksum`=271601676  where `script`='V3.1.0__schema.sql' and `checksum` <> 271601676;
        update `zstack`.`schema_version` set `checksum`=-294520100   where `script`='V4.6.21__schema.sql' and `checksum` <> -294520100;
        update `zstack`.`schema_version` set `checksum`=1170348213   where `script`='V4.8.0.6__schema.sql' and `checksum` <> 1170348213;
        update `zstack`.`schema_version` set `checksum`=1298863127   where `script`='V5.3.0__schema.sql' and `checksum` <> 1298863127;
        update `zstack`.`schema_version` set `checksum`=613548663   where `script`='V5.3.6__schema.sql' and `checksum` <> 613548663;
        update `zstack`.`schema_version` set `checksum`=1489363540   where `script`='V5.3.22__schema.sql' and `checksum` <> 1489363540;
        update `zstack`.`schema_version` set `checksum`=1648524318   where `script`='V4.4.0__schema.sql' and `checksum` <> 1648524318;
        update `zstack`.`schema_version` set `checksum`=268584834   where `script`='V5.4.6__schema.sql' and `checksum` <> 268584834;
        update `zstack`.`schema_version` set `checksum`=-1455020895   where `script`='V5.1.4__schema.sql' and `checksum` <> -1455020895;
        update `zstack`.`schema_version` set `checksum`=569762641   where `script`='V5.4.0__schema.sql' and `checksum` <> 569762641;
        update `zstack`.`schema_version` set `checksum`=1175380657   where `script`='V5.4.2__schema.sql' and `checksum` <> 1175380657;
    END IF;
END $$

DELIMITER  ;

CALL `zstack`.update_schema_checksum();
DROP PROCEDURE IF EXISTS `zstack`.update_schema_checksum;
