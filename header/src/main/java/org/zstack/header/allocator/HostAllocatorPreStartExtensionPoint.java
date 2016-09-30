package org.zstack.header.allocator;

import java.util.List;

/**
 * Created by frank on 9/19/2015.
 */
public interface HostAllocatorPreStartExtensionPoint {
    void beforeHostAllocatorStart(HostAllocatorSpec spec, List<AbstractHostAllocatorFlow> flows);
}
