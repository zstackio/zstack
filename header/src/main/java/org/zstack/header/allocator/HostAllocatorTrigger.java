package org.zstack.header.allocator;

import org.zstack.header.host.HostVO;

import java.util.List;

/**
 */
public interface HostAllocatorTrigger {
    void moveOn();

    void next(List<HostVO> candidates);

    int indexOfFlow(AbstractHostAllocatorFlow flow);
}
