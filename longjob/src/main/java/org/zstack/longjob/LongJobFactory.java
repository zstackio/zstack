package org.zstack.longjob;

import org.zstack.header.longjob.LongJob;

/**
 * Created by GuoYi on 11/25/17.
 */
public interface LongJobFactory {
    LongJob getLongJob(String jobName);
}
