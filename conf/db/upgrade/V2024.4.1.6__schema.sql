-- in version zsv_4.1.6
-- from issue: (feature) ZSV-4408

ALTER TABLE `zstack`.`VmSchedHistoryVO` ADD COLUMN `reason` text DEFAULT NULL;
