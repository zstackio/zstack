package org.zstack.longjob;

/**
 * Created by GuoYi on 11/25/17.
 */
public interface LongJobFactory {
    LongJob getLongJob(String jobName);
}
