package org.zstack.longjob;

import org.zstack.header.longjob.LongJobVO;

import java.util.function.Consumer;

public interface LongJobTracker {
    void registerLongJobListener(String jobUuid, Consumer<LongJobVO> listener, String resourceType);
}
