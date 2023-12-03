package org.zstack.header.allocator;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.host.HostVO;

import java.util.List;

/**
 */
public interface HostAllocatorTrigger {
    void next(List<HostVO> candidates);

    void skip();

    boolean isFirstFlow(AbstractHostAllocatorFlow flow);

    void fail(ErrorCode errorCode);
}
