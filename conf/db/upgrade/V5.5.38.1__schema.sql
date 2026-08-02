CALL ADD_COLUMN('AccountResourceRefVO', 'creatorVirtualIDUuid', 'VARCHAR(32)', 1, NULL);
CALL CREATE_INDEX('AccountResourceRefVO', 'idxAccountResourceRefVOCreatorVirtualIDUuid', 'creatorVirtualIDUuid');
