package org.zstack.header.vm;

/**
 * @Author: qiuyu.zhang
 * @Date: 2024/4/12 17:17
 */
public interface KvmReportVmShutdownFromGuestEventExtensionPoint {
    void kvmReportVmShutdownEvent(String vmUuid);
}
