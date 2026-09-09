-- Freeze the AIOS model center chosen for each zsdataset publication.
--
-- Existing rows intentionally remain NULL: they predate target persistence and cannot be retried
-- safely without asking the caller to choose a target again. ADD_COLUMN is provided by
-- beforeMigrate.sql and makes the migration safe when the column is already present.

CALL ADD_COLUMN('ZsDatasetPublicationVO', 'modelCenterUuid', 'VARCHAR(32)', 1, NULL);
