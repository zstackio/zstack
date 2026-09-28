-- Generic runtime metadata for multi-vendor AI VM image selection.
-- Kept in a new migration because V5.5.38 may already be applied.

CALL ADD_COLUMN('ModelServiceTemplateVO', 'runtimeName', 'VARCHAR(255)', 1, NULL);
CALL ADD_COLUMN('ModelServiceTemplateVO', 'runtimeVersionPattern', 'VARCHAR(2048)', 1, NULL);
