-- Append-only for both official and historical experimental memory cohorts.
-- Never alter checksums of previously applied memory migrations.
CREATE TABLE IF NOT EXISTS zstack.MemoryQueryVersionVO (
    name VARCHAR(16) NOT NULL,
    revision BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

INSERT IGNORE INTO zstack.MemoryQueryVersionVO (name, revision) VALUES ('state', 0), ('task', 0);
CREATE INDEX memory_task_created_uuid ON zstack.MemoryTaskVO (createDate, uuid);
