ALTER TABLE `zstack`.`AutoScalingGroupActivityVO` ADD COLUMN `instanceUuids` varchar(2048) DEFAULT NULL;
ALTER TABLE `zstack`.`SNSEmailPlatformVO` modify COLUMN `username` VARCHAR(255) NOT NULL;