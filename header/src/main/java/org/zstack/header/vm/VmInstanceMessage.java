package org.zstack.header.vm;

public interface VmInstanceMessage {
    String getVmInstanceUuid();

    default void setVmInstanceUuid(String uuid) {

    }
}
