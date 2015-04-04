package org.zstack.storage.primary;

import org.zstack.core.workflow.FlowChainBuilder;
import org.zstack.header.Component;
import org.zstack.header.storage.primary.PrimaryStorageAllocatorStrategy;
import org.zstack.header.storage.primary.PrimaryStorageAllocatorStrategyFactory;
import org.zstack.header.storage.primary.PrimaryStorageAllocatorStrategyType;
import org.zstack.header.storage.primary.PrimaryStorageConstant;

import java.util.List;

public class DefaultPrimaryStorageAllocatorFactory implements PrimaryStorageAllocatorStrategyFactory, Component {
	private static final PrimaryStorageAllocatorStrategyType type = new PrimaryStorageAllocatorStrategyType(PrimaryStorageConstant.DEFAULT_PRIMARY_STORAGE_ALLOCATION_STRATEGY_TYPE);
	private DefaultPrimaryStorageAllocatorStrategy strategy;
    private List<String> allocatorFlowNames;
    private FlowChainBuilder builder = new FlowChainBuilder();

    public void setAllocatorFlowNames(List<String> allocatorFlowNames) {
        this.allocatorFlowNames = allocatorFlowNames;
    }

    @Override
	public PrimaryStorageAllocatorStrategyType getPrimaryStorageAlloactorStrategyType() {
		return type;
	}

	@Override
	public PrimaryStorageAllocatorStrategy getPrimaryStorageAlloactorStrategy() {
		return strategy;
	}

	@Override
    public boolean start() {
        builder.setFlowClassNames(allocatorFlowNames).construct();
		strategy = new DefaultPrimaryStorageAllocatorStrategy(builder);
	    return true;
    }

	@Override
    public boolean stop() {
	    return true;
    }
}
