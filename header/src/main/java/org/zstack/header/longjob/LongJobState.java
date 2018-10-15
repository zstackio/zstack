package org.zstack.header.longjob;

/**
 * Created by GuoYi on 11/13/17.
 */
public enum LongJobState {
    Waiting,
    Suspended,
    Running,
    Succeeded,
    Canceling,
    Canceled,
    Failed,
}
