package org.zstack.header.storage.primary;

public interface PrimaryStorageAllocatorStrategyFactory {
	PrimaryStorageAllocatorStrategyType getPrimaryStorageAlloactorStrategyType();

	PrimaryStorageAllocatorStrategy getPrimaryStorageAlloactorStrategy();
}
