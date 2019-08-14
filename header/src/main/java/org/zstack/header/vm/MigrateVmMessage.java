package org.zstack.header.vm;

public interface MigrateVmMessage {
    String getHostUuid();
    String getStrategy();
    boolean isMigrateFromDestination();
    boolean isAllowUnknown();
}
