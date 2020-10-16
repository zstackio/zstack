package org.zstack.compute.allocator;

import org.zstack.header.allocator.HostAllocatorStrategyFactory;
import org.zstack.header.allocator.HostAllocatorStrategyType;

public interface HostAllocatorManager {
    HostAllocatorStrategyFactory getHostAllocatorStrategyFactory(HostAllocatorStrategyType type);

    void returnComputeResourceCapacity(String uuid, long cpu, long memory);
}
