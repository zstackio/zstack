package org.zstack.header.longjob;

import org.zstack.header.core.Completion;
import org.zstack.header.longjob.LongJobVO;

/**
 * Created by GuoYi on 11/24/17.
 */
public interface LongJob {
    void start(LongJobVO job, Completion completion);
    void cancel(LongJobVO job, Completion completion);
}
