package org.zstack.utils.logging;

public interface LogDropCounterMBean {
    long getDroppedCount();
    long getImportantDroppedCount();
    long getLastDropTimestamp();
}
