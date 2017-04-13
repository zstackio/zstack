package org.zstack.compute.allocator;

import org.zstack.header.Component;
import org.zstack.header.allocator.*;

import java.util.List;

public abstract class AbstractHostAllocatorStrategyFactory implements HostAllocatorStrategyFactory, Component {
    protected HostAllocatorChainBuilder builder;
    private List<String> allocatorFlowNames;

    public HostAllocatorStrategy getHostAllocatorStrategy() {
        return builder.build();
    }

    public abstract HostAllocatorStrategyType getHostAllocatorStrategyType();

    public void setAllocatorFlowNames(List<String> allocatorFlowNames) {
        this.allocatorFlowNames = allocatorFlowNames;
    }

    public List<String> getAllocatorFlowNames() {
        return allocatorFlowNames;
    }

    public boolean start() {
        builder = HostAllocatorChainBuilder.newBuilder().setFlowClassNames(allocatorFlowNames).construct();
        return true;
    }

    public boolean stop() {
        return true;
    }

    public void marshalSpec(HostAllocatorSpec spec, AllocateHostMsg msg) {
    }
}
