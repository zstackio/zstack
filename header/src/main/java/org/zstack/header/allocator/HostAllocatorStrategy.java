package org.zstack.header.allocator;

import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.host.HostInventory;

import java.util.List;

public interface HostAllocatorStrategy {
    void allocate(HostAllocatorSpec spec, ReturnValueCompletion<HostInventory> completion);

    void dryRun(HostAllocatorSpec spec, ReturnValueCompletion<List<HostInventory>> completion);

    void setMarshalResultFunction(MarshalResultFunction func);
}
