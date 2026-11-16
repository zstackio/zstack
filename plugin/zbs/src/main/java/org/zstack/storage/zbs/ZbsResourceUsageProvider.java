package org.zstack.storage.zbs;

import org.zstack.header.core.ReturnValueCompletion;

import java.util.Collection;
import java.util.List;

public interface ZbsResourceUsageProvider {
    boolean isAvailable(ZbsNodeRef nodeRef);

    void query(
            ZbsNodeRef nodeRef,
            Collection<String> cgroupNames, ReturnValueCompletion<List<ZbsCgroupResourceUsage>> completion);
}
