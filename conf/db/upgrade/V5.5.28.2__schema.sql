-- Add managementEndpoint column for ModelServiceInstanceGroupVO
-- This column was added in Java code (commit b25f49a211) but missed in V5.5.28.1 DDL

CALL ADD_COLUMN('ModelServiceInstanceGroupVO', 'managementEndpoint', 'VARCHAR(2048)', 1, NULL);

-- Add component status columns for AIBusinessGatewayVO.
CALL ADD_COLUMN('AIBusinessGatewayVO', 'agentStatus', 'VARCHAR(32)', 1, 'Unknown');
CALL ADD_COLUMN('AIBusinessGatewayVO', 'dataPlaneStatus', 'VARCHAR(32)', 1, 'Unknown');
