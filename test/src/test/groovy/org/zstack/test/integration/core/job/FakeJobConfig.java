package org.zstack.test.integration.core.job;

/**
 * Created by Administrator on 2017-03-24.
 */
import java.util.ArrayList;
import java.util.List;

public class FakeJobConfig {
    List<Long> indexs = new ArrayList<Long>();
    volatile int flag;
    boolean success;
    volatile int jdbRepeatNum;
    volatile boolean condition = false;
}

