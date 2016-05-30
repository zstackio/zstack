package org.zstack.core.logging;

/**
 * Created by xing5 on 2016/5/30.
 */
public interface LogBackend {
    void writeLog(Log log);
}
