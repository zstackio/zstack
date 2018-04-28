package org.zstack.header.vm;

public interface MigrateVmMessage {
    String getHostUuid();

    boolean isMigrateFromDestination();
}
