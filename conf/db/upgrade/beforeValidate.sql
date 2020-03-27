-- Update `schema_version' since we've changed SQL to match the expectation
-- of the newer version MariaDB.
DELIMITER $$

DROP PROCEDURE IF EXISTS `update_schema_checksum` $$

CREATE PROCEDURE `update_schema_checksum`()
BEGIN
    IF EXISTS(SELECT schema_name   FROM information_schema.schemata  WHERE schema_name = 'zstack')
    THEN
        IF EXISTS(SELECT table_name FROM information_schema.tables WHERE table_name = 'schema_version')
        THEN
            update `zstack`.`schema_version` set `checksum`=1083194846  where `script`='V1.6__schema.sql'   and `checksum` <> 1083194846;
            update `zstack`.`schema_version` set `checksum`=-1569422253 where `script`='V2.1.0__schema.sql' and `checksum` <> -1569422253;
            update `zstack`.`schema_version` set `checksum`=1564279419  where `script`='V3.0.0__schema.sql' and `checksum` <> 1564279419;
            update `zstack`.`schema_version` set `checksum`=565652311   where `script`='V3.7.2__schema.sql' and `checksum` <> 565652311;
        END IF;
    END IF;
END $$

DELIMITER  ;

CALL update_schema_checksum();
DROP PROCEDURE IF EXISTS update_schema_checksum;
