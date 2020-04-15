ALTER TABLE JsonLabelVO MODIFY COLUMN labelValue MEDIUMTEXT;

CREATE INDEX idxTaskProgressVOapiId ON TaskProgressVO(apiId);

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE DahoVllVbrRefVO;
DROP TABLE DahoCloudConnectionVO;
DROP TABLE DahoVllsVO;
DROP TABLE DahoConnectionVO;
DROP TABLE DahoDCAccessVO;

SET FOREIGN_KEY_CHECKS = 1;

ALTER TABLE ImageBackupStorageRefVO ADD COLUMN exportMd5Sum VARCHAR(255) DEFAULT NULL;
ALTER TABLE ImageBackupStorageRefVO ADD COLUMN exportUrl VARCHAR(2048) DEFAULT NULL;
UPDATE ImageBackupStorageRefVO ibs, ImageVO i SET ibs.exportMd5Sum = i.exportMd5Sum, ibs.exportUrl = i.exportUrl WHERE ibs.imageUuid = i.uuid;
DROP VIEW IF EXISTS `zstack`.`ImageVO`;
CREATE VIEW `zstack`.`ImageVO` AS SELECT uuid, name, description, status, state, size, actualSize, md5Sum, platform, type, format, url, system, mediaType, createDate, lastOpDate, guestOsType FROM `zstack`.`ImageEO` WHERE deleted IS NULL;
ALTER TABLE ImageEO DROP exportMd5Sum, DROP exportUrl;

ALTER TABLE `zstack`.`PolicyRouteRuleSetVO` ADD COLUMN type VARCHAR(64) DEFAULT "User" NOT NULL;
ALTER TABLE `zstack`.`PolicyRouteTableVO` ADD COLUMN type VARCHAR(64) DEFAULT "User" NOT NULL;

ALTER TABLE `zstack`.`SchedulerJobHistoryVO` CHANGE COLUMN `startTime` `startTime` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE `zstack`.`SchedulerJobHistoryVO` ADD COLUMN jobType VARCHAR(255) DEFAULT NULL;
ALTER TABLE `zstack`.`SchedulerJobHistoryVO` ADD COLUMN fireInstanceId VARCHAR(32) DEFAULT NULL;
ALTER TABLE `zstack`.`SchedulerJobHistoryVO` ADD INDEX idxSchedulerJobHistoryVOStartTime (`startTime`);
ALTER TABLE `zstack`.`SchedulerJobHistoryVO` ADD INDEX idxSchedulerJobHistoryVOFireInstanceId (`fireInstanceId`);

UPDATE `zstack`.`SchedulerJobGroupVO` SET `jobClassName` = 'org.zstack.storage.backup.CreateRootVolumeBackupJob' WHERE `jobType` = 'rootVolumeBackup';
UPDATE `zstack`.`SchedulerJobVO` job, `zstack`.`SchedulerJobGroupVO` jobGroup, `zstack`.`SchedulerJobGroupJobRefVO` ref
SET job.`jobClassName` = 'org.zstack.storage.backup.CreateRootVolumeBackupJob'
WHERE jobGroup.jobType = 'rootVolumeBackup' AND ref.schedulerJobGroupUuid = jobGroup.uuid AND ref.schedulerJobUuid = job.uuid;

