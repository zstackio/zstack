package org.zstack.test.integration.core.job;

/**
 * Created by Administrator on 2017-03-24.
 */
class FakeJobConfig implements Serializable {
    List<Long> indexes = new ArrayList<Long>()
    volatile int flag
    boolean success
    volatile int jdbRepeatNum
    volatile boolean condition = false
}

