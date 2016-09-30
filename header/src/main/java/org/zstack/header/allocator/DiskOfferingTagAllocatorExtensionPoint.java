package org.zstack.header.allocator;

import org.zstack.header.host.HostVO;
import org.zstack.header.tag.TagInventory;

import java.util.List;

/**
 */
public interface DiskOfferingTagAllocatorExtensionPoint {
    List<HostVO> allocateHost(List<TagInventory> tags, List<HostVO> candidates, HostAllocatorSpec spec);
}
