CREATE TABLE zstack.S3BackupStorageVO
(
    uuid         varchar(32)  NOT NULL UNIQUE,
    region       varchar(255) NOT NULL,
    endpoint     varchar(255) NOT NULL,
    bucket       varchar(255) NOT NULL,
    accessStyle  varchar(32)  NOT NULL,
    akeyUuid     varchar(32),
    PRIMARY KEY (uuid),
    CONSTRAINT fkS3BackupStorageVHybridAccountVO FOREIGN KEY (akeyUuid) REFERENCES zstack.HybridAccountVO (uuid) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8;