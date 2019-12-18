package org.zstack.test.compute.hostallocator;

import org.zstack.core.Platform;
import org.zstack.header.allocator.HostAllocatorSpec;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.host.HostAllocateExtensionPoint;

public class HostAllocateExtension implements HostAllocateExtensionPoint {
    private boolean errorOut = false;

    public boolean isErrorOut() {
        return errorOut;
    }

    public void setErrorOut(boolean errorOut) {
        this.errorOut = errorOut;
    }

    @Override
    public void beforeAllocateHostSuccessReply(HostAllocatorSpec spec, String replyHostUuid) {
        if (isErrorOut()) {
            throw new OperationFailureException(Platform.operr("On purpose"));
        }
    }
}
