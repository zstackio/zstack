package org.zstack.compute.allocator;

import org.zstack.header.allocator.HostAllocatorConstant;
import org.zstack.header.allocator.HostAllocatorStrategy;
import org.zstack.header.allocator.HostAllocatorStrategyType;
import org.zstack.header.allocator.MarshalResultFunction;
import org.zstack.header.host.HostVO;
import java.util.Collections;
import java.util.List;

public class DefaultHostAllocatorStrategyFactory extends AbstractHostAllocatorStrategyFactory {
	private static final HostAllocatorStrategyType type = new HostAllocatorStrategyType(HostAllocatorConstant.DEFAULT_HOST_ALLOCATOR_STRATEGY_TYPE);

	public HostAllocatorStrategy getHostAllocatorStrategy() {
		HostAllocatorStrategy strategy = builder.build();
		strategy.setMarshalResultFunction(new MarshalResultFunction() {
			@Override
			public void marshal(List<HostVO> hosts) {
				Collections.shuffle(hosts);
			}
		});
		return strategy;
	}

	@Override
    public HostAllocatorStrategyType getHostAllocatorStrategyType() {
	    return type;
    }
}
