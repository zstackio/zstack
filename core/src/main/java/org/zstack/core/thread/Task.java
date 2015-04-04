package org.zstack.core.thread;

import java.util.concurrent.Callable;

public interface Task<T> extends Callable<T> {
    String getName();
}
