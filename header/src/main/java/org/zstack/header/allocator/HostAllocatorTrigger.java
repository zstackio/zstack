package org.zstack.header.allocator;

import org.zstack.header.host.HostVO;

import java.util.List;

/**
 */
public interface HostAllocatorTrigger {
    void next(List<HostVO> candidates);

    void skip();

    int indexOfFlow(AbstractHostAllocatorFlow flow);
}
