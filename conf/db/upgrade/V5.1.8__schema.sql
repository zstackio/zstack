ALTER TABLE `zstack`.`AuditsVO` ADD COLUMN `startTime` bigint(20);

CREATE INDEX idx_startTime ON AuditsVO (startTime);
CREATE INDEX id_id_resourceType ON AuditsVO (id, resourceType);
CREATE INDEX idx_id_resourceType_startTime ON AuditsVO (id, resourceType, startTime);

UPDATE AuditsVO set startTime = createTime WHERE startTime IS NULL;

