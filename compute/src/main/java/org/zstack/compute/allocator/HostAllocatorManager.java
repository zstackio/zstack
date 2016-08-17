package org.zstack.compute.allocator;

import org.zstack.header.allocator.HostAllocatorStrategyFactory;
import org.zstack.header.allocator.HostAllocatorStrategyType;

import java.util.List;
import java.util.Map;

public interface HostAllocatorManager {
	HostAllocatorStrategyFactory getHostAllocatorStrategyFactory(HostAllocatorStrategyType type);

	Map<String, List<String>> getBackupStoragePrimaryStorageMetrics();
	
	void returnCapacity(String uuid, long cpu, long memory);
}
