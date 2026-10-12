CALL ADD_COLUMN('MemoryTaskVO', 'requiresKsmNativePartialFence', 'TINYINT(1)', 1, NULL);
CALL ADD_COLUMN('MemoryTaskVO', 'ksmNativeObservationBootId', 'VARCHAR(128)', 1, NULL);
CALL ADD_COLUMN('MemoryTaskVO', 'ksmNativeObservationSampleTime', 'BIGINT', 1, NULL);
