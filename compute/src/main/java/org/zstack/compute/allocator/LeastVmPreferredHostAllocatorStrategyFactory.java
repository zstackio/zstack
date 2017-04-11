package org.zstack.compute.allocator;

import org.zstack.header.allocator.HostAllocatorConstant;
import org.zstack.header.allocator.HostAllocatorStrategyType;

public class LeastVmPreferredHostAllocatorStrategyFactory extends AbstractHostAllocatorStrategyFactory {
	private static final HostAllocatorStrategyType type = new HostAllocatorStrategyType(HostAllocatorConstant.LEAST_VM_PREFERRED_HOST_ALLOCATOR_STRATEGY_TYPE);

	@Override
    public HostAllocatorStrategyType getHostAllocatorStrategyType() {
	    return type;
    }
}
