package org.zstack.header.allocator;

public interface HostAllocatorStrategyFactory {
    HostAllocatorStrategyType getHostAllocatorStrategyType();

    HostAllocatorStrategy getHostAllocatorStrategy();

    void marshalSpec(HostAllocatorSpec spec, AllocateHostMsg msg);
}
