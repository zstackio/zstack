CALL ADD_COLUMN('MemoryMigrationVO', 'targetPoolGeneration', 'VARCHAR(128)', 1, NULL);
CALL ADD_COLUMN('MemoryMigrationVO', 'sourceHoldStatus', 'VARCHAR(32)', 1, NULL);
CALL ADD_COLUMN('MemoryMigrationVO', 'targetHoldStatus', 'VARCHAR(32)', 1, NULL);
CALL ADD_COLUMN('MemoryMigrationVO', 'swapDirect', 'TINYINT(1)', 0, '0');
CALL ADD_COLUMN('MemoryMigrationVO', 'swapDirectState', 'TEXT', 1, NULL);
