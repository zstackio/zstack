-- One-time, fail-closed migration marker for the standard-config memory policy owner.
CREATE TABLE IF NOT EXISTS zstack.MemoryStandardConfigMigrationVO (
    uuid VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL,
    reason VARCHAR(2048) NULL,
    createDate TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lastOpDate TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (uuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
