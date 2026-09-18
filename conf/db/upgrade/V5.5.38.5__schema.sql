-- The original V5.5.38 schema created these columns as varchar(32) on some
-- installations. Editing that already-applied migration does not widen them.
-- Marketplace application instance UUIDs contain hyphens and require 36 characters.

ALTER TABLE `zstack`.`ZsDatasetSpaceRefVO`
    MODIFY COLUMN `appInstanceUuid` varchar(36) NOT NULL;

ALTER TABLE `zstack`.`ZsDatasetServiceKeyVO`
    MODIFY COLUMN `appInstanceUuid` varchar(36) NOT NULL;

ALTER TABLE `zstack`.`ZsDatasetPublicationVO`
    MODIFY COLUMN `appInstanceUuid` varchar(36) NOT NULL;
