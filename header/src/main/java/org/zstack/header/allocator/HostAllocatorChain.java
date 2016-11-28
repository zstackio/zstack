package org.zstack.header.allocator;

import org.zstack.header.host.HostInventory;
import org.zstack.header.host.HostVO;

import java.util.List;


public interface HostAllocatorChain {
    void setNextChain(HostAllocatorChain next);

    HostAllocatorChain getNextChain();

    HostInventory allocate(List<HostVO> candidates, AllocateHostSpec spec) throws CloudNoAvailableHostException;
}
