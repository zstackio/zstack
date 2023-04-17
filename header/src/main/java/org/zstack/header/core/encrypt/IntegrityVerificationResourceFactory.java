package org.zstack.header.core.encrypt;

import org.zstack.header.core.Completion;
import org.zstack.header.core.progress.TaskProgressRange;

/**
 * @author hanyu.liang
 * @date 2023/4/10 15:35
 */
public interface IntegrityVerificationResourceFactory {
    String getResourceType();

    void doIntegrityVerification(Integer integrityDataRangeInDays, TaskProgressRange taskProgressRange, Completion completion);
}
