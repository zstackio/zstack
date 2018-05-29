-- close foreign key checks during migration V2.4.0
SELECT version INTO @max_ver FROM `zstack`.`schema_version` ORDER BY version_rank DESC LIMIT 1;
SET FOREIGN_KEY_CHECKS = IF(@max_ver = "2.3.2", 0, @@FOREIGN_KEY_CHECKS);
SET FOREIGN_KEY_CHECKS = IF(@max_ver = "2.4.0", 1, @@FOREIGN_KEY_CHECKS);
