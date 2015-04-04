package org.zstack.test.core.job;

import java.util.ArrayList;
import java.util.List;

public class FakeJobConfig {
    List<Long> indexs = new ArrayList<Long>();
    volatile int flag;
    boolean success;
    volatile int jdbRepeatNum;
    volatile boolean condition = false;
}
