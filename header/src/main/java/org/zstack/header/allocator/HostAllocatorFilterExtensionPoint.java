package org.zstack.header.allocator;

import org.zstack.header.host.HostVO;
import org.zstack.header.core.ReturnValueCompletion;

import java.util.List;

/**
 * Created by frank on 7/2/2015.
 */
public interface HostAllocatorFilterExtensionPoint {
    List<HostVO> filterHostCandidates(List<HostVO> candidates, HostAllocatorSpec spec);
    default void asyncFilterHostCandidates(List<HostVO> candidates, HostAllocatorSpec spec, ReturnValueCompletion completion) {
        completion.success(candidates);
    }
    String filterErrorReason();
}
