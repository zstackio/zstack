package org.zstack.compute.allocator;

import org.zstack.header.allocator.AbstractHostAllocatorFlow;
import org.zstack.header.exception.CloudRuntimeException;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class HostAllocatorChainBuilder {
    private List<String> flowClassNames;
    private boolean isConstructed;
    private List<Class> classes = new ArrayList<Class>();

    public static HostAllocatorChain newAllocationChain() {
        return new HostAllocatorChain();
    }

    public static HostAllocatorChainBuilder newBuilder() {
        return new HostAllocatorChainBuilder();
    }

    public HostAllocatorChainBuilder construct() {
        try {
            for (String clzName : flowClassNames) {
                classes.add(Class.forName(clzName));
            }

            isConstructed = true;
            return this;
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
    }

    private List<AbstractHostAllocatorFlow> buildFlows() {
        List<AbstractHostAllocatorFlow> flows = new ArrayList<AbstractHostAllocatorFlow>();
        try {
            for (Class flowClass : classes) {
                flows.add((AbstractHostAllocatorFlow) flowClass.newInstance());
            }
        } catch (Exception e) {
            throw new CloudRuntimeException(e);
        }
        return flows;
    }

    public HostAllocatorChain build() {
        if (!isConstructed) {
            construct();
        }

        HostAllocatorChain chain = newAllocationChain();
        chain.setFlows(buildFlows());
        return chain;
    }

    public List<String> getFlowClassNames() {
        return flowClassNames;
    }

    public HostAllocatorChainBuilder setFlowClassNames(List<String> flowClassNames) {
        this.flowClassNames = flowClassNames;
        return this;
    }
}
