-- Add managementEndpoint column for ModelServiceInstanceGroupVO
-- This column was added in Java code (commit b25f49a211) but missed in V5.5.28.1 DDL

CALL ADD_COLUMN('ModelServiceInstanceGroupVO', 'managementEndpoint', 'VARCHAR(2048)', 1, NULL);
