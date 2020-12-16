CREATE TABLE zstack.S3BackupStorageVO
(
    uuid         varchar(32)  NOT NULL UNIQUE,
    region       varchar(255) NOT NULL,
    endpoint     varchar(255) NOT NULL,
    bucket       varchar(255) NOT NULL,
    usePathStyle boolean      NOT NULL DEFAULT TRUE,
    akeyUuid     varchar(32),
    PRIMARY KEY (uuid),
    CONSTRAINT fkS3BackupStorageVHybridAccountVO FOREIGN KEY (akeyUuid) REFERENCES zstack.HybridAccountVO (uuid) ON DELETE SET NULL,
    constraint uniqBucket unique (region, endpoint, bucket)
) ENGINE = InnoDB DEFAULT CHARSET = utf8;