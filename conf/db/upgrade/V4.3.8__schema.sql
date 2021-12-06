ALTER TABLE `BareMetal2ChassisVO` ADD COLUMN `provisionType` varchar(32) NOT NULL DEFAULT 'Remote';
ALTER TABLE `BareMetal2InstanceVO` ADD COLUMN `provisionType` varchar(32) NOT NULL DEFAULT 'Remote';
ALTER TABLE `BareMetal2ChassisOfferingVO` ADD COLUMN `provisionType` varchar(32) NOT NULL DEFAULT 'Remote';
ALTER TABLE `BareMetal2ChassisDiskVO` ADD COLUMN `wwn` varchar(128) DEFAULT NULL;
