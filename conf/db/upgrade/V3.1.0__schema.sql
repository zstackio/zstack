ALTER TABLE `AlarmVO`  ADD COLUMN `type` varchar(32) NOT NULL;
UPDATE `AlarmVO` SET `type` = 'Any';

CREATE TABLE IF NOT EXISTS `V2VConversionCacheVO` (
    `id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT,
    `conversionHostUuid` varchar(32) NOT NULL,
    `srcVmUrl` varchar(255) NOT NULL,
    `installPath` varchar(255) NOT NULL,
    `deviceId` int unsigned NOT NULL,
    `virtualSize` bigint unsigned NOT NULL,
    `actualSize` bigint unsigned NOT NULL,
    `bootMode` varchar(64) DEFAULT NULL,
    `lastOpDate` timestamp ON UPDATE CURRENT_TIMESTAMP,
    `createDate` timestamp,
    PRIMARY KEY  (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE UNIQUE INDEX `type` ON NetworkServiceProviderVO(`type`);
CREATE INDEX idxVmUsageVOaccountUuid ON VmUsageVO(accountUuid, dateInLong);

DROP PROCEDURE IF EXISTS updateClusterHostCpuModelCheckTag;
DELIMITER $$
CREATE PROCEDURE updateClusterHostCpuModelCheckTag()
    BEGIN
        DECLARE clusterUuid VARCHAR(32);
        DECLARE tagUuid VARCHAR(32);
        DECLARE done INT DEFAULT FALSE;
        DECLARE cur CURSOR FOR SELECT uuid FROM ClusterVO;
        DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
        OPEN cur;
        read_loop: LOOP
            FETCH cur INTO clusterUuid;
            IF done THEN
                LEAVE read_loop;
            END IF;

            SET tagUuid = REPLACE(UUID(), '-', '');

            IF (select count(*) from SystemTagVO systemTag where systemTag.type = 'System' and systemTag.tag like '%clusterKVMCpuModel::%') != 0 THEN
            BEGIN
            INSERT INTO zstack.SystemTagVO (`uuid`, `resourceUuid`, `resourceType`, `inherent`, `type`, `tag`, `lastOpDate`, `createDate`)
                    values (tagUuid, clusterUuid, 'ClusterVO', 0, 'System', 'check::cluster::cpu::model::true', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP());
            END;
            END IF;
        END LOOP;
        CLOSE cur;
        # work around a bug of mysql : jira.mariadb.org/browse/MDEV-4602
        SELECT CURTIME();
    END $$
DELIMITER ;

CALL updateClusterHostCpuModelCheckTag();
DROP PROCEDURE IF EXISTS updateClusterHostCpuModelCheckTag;

ALTER TABLE `zstack`.`LongJobVO` MODIFY COLUMN `jobData` mediumtext NOT NULL;
ALTER TABLE `zstack`.`LongJobVO` MODIFY COLUMN `jobResult` mediumtext DEFAULT NULL;
