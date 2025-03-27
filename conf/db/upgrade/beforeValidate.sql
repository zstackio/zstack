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
    END IF;
END $$

DELIMITER  ;

CALL `zstack`.update_schema_checksum();
DROP PROCEDURE IF EXISTS `zstack`.update_schema_checksum;
