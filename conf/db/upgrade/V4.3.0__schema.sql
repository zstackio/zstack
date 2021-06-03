ALTER TABLE `zstack`.`AutoScalingGroupActivityVO` ADD COLUMN `instanceUuids` varchar(2048) DEFAULT NULL;
ALTER TABLE QuotaVO MODIFY COLUMN `value` bigint DEFAULT 0;