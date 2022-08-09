-- Update `schema_version' since we've changed SQL to match the expectation
-- of the newer version MariaDB.
--
-- WARNING: `schema_version' table doesn't exist on a clean environment, thus
-- after 'flyway clean' we prepared an empty table after 'flyway baseline'.
DELIMITER $$

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
        update `zstack`.`schema_version` set `checksum`=-1475279966   where `script`='V4.3.8.1__schema.sql' and `checksum` <> -1094750249;
        update `zstack`.`schema_version` set `checksum`=-540021638   where `script`='V4.4.6__schema.sql' and `checksum` <> -540021638;
    END IF;
END $$

DELIMITER  ;

CALL `zstack`.update_schema_checksum();
DROP PROCEDURE IF EXISTS `zstack`.update_schema_checksum;
