package org.zstack.compute.allocator;

import org.zstack.header.Component;
import org.zstack.header.allocator.*;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractHostAllocatorStrategyFactory implements HostAllocatorStrategyFactory, Component {
    protected HostAllocatorChainBuilder builder;
    protected HostAllocatorChainBuilder sorter;
    private List<String> allocatorFlowNames = new ArrayList<>();
    private List<String> sortFlowNames = new ArrayList<>();

    public HostAllocatorStrategy getHostAllocatorStrategy() {
        return builder.build();
    }

    public HostSortorStrategy getHostSortorStrategy() {
        return sorter.buildSort();
    }

    public abstract HostAllocatorStrategyType getHostAllocatorStrategyType();

    public void setAllocatorFlowNames(List<String> allocatorFlowNames) {
        this.allocatorFlowNames = allocatorFlowNames;
    }

    public List<String> getAllocatorFlowNames() {
        return allocatorFlowNames;
    }

    public List<String> getSortFlowNames() {
        return sortFlowNames;
    }

    public void setSortFlowNames(List<String> sortFlowNames) {
        this.sortFlowNames = sortFlowNames;
    }

    public boolean start() {
        builder = HostAllocatorChainBuilder.newBuilder().setFlowClassNames(allocatorFlowNames).construct();
        sorter = HostAllocatorChainBuilder.newBuilder().setFlowClassNames(sortFlowNames).construct();
        return true;
    }

    public boolean stop() {
        return true;
    }

    public void marshalSpec(HostAllocatorSpec spec, AllocateHostMsg msg) {
        if (msg instanceof DesignatedAllocateHostMsg) {
            DesignatedAllocateHostMsg dmsg = (DesignatedAllocateHostMsg)msg;
            spec.getExtraData().put(HostAllocatorConstant.LocationSelector.zone, dmsg.getZoneUuid());
            spec.getExtraData().put(HostAllocatorConstant.LocationSelector.cluster, dmsg.getClusterUuids());
            spec.getExtraData().put(HostAllocatorConstant.LocationSelector.host, dmsg.getHostUuid());
        }
    }
}
